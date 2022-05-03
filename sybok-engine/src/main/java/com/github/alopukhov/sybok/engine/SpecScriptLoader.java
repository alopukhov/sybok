package com.github.alopukhov.sybok.engine;

import groovy.lang.GroovyClassLoader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

class SpecScriptLoader implements Closeable {
    private final GroovyClassLoader gcl;
    private final List<Path> roots;

    public static SpecScriptLoader create(SybokEngineOptions options, ClassLoader parentClassloader) {
        GroovyClassLoader gcl = new GroovyClassLoader(parentClassloader);
        List<Path> scriptRoots = options.getScriptRoots().stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(toList());
        try {
            for (Path scriptRoot : scriptRoots) {
                gcl.addURL(scriptRoot.toUri().toURL());
            }
        } catch (Exception e) {
            try {
                gcl.close();
            } catch (Exception closeException) {
                e.addSuppressed(closeException);
            }
            throw new IllegalStateException(e);
        }
        return new SpecScriptLoader(gcl, scriptRoots);
    }

    private SpecScriptLoader(GroovyClassLoader gcl, List<Path> roots) {
        this.gcl = gcl;
        this.roots = Collections.unmodifiableList(roots);
    }

    public Optional<Path> rootOf(Path path) {
        return roots.stream()
                .filter(path.toAbsolutePath().normalize()::startsWith)
                .findFirst();
    }

    public Optional<Class<?>> safeLoadClass(String className) {
        try {
            return Optional.of(Class.forName(className, false, gcl));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        gcl.close();
    }
}
