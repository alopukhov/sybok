package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class FileResolver implements SelectorResolver {
    private final CandidatesSelectorContext ctxt;

    public FileResolver(CandidatesSelectorContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public Resolution resolve(FileSelector selector, Context unused) {
        Path path = selector.getPath().toAbsolutePath().normalize();
        Optional<String> nameWithoutExtension = ctxt.withoutScriptExtension(path.getFileName().toString());
        if (!nameWithoutExtension.isPresent() || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Resolution.unresolved();
        }
        Optional<Path> scriptRootOpt = ctxt.rootOf(path);
        if (!scriptRootOpt.isPresent() || scriptRootOpt.get().equals(path)) {
            return Resolution.unresolved();
        }
        String className = resolveClassName(scriptRootOpt.get(), path.getParent(), nameWithoutExtension.get());
        ctxt.registerCandidate(className, selector);
        return Resolution.unresolved(); //not resolved yet
    }

    @Override
    public Resolution resolve(ClassSelector selector, Context unused) {
        String className = selector.getClassName().trim();
        String subpath = ctxt.withExtension(className.replace('.', File.separatorChar));
        if (subpath.startsWith("\\") || subpath.startsWith("/") || subpath.isEmpty()) {
            return Resolution.unresolved();
        }
        for (Path root : ctxt.scriptRoots()) {
            try {
                Path path = root.resolve(subpath);
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    ctxt.registerCandidate(className, selector);
                    return Resolution.unresolved();
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
        return Resolution.unresolved();
    }

    private String resolveClassName(Path root, Path dir, String nameWithoutExtension) {
        List<String> elements = new ArrayList<>();
        elements.add(nameWithoutExtension);
        while (!root.equals(dir)) {
            elements.add(dir.getFileName().toString());
            dir = dir.getParent();
        }
        Collections.reverse(elements);
        return String.join(".", elements);
    }
}
