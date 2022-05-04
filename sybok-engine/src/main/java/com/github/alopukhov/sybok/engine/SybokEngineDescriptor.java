package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.io.Closeable;
import java.io.IOException;

class SybokEngineDescriptor extends EngineDescriptor implements Closeable {
    private final SpecScriptLoader specScriptLoader;

    public SybokEngineDescriptor(UniqueId uniqueId, SpecScriptLoader specScriptLoader, TestDescriptor delegateDescriptor) {
        super(uniqueId, "Sybok");
        this.specScriptLoader = specScriptLoader;
        addChild(delegateDescriptor);
    }

    @Override
    public void close() throws IOException {
        specScriptLoader.close();
    }
}
