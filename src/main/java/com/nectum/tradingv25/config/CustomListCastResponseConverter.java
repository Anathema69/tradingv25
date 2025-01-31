package com.nectum.tradingv25.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nectum.tradingv25.model.response.ListCastResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class CustomListCastResponseConverter implements HttpMessageConverter<List<ListCastResponse>> {

    private final ObjectMapper objectMapper;

    public CustomListCastResponseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return List.class.isAssignableFrom(clazz) &&
                mediaType != null &&
                mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.APPLICATION_JSON);
    }

    @Override
    public List<ListCastResponse> read(Class<? extends List<ListCastResponse>> clazz, HttpInputMessage inputMessage) {
        throw new HttpMessageNotReadableException("Reading not supported", inputMessage);
    }

    @Override
    public void write(List<ListCastResponse> responses, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        StringBuilder result = new StringBuilder();
        for (ListCastResponse response : responses) {
            result.append(objectMapper.writeValueAsString(response));
        }

        outputMessage.getBody().write(result.toString().getBytes(StandardCharsets.UTF_8));
    }
}