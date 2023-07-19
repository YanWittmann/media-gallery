package de.yanwittmann.gallery.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileWalkerUtils {

    public static void walkFileTreeMultiThreaded(Path rootDir, Predicate<Path> fileFilter, Predicate<Path> dirFilter, Consumer<Path> consumer, int threads) {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dirFilter.test(dir)) {
                        executor.submit(() -> {
                            try {
                                Files.walk(dir)
                                        .filter(fileFilter)
                                        .forEach(consumer);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();

        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Predicate<Path> extensionFilter(String... extensions) {
        List<String> extensionList = Arrays.stream(extensions)
                .map(extension -> extension.startsWith(".") ? extension : "." + extension)
                .collect(Collectors.toList());

        return path -> {
            String fileName = path.getFileName().toString();
            return extensionList.stream().anyMatch(fileName::endsWith);
        };
    }
}

