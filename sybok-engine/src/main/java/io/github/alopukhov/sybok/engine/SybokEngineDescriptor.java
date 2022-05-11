package io.github.alopukhov.sybok.engine;

import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

class SybokEngineDescriptor extends EngineDescriptor implements Closeable {
    private final GroovyContext groovyContext;
    private final Map<UniqueId, TestEngine> engines = synchronizedMap(new HashMap<>());

    SybokEngineDescriptor(UniqueId uniqueId, GroovyContext groovyContext) {
        super(uniqueId, "Sybok");
        this.groovyContext = groovyContext;
    }

    @Override
    public void close() {
        engines.clear();
        try {
            groovyContext.close();
        } catch (Exception ignore) {
            // ignore
        }
    }

    public void addChild(TestDescriptor rootDescriptor, TestEngine testEngine) {
        UniqueId id = rootDescriptor.getUniqueId();
        TestEngine oldEngine = engines.put(id, testEngine);
        if (oldEngine != null) {
            throw new IllegalStateException("Reusing child descriptor for " + id);
        }
        this.addChild(rootDescriptor);
    }

    public void execute(ExecutionRequest originalRequest) {
        for (TestDescriptor child : children) {
            execute(child, originalRequest);
        }
    }

    private void execute(TestDescriptor child, ExecutionRequest originalRequest) {
        TestEngine engine = engines.get(child.getUniqueId());
        EngineExecutionListener listener = originalRequest.getEngineExecutionListener();
        ConfigurationParameters parameters = originalRequest.getConfigurationParameters();
        if (engine == null) {
            listener.executionStarted(child);
            listener.executionFinished(child, TestExecutionResult
                    .failed(new IllegalStateException("Internal engine error")));
            return;
        }
        ExecutionRequest modifiedRequest = new ExecutionRequest(child, listener, parameters);
        engine.execute(modifiedRequest);
    }
}
