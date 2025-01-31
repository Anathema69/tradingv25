package com.nectum.tradingv25.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CustomListCastResponseConverter customConverter;

    public WebConfig(CustomListCastResponseConverter customConverter) {
        this.customConverter = customConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, customConverter);
    }
}