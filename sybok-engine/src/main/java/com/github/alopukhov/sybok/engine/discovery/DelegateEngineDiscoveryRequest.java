package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.*;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

class DelegateEngineDiscoveryRequest implements EngineDiscoveryRequest {
    private final List<DiscoverySelector> selectors;
    private final EngineDiscoveryRequest originalRequest;
    private final EngineDiscoveryListener listener;

    DelegateEngineDiscoveryRequest(List<DiscoverySelector> selectors, EngineDiscoveryRequest originalRequest, EngineDiscoveryListener listener) {
        this.selectors = selectors;
        this.originalRequest = originalRequest;
        this.listener = listener;
    }

    @Override
    public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
        return selectors.stream().filter(selectorType::isInstance)
                .map(selectorType::cast)
                .collect(toList());
    }

    @Override
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        return Collections.emptyList();
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return originalRequest.getConfigurationParameters();
    }

    @Override
    public EngineDiscoveryListener getDiscoveryListener() {
        return listener;
    }
}
