package de.yanwittmann.gallery;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class AppEntryPoint {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        // SpringApplication.run(MainController.class, args);

        SpringApplicationBuilder builder = new SpringApplicationBuilder(MainController.class);
        builder.headless(false);
        context = builder.run(args);
    }

    public static void stop() {
        context.close();
        System.exit(0);
    }
}
