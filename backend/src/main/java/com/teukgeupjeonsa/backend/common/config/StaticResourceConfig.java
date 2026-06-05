package com.teukgeupjeonsa.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.profile-images-dir:uploads/profile-images}")
    private String profileImagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(profileImagesDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations(uploadPath.toUri().toString() + "/");
    }
}
