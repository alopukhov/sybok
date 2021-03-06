package io.github.alopukhov.sybok.engine;

import io.github.alopukhov.sybok.engine.discovery.DiscoveryContext;
import io.github.alopukhov.sybok.engine.discovery.EngineAndDescriptor;
import org.junit.platform.engine.*;

import java.util.*;

import static java.util.Optional.ofNullable;

public class SybokEngine implements TestEngine {
    public static final String ENGINE_ID = "sybok-engine";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ConfigurationParameters configurationParameters = discoveryRequest.getConfigurationParameters();
        SybokEngineOptions engineOptions = SybokEngineOptions.from(configurationParameters);
        GroovyContext groovyContext = GroovyContext.create(engineOptions, selectBestParentClassLoader());
        try {
            List<TestEngine> testEngines = selectEngines(groovyContext, engineOptions);
            DiscoveryContext context = new DiscoveryContext(uniqueId, groovyContext, testEngines);
            Collection<EngineAndDescriptor> discovered = context.discover(discoveryRequest);
            return createDescriptor(groovyContext, discovered, uniqueId);
        } catch (Exception e) {
            try {
                groovyContext.close();
            } catch (Exception closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }

    private TestDescriptor createDescriptor(GroovyContext groovyContext,
                                            Collection<EngineAndDescriptor> discovered,
                                            UniqueId uniqueId) {
        SybokEngineDescriptor engineDescriptor = new SybokEngineDescriptor(uniqueId, groovyContext);
        for (EngineAndDescriptor pair : discovered) {
            engineDescriptor.addChild(pair.descriptor(), pair.engine());
        }
        return engineDescriptor;
    }

    @Override
    public void execute(ExecutionRequest request) {
        EngineExecutionListener listener = request.getEngineExecutionListener();
        listener.executionStarted(request.getRootTestDescriptor());
        try (SybokEngineDescriptor root = (SybokEngineDescriptor) request.getRootTestDescriptor()) {
            root.execute(request);
        } catch (Exception e) {
            listener.executionFinished(request.getRootTestDescriptor(), TestExecutionResult.failed(e));
            throw e;
        }
        listener.executionFinished(request.getRootTestDescriptor(), TestExecutionResult.successful());
    }

    @Override
    public Optional<String> getGroupId() {
        return ofNullable(VersionInfo.group());
    }

    @Override
    public Optional<String> getArtifactId() {
        return ofNullable(VersionInfo.artifact());
    }

    @Override
    public Optional<String> getVersion() {
        return ofNullable(VersionInfo.version());
    }

    private ClassLoader selectBestParentClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            return loader;
        }
        return getClass().getClassLoader();
    }

    private List<TestEngine> selectEngines(GroovyContext groovyContext, SybokEngineOptions engineOptions) {
        Set<String> includedIds = new HashSet<>(engineOptions.getEngineIds());
        Map<String, TestEngine> engines = new HashMap<>();
        for (TestEngine testEngine : ServiceLoader.load(TestEngine.class)) {
            String id = testEngine.getId();
            if (!testEngine.getClass().equals(this.getClass()) &&
                    !this.getId().equals(id) &&
                    (includedIds.isEmpty() || includedIds.contains(id))) {
                TestEngine decorated = decorate(groovyContext, testEngine, engineOptions);
                engines.put(id, decorated);
            }
        }
        return new ArrayList<>(engines.values());
    }

    private TestEngine decorate(GroovyContext groovyContext, TestEngine testEngine, SybokEngineOptions engineOptions) {
        if (engineOptions.alterClassloader(testEngine.getId())) {
            testEngine = groovyContext.decorateClassLoader(testEngine);
        }
        return testEngine;
    }
}
