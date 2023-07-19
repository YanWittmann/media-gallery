package de.yanwittmann.gallery.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageUtil {

    //private static final long MAX_THUMB_AGE = TimeUnit.MINUTES.toMillis(5);
    //private static final long DELETE_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    private static final long MAX_THUMB_AGE = TimeUnit.SECONDS.toMillis(10);
    private static final long DELETE_INTERVAL = TimeUnit.SECONDS.toMillis(2);


    public static void initializeThumbnailCleanup(File directoryPath) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            final File[] files = directoryPath.listFiles();
            if (files == null) return;
            long now = System.currentTimeMillis();

            for (File file : files) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    long fileTime = attrs.creationTime().toMillis();
                    if (now - fileTime > MAX_THUMB_AGE) {
                        Files.delete(file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, DELETE_INTERVAL, DELETE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public static File createThumbnail(File originalFile, File thumbnailFile, int maxSize) throws IOException {
        if (thumbnailFile.exists()) {
            return thumbnailFile;
        }

        BufferedImage originalImage = ImageIO.read(originalFile);

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width > maxSize) {
            height = (int) (height * ((double) maxSize / width));
            width = maxSize;
        }

        if (height > maxSize) {
            width = (int) (width * ((double) maxSize / height));
            height = maxSize;
        }

        BufferedImage thumbnailImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = thumbnailImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        if (!thumbnailFile.getParentFile().exists()) {
            thumbnailFile.getParentFile().mkdirs();
        }

        ImageIO.write(thumbnailImage, "png", thumbnailFile);

        return thumbnailFile;
    }
}
