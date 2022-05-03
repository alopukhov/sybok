package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.*;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.spockframework.runtime.*;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class SybokScriptEngine extends HierarchicalTestEngine<SpockExecutionContext> {
    @Override
    public String getId() {
        return "sybok-engine";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ConfigurationParameters configurationParameters = discoveryRequest.getConfigurationParameters();
        SybokEngineOptions engineOptions = SybokEngineOptions.from(configurationParameters);
        SpecScriptLoader specScriptLoader = SpecScriptLoader.create(engineOptions, selectBestParentClassLoader());
        RunContext runContext = RunContext.get();
        SpecDescriptorFactory specDescriptorFactory = new SpecDescriptorFactory(runContext);
        SybokEngineDescriptor engineDescriptor = new SybokEngineDescriptor(uniqueId, runContext, specScriptLoader);
        EngineDiscoveryRequestResolver.builder()
                .addSelectorResolver(new DirectoryScriptResolver(specScriptLoader))
                .addSelectorResolver(new FileScriptResolver(specScriptLoader))
                .addSelectorResolver(new ScriptClassSelectorResolver(specDescriptorFactory))
                .build()
                .resolve(discoveryRequest, engineDescriptor);
        return engineDescriptor;
    }

    @Override
    protected SpockExecutionContext createExecutionContext(ExecutionRequest request) {
        return new SpockExecutionContext(request.getEngineExecutionListener());
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
}
