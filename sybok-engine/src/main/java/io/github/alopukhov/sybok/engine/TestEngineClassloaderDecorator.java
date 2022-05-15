package io.github.alopukhov.sybok.engine;

import org.junit.platform.engine.*;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class TestEngineClassloaderDecorator implements TestEngine {
    private final TestEngine delegate;
    private final ClassLoader classLoader;

    TestEngineClassloaderDecorator(TestEngine delegate, ClassLoader classLoader) {
        this.delegate = requireNonNull(delegate);
        this.classLoader = requireNonNull(classLoader);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return delegate.discover(discoveryRequest, uniqueId);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Override
    public void execute(ExecutionRequest request) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            delegate.execute(request);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Override
    public Optional<String> getGroupId() {
        return delegate.getGroupId();
    }

    @Override
    public Optional<String> getArtifactId() {
        return delegate.getArtifactId();
    }

    @Override
    public Optional<String> getVersion() {
        return delegate.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEngineClassloaderDecorator that = (TestEngineClassloaderDecorator) o;
        return delegate.equals(that.delegate) &&
                Objects.equals(classLoader, that.classLoader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, classLoader);
    }

    @Override
    public String toString() {
        return "TestEngineWithClassloader{" +
                "delegate=" + delegate +
                ", classLoader=" + classLoader +
                '}';
    }
}
