package de.yanwittmann.gallery;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class AppEntryPoint {

    public static void main(String[] args) {
        // SpringApplication.run(MainController.class, args);

        SpringApplicationBuilder builder = new SpringApplicationBuilder(MainController.class);
        builder.headless(false);
        ConfigurableApplicationContext context = builder.run(args);
    }
}
