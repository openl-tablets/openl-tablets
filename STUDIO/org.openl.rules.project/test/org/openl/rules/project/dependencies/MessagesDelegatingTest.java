package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.project.instantiation.SimpleDependencyManager;
import org.openl.rules.project.instantiation.SimpleMultiModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public class MessagesDelegatingTest {
    private ProjectDescriptor project;
    private SimpleDependencyManager dependencyManager;

    @Before
    public void init() throws Exception {
        File rulesFolder = new File("test-resources/modules_with_errors/");
        project = ProjectResolver.instance().resolve(rulesFolder);
        dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
            Thread.currentThread().getContextClassLoader(),
            false,
            false);
    }

    private Module findModuleByName(String moduleName) {
        for (Module module : project.getModules()) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    private static IDependency getDependencyForModule(String moduleName) {
        return new Dependency(DependencyType.MODULE,
            new IdentifierNode(IXlsTableNames.DEPENDENCY, null, moduleName, null));
    }

    private CompiledOpenClass getCompiledOpenClassForModule(String moduleName) throws Exception {
        // it is passed through the dependency manager to receive the same
        // instances of OpenLMessages
        IDependency dependency = getDependencyForModule(moduleName);
        return dependencyManager.loadDependency(dependency).getCompiledOpenClass();
    }

    @Test
    public void testMessagesDelegatingFromDependencies() throws Exception {
        CompiledOpenClass compiledRules = getCompiledOpenClassForModule("Rules");
        CompiledOpenClass compiledRules2 = getCompiledOpenClassForModule("Rules2");
        assertTrue(compiledRules2.getMessages().size() >= compiledRules.getMessages().size());
        assertTrue(compiledRules2.getMessages().containsAll(compiledRules.getMessages()));
        CompiledOpenClass compiledRules3 = getCompiledOpenClassForModule("Rules3");
        assertTrue(compiledRules3.getMessages().size() >= compiledRules.getMessages().size());
        assertTrue(compiledRules3.getMessages().containsAll(compiledRules.getMessages()));
        assertTrue(compiledRules3.getMessages().size() >= compiledRules2.getMessages().size());
        assertTrue(compiledRules3.getMessages().containsAll(compiledRules2.getMessages()));
    }

    @Test
    public void testMessagesGatheringInMultimodule() throws Exception {
        List<Module> forGrouping = new ArrayList<>();
        forGrouping.add(findModuleByName("Rules3"));
        forGrouping.add(findModuleByName("Rules4"));
        forGrouping.add(findModuleByName("Rules5"));
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(forGrouping, true);
        CompiledOpenClass compiledMultiModule = strategy.compile();
        for (Module module : project.getModules()) {
            CompiledOpenClass compiledModule = getCompiledOpenClassForModule(module.getName());
            compiledMultiModule.getMessages().containsAll(compiledModule.getMessages());
        }

        assertFalse("During compilation DuplicatedMethodException must not be thrown",
            hasDuplicatedMethodException(compiledMultiModule));
    }

    @Test
    public void testDublicateTableDefenitionInMultimodule() throws Exception {
        List<Module> forGrouping = new ArrayList<>();
        forGrouping.add(findModuleByName("Rules"));
        forGrouping.add(findModuleByName("Rules2"));
        forGrouping.add(findModuleByName("Rules3"));
        forGrouping.add(findModuleByName("Rules6"));
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(forGrouping, true);
        CompiledOpenClass compiledMultiModule = strategy.compile();
        for (Module module : project.getModules()) {
            CompiledOpenClass compiledModule = getCompiledOpenClassForModule(module.getName());
            compiledMultiModule.getMessages().containsAll(compiledModule.getMessages());
        }

        assertTrue("During compilation DuplicatedMethodException must be thrown",
            hasDuplicatedMethodException(compiledMultiModule));
    }

    private boolean hasDuplicatedMethodException(CompiledOpenClass compiledOpenClass) {
        boolean hasDuplicatedMethodException = false;

        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils
            .filterMessagesBySeverity(compiledOpenClass.getMessages(), Severity.ERROR);
        for (OpenLMessage error : errorMessages) {
            if (error instanceof OpenLErrorMessage) {
                Throwable cause = ((OpenLErrorMessage) error).getError().getCause();
                if (cause instanceof DuplicatedMethodException) {
                    hasDuplicatedMethodException = true;
                    break;
                }
            }
        }
        return hasDuplicatedMethodException;
    }

}
