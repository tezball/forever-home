package com.example.foreverhome.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Configuration for serving the React SPA.
 * Forwards all non-API, non-static requests to index.html for client-side routing.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource resource = location.createRelative(resourcePath);

                    // If resource exists, serve it
                    if (resource.exists() && resource.isReadable()) {
                        return resource;
                    }

                    // Don't forward API or actuator requests to index.html
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                        return null;
                    }

                    // For SPA routes, return index.html
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
