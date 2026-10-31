package com.app.suspiro.config;

import io.github.cdimascio.dotenv.Dotenv;

public final class DotenvLoader {
    private DotenvLoader() { }

    public static void init() {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
            }
        });

        String activeProfile = System.getProperty("SPRING_PROFILES_ACTIVE");
        if (activeProfile != null && !activeProfile.isBlank()
                && System.getProperty("spring.profiles.active") == null) {
            System.setProperty("spring.profiles.active", activeProfile);
        }
    }
}
