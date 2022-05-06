package com.github.alopukhov.sybok.engine.discovery;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

public class EngineAndDescriptor {
    private final TestEngine engine;
    private final TestDescriptor descriptor;

    EngineAndDescriptor(TestEngine engine, TestDescriptor descriptor) {
        this.engine = engine;
        this.descriptor = descriptor;
    }

    public TestEngine engine() {
        return engine;
    }

    public TestDescriptor descriptor() {
        return descriptor;
    }
}
