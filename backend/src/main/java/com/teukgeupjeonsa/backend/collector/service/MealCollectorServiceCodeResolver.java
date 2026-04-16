package com.teukgeupjeonsa.backend.collector.service;

import com.teukgeupjeonsa.backend.collector.config.MealCollectorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MealCollectorServiceCodeResolver {

    private final MealCollectorProperties properties;

    public List<String> resolveFixedServiceCodes() {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : properties.getFixedServices()) {
            result.add(resolveSingle(raw));
        }
        return result.stream().toList();
    }

    public String resolveSingle(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("서비스 코드가 비어 있습니다.");
        }

        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if ("ATC".equals(normalized)) {
            return properties.getAtcServiceCode();
        }
        if ("STANDARD".equals(normalized)) {
            return properties.getStandardServiceCode();
        }
        if (normalized.startsWith(properties.getServicePrefix())) {
            return normalized;
        }
        if (normalized.matches("[0-9]+")) {
            return properties.getServicePrefix() + normalized;
        }

        throw new IllegalArgumentException("지원하지 않는 서비스 코드 형식입니다: " + rawCode);
    }
}
