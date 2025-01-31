package com.nectum.tradingv25.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nectum.tradingv25.model.request.ListCastRequest;
import com.nectum.tradingv25.model.response.ListCastResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ListCastCache {
    private final Cache<String, List<ListCastResponse>> cache;

    public ListCastCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats()
                .build();
    }

    public List<ListCastResponse> get(ListCastRequest request) {
        return cache.getIfPresent(createKey(request));
    }

    public void put(ListCastRequest request, List<ListCastResponse> responses) {
        cache.put(createKey(request), responses);
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * Genera la clave única a partir de la petición completa,
     * convirtiéndola a JSON y luego haciendo un MD5.
     */
    private String createKey(ListCastRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(request);
            return DigestUtils.md5DigestAsHex(json.getBytes());
        } catch (Exception e) {
            // Si ocurre algún problema, usamos el hashCode de Java como fallback
            return String.valueOf(request.hashCode());
        }
    }
}
