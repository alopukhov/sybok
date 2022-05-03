package com.github.alopukhov.sybok.engine;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.spockframework.runtime.*;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;
import spock.config.RunnerConfiguration;

public class SpecDescriptorFactory {
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final RunContext runContext;
    private final RunnerConfiguration runnerConfiguration;

    public SpecDescriptorFactory(RunContext runContext) {
        this.runContext = runContext;
        this.runnerConfiguration = runContext.getConfiguration(RunnerConfiguration.class);
    }

    public TestDescriptor createTestDescriptor(Class<?> specClass,
                                                         TestDescriptor parent) {
        SpecInfo specInfo = new SpecInfoBuilder(specClass).build();
        UniqueId id = parent.getUniqueId().append("spec", specInfo.getReflection().getName());
        try {
            runContext.createExtensionRunner(specInfo).run();
        } catch (Exception e) {
            return new ErrorSpecNode(id, runnerConfiguration, specInfo, e) {/*TODO: ask spock team*/};
        }
        SpecNode specNode = new SpecNode(id, runnerConfiguration, specInfo){ /*TODO: ask spock team*/ };
        specInfo.getAllFeaturesInExecutionOrder().stream()
                .filter(featureInfo -> !featureInfo.isExcluded())
                .map(featureInfo -> createNode(specNode, featureInfo))
                .forEach(specNode::addChild);
        return specNode;
    }

    private SpockNode<?> createNode(SpecNode specNode, FeatureInfo feature) {
        if (feature.isParameterized()) {
            return describeParameterizedFeature(specNode.getUniqueId(), feature);
        } else {
            return describeSimpleFeature(specNode.getUniqueId(), feature);
        }
    }

    private FeatureNode describeParameterizedFeature(UniqueId parentId, FeatureInfo feature) {
        return new ParameterizedFeatureNode(toUniqueId(parentId, feature), runnerConfiguration, feature) {
            //TODO: ask spock team
        };
    }

    private SpockNode<?> describeSimpleFeature(UniqueId parentId, FeatureInfo feature) {
        IterationInfo iterationInfo = new IterationInfo(feature, 0, EMPTY_ARGS, 1);
        iterationInfo.setName(feature.getName());
        UniqueId uniqueId = toUniqueId(parentId, feature);
        IterationNode iterationNode = new IterationNode(uniqueId, runnerConfiguration, iterationInfo) {
            //TODO: ask spock team
        };
        return new SimpleFeatureNode(uniqueId, runnerConfiguration, feature, iterationNode);
    }

    private UniqueId toUniqueId(UniqueId parentId, FeatureInfo feature) {
        return parentId.append("feature", feature.getFeatureMethod().getReflection().getName());
    }
}
