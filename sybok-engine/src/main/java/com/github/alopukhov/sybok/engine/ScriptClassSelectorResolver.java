package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;
import org.spockframework.runtime.SpecUtil;

import java.util.Optional;

public class ScriptClassSelectorResolver implements SelectorResolver {
    private final SpecDescriptorFactory specDescriptorFactory;

    public ScriptClassSelectorResolver(SpecDescriptorFactory specDescriptorFactory) {
        this.specDescriptorFactory = specDescriptorFactory;
    }

    @Override
    public Resolution resolve(ClassSelector selector, Context context) {
        Class<?> specClass = selector.getJavaClass();
        if (!SpecUtil.isRunnableSpec(specClass)) {
            return Resolution.unresolved();
        }
        return context
                .addToParent(parent -> Optional.of(specDescriptorFactory.createTestDescriptor(specClass, parent)))
                .map(Match::exact)
                .map(Resolution::match)
                .orElseGet(Resolution::unresolved);
    }
}
