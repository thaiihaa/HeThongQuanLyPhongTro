package com.quanlyphongtro.media;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RoomMediaProperties.class)
public class RoomMediaStorageConfiguration implements WebMvcConfigurer {

    private final RoomMediaProperties properties;
    private final ObjectProvider<RoomMediaAccessInterceptor> accessInterceptorProvider;

    public RoomMediaStorageConfiguration(
            RoomMediaProperties properties,
            ObjectProvider<RoomMediaAccessInterceptor> accessInterceptorProvider
    ) {
        this.properties = properties;
        this.accessInterceptorProvider = accessInterceptorProvider;
    }

    @Bean
    @ConditionalOnBean(RoomMediaRepository.class)
    public RoomMediaAccessInterceptor roomMediaAccessInterceptor(RoomMediaRepository repository) {
        return new RoomMediaAccessInterceptor(repository);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = properties.getStorageDir().toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/api/v1/media-files/rooms/**")
                .addResourceLocations(location + "rooms/")
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        RoomMediaAccessInterceptor accessInterceptor = accessInterceptorProvider.getIfAvailable();
        if (accessInterceptor != null) {
            registry.addInterceptor(accessInterceptor).addPathPatterns("/api/v1/media-files/rooms/**");
        }
    }
}
