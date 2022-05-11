package io.github.alopukhov.sybok.engine;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class GroovyContext implements Closeable {
    private final GroovyClassLoader gcl;
    private final List<Path> roots;
    private final String extension;

    static GroovyContext create(SybokEngineOptions options, ClassLoader parentClassloader) {
        CompilerConfiguration configuration = CompilerConfiguration.DEFAULT;
        GroovyClassLoader gcl = new GroovyClassLoader(parentClassloader, configuration);
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
        return new GroovyContext(gcl, scriptRoots, configuration.getDefaultScriptExtension());
    }

    private GroovyContext(GroovyClassLoader gcl, List<Path> roots, String extension) {
        this.gcl = gcl;
        this.roots = Collections.unmodifiableList(roots);
        this.extension = extension;
    }

    public List<Path> roots() {
        return roots;
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

    public String scriptExtension() {
        return extension;
    }

    @Override
    public void close() throws IOException {
        gcl.close();
    }
}
