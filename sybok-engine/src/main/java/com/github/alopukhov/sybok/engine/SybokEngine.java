package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.spockframework.runtime.SpockEngine;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class SybokEngine implements TestEngine {
    private final TestEngine delegate = new SpockEngine();

    @Override
    public String getId() {
        return "sybok-engine";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        ConfigurationParameters configurationParameters = discoveryRequest.getConfigurationParameters();
        SybokEngineOptions engineOptions = SybokEngineOptions.from(configurationParameters);
        SpecScriptLoader specScriptLoader = SpecScriptLoader.create(engineOptions, selectBestParentClassLoader());
        CandidatesSelector candidatesSelector = new CandidatesSelector(specScriptLoader);
        List<ClassSelector> delegateSelectors = candidatesSelector.selectCandidates(discoveryRequest).stream()
                .distinct()
                .map(DiscoverySelectors::selectClass)
                .collect(toList());
        DiscoveryRequestWrapper newRequest = new DiscoveryRequestWrapper(delegateSelectors, discoveryRequest);
        UniqueId subId = uniqueId.append("delegate", this.delegate.getId());
        TestDescriptor delegateDescriptor = delegate.discover(newRequest, subId);
        return new SybokEngineDescriptor(uniqueId, specScriptLoader, delegateDescriptor);
    }

    @Override
    public void execute(ExecutionRequest request) {
        EngineExecutionListener listener = request.getEngineExecutionListener();
        SybokEngineDescriptor root = (SybokEngineDescriptor) request.getRootTestDescriptor();
        listener.executionStarted(root);
        root.getChildren().stream()
                .map(td -> new ExecutionRequest(td, listener, request.getConfigurationParameters()))
                .forEachOrdered(delegate::execute);
        try {
            ((AutoCloseable) root).close();
        } catch (Exception exception) {
            // ignore
        }
        listener.executionFinished(root, TestExecutionResult.successful());
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
