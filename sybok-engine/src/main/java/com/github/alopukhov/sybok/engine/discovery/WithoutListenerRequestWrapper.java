package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;

import java.util.List;

class WithoutListenerRequestWrapper implements EngineDiscoveryRequest {
    private final EngineDiscoveryRequest originalRequest;

    public WithoutListenerRequestWrapper(EngineDiscoveryRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    @Override
    public <U extends DiscoverySelector> List<U> getSelectorsByType(Class<U> selectorType) {
        return originalRequest.getSelectorsByType(selectorType);
    }

    @Override
    public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
        return originalRequest.getFiltersByType(filterType);
    }

    @Override
    public ConfigurationParameters getConfigurationParameters() {
        return originalRequest.getConfigurationParameters();
    }
}
