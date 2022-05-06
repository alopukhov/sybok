package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.SelectorResolutionResult;
import org.junit.platform.engine.UniqueId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class DiscoveryListenerAdapter implements EngineDiscoveryListener {
    private final Map<DiscoverySelector, List<DiscoverySelector>> originalSelectors = new HashMap<>();
    private final Set<DiscoverySelector> allOriginals = new HashSet<>();
    private final Set<DiscoverySelector> resolvedSelectors = ConcurrentHashMap.newKeySet();

    public void listenFor(DiscoverySelector real, DiscoverySelector original) {
        originalSelectors.computeIfAbsent(real, any -> new ArrayList<>()).add(original);
        allOriginals.add(original);
    }

    public void replayFor(UniqueId engineId, EngineDiscoveryListener listener) {
        for (DiscoverySelector selector : allOriginals) {
            boolean resolved = resolvedSelectors.contains(selector);
            SelectorResolutionResult result = resolved ? SelectorResolutionResult.resolved() :
                    SelectorResolutionResult.unresolved();
            listener.selectorProcessed(engineId, selector, result);
        }
    }

    @Override
    public void selectorProcessed(UniqueId engineId, DiscoverySelector selector, SelectorResolutionResult result) {
        if (result.getStatus() != SelectorResolutionResult.Status.RESOLVED) {
            List<DiscoverySelector> originals = originalSelectors.get(selector);
            if (originals != null) {
                resolvedSelectors.addAll(originals);
            }
        }
    }
}
