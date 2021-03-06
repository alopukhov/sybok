package io.github.alopukhov.sybok.engine.discovery;

import io.github.alopukhov.sybok.engine.GroovyContext;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.nio.file.Path;
import java.util.*;

class CandidatesSelectorContext {
    private final Map<String, List<DiscoverySelector>> selectorSources = new HashMap<>();
    private final GroovyContext groovyContext;
    private final String scriptExtension;

    CandidatesSelectorContext(GroovyContext groovyContext) {
        this.groovyContext = groovyContext;
        this.scriptExtension = groovyContext.scriptExtension();
    }

    public List<Path> scriptRoots() {
        return groovyContext.roots();
    }

    public Optional<Path> rootOf(Path path) {
        return groovyContext.rootOf(path);
    }

    /**
     * @param filename name of fiel
     * @return filename with script extension removed
     */
    public Optional<String> withoutScriptExtension(String filename) {
        if (filename.endsWith(scriptExtension)) {
            return Optional.of(filename.substring(0, filename.length() - scriptExtension.length()));
        }
        return Optional.empty();
    }

    public String withExtension(String filename) {
        return filename + scriptExtension;
    }

    public void registerCandidate(String className, DiscoverySelector source) {
        selectorSources.computeIfAbsent(className, any -> new ArrayList<>()).add(source);
    }

    public List<DiscoverySelector> nextSelectors(DiscoveryListenerAdapter discoveryListenerAdapter) {
        List<DiscoverySelector> classSelectors = new ArrayList<>();
        selectorSources.forEach((className, originalSelectors) -> {
            Class<?> candidate = groovyContext.safeLoadClass(className).orElse(null);
            if (candidate == null) {
                return;
            }
            ClassSelector selector = DiscoverySelectors.selectClass(candidate);
            for (DiscoverySelector originalSelector : originalSelectors) {
                discoveryListenerAdapter.listenFor(selector, originalSelector);
            }
            classSelectors.add(selector);
        });
        return classSelectors;
    }
}
