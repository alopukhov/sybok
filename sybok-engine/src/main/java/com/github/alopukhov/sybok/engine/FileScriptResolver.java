package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

class FileScriptResolver implements SelectorResolver {
    private final SpecScriptLoader specScriptLoader;

    FileScriptResolver(SpecScriptLoader specScriptLoader) {
        this.specScriptLoader = requireNonNull(specScriptLoader);
    }

    @Override
    public Resolution resolve(FileSelector selector, Context context) {
        Path path = selector.getPath().toAbsolutePath().normalize();
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith("spec.groovy") ||
                !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Resolution.unresolved();
        }
        Optional<Path> scriptRootOpt = specScriptLoader.rootOf(path);
        if (!scriptRootOpt.isPresent() || scriptRootOpt.get().equals(path)) {
            return Resolution.unresolved();
        }
        String className = resolveClassName(scriptRootOpt.get(), path);
        return specScriptLoader.safeLoadClass(className)
                .map(DiscoverySelectors::selectClass)
                .map(Collections::singleton)
                .map(Resolution::selectors)
                .orElseGet(Resolution::unresolved);
    }

    private String resolveClassName(Path root, Path file) {
        List<String> elements = new ArrayList<>();
        String filename = file.getFileName().toString();
        elements.add(filename.substring(0, filename.length() - ".groovy".length()));
        file = file.getParent();
        while (!root.equals(file)) {
            elements.add(file.getFileName().toString());
            file = file.getParent();
        }
        Collections.reverse(elements);
        return String.join(".", elements);
    }
}
