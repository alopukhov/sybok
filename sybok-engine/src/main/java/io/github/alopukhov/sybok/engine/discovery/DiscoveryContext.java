package io.github.alopukhov.sybok.engine.discovery;

import io.github.alopukhov.sybok.engine.SpecScriptLoader;
import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryContext {
    private final SpecScriptLoader specScriptLoader;
    private final UniqueId root;
    private final List<TestEngine> delegateEngines;

    public DiscoveryContext(UniqueId root, SpecScriptLoader specScriptLoader, List<TestEngine> delegateEngines) {
        this.specScriptLoader = specScriptLoader;
        this.root = root;
        this.delegateEngines = delegateEngines;
    }

    public List<EngineAndDescriptor> discover(EngineDiscoveryRequest request) {
        CandidatesSelectorContext candidatesSelectorContext = new CandidatesSelectorContext(specScriptLoader);
        resolveToCandidates(request, candidatesSelectorContext);
        DiscoveryListenerAdapter listenerAdapter = new DiscoveryListenerAdapter();
        List<DiscoverySelector> nextSelectors = candidatesSelectorContext.nextSelectors(listenerAdapter);
        EngineDiscoveryRequest newReq = new DelegateEngineDiscoveryRequest(nextSelectors, request, listenerAdapter);
        List<EngineAndDescriptor> res = discoverInDelegates(newReq);
        listenerAdapter.replayFor(root, request.getDiscoveryListener());
        return res;
    }

    private List<EngineAndDescriptor> discoverInDelegates(EngineDiscoveryRequest modifiedRequest) {
        List<EngineAndDescriptor> discovered = new ArrayList<>();
        for (TestEngine engine : delegateEngines) {
            UniqueId delegateId = root.append("delegate", engine.getId());
            TestDescriptor descriptor = engine.discover(modifiedRequest, delegateId);
            discovered.add(new EngineAndDescriptor(engine, descriptor));
        }
        return discovered;
    }

    private void resolveToCandidates(EngineDiscoveryRequest request, CandidatesSelectorContext candidatesSelectorContext) {
        createResolver(candidatesSelectorContext)
                .resolve(new WithoutListenerRequestWrapper(request), new ThrowawayTestDescriptor());
    }

    private EngineDiscoveryRequestResolver<TestDescriptor> createResolver(CandidatesSelectorContext ctxt) {
        return EngineDiscoveryRequestResolver.builder()
                .addSelectorResolver(initContext -> new FileResolver(ctxt))
                .addSelectorResolver(initContext -> new DirectoryScannerResolver(ctxt, initContext.getClassNameFilter()))
                .build();
    }

    private static class ThrowawayTestDescriptor extends AbstractTestDescriptor {
        public ThrowawayTestDescriptor() {
            super(UniqueId.root("unused", "unused"), "<internal>");
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }
    }

}
