package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.ClassSelector;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

class DiscoveryRequestWrapper implements EngineDiscoveryRequest {
    private final List<ClassSelector> overrideSelectors;
    private final EngineDiscoveryRequest originalRequest;

    public DiscoveryRequestWrapper(List<ClassSelector> overrideSelectors, EngineDiscoveryRequest originalRequest) {
        this.overrideSelectors = overrideSelectors;
        this.originalRequest = originalRequest;
    }

    @Override
    public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
        if (selectorType.isAssignableFrom(ClassSelector.class)) {
            return overrideSelectors.stream()
                    .map(selectorType::cast)
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        return originalRequest.getFiltersByType(filterType);
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return originalRequest.getConfigurationParameters();
    }

    @Override
    public EngineDiscoveryListener getDiscoveryListener() {
        return originalRequest.getDiscoveryListener();
    }
}
