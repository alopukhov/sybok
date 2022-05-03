package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import org.spockframework.runtime.RunContext;
import org.spockframework.runtime.SpockExecutionContext;

public class SybokEngineDescriptor extends EngineDescriptor implements Node<SpockExecutionContext> {
    private final RunContext runContext;
    private final SpecScriptLoader specScriptLoader;

    public SybokEngineDescriptor(UniqueId uniqueId, RunContext runContext, SpecScriptLoader specScriptLoader) {
        super(uniqueId, "Sybok");
        this.runContext = runContext;
        this.specScriptLoader = specScriptLoader;
    }

    @Override
    public SpockExecutionContext prepare(SpockExecutionContext context) {
        return context.withRunContext(runContext);
    }

    @Override
    public void after(SpockExecutionContext context) throws Exception {
        specScriptLoader.close();
    }
}
