package org.openl.rules.project;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IOpenClass;

public class SystemPropertiesTest {

    @Test
    public void testSystemPropertiesOverloading() throws Exception {
        System.setProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, "true");
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        File root = new File("test/resources/system-properties-test");
        ProjectDescriptor project = RulesProjectResolver.loadProjectResolverFromClassPath().isRulesProject(root).resolveProject(root);
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(project.getModules());
        Map<String, Object> externalParameters = new HashMap<String, Object>();
        externalParameters.put(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY, false);
        externalParameters.put(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        strategy.setExternalParameters(externalParameters);
        assertTrue(strategy.compile().getOpenClass().getMethod("calc", new IOpenClass[0]) instanceof OverloadedMethodsDispatcherTable);
    }
}
