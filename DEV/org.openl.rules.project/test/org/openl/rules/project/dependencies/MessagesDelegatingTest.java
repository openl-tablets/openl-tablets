package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.dependency.DependencyType;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
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
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public class MessagesDelegatingTest {
    private ProjectDescriptor project;
    private SimpleDependencyManager dependencyManager;

    @BeforeEach
    public void init() throws Exception {
        File rulesFolder = new File("test-resources/modules_with_errors/");
        project = ProjectResolver.getInstance().resolve(rulesFolder);
        dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
                Thread.currentThread().getContextClassLoader(),
                false,
                null);
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
        Collection<ResolvedDependency> resolvedDependencies = dependencyManager.resolveDependency(dependency, false);
        return dependencyManager.loadDependency(resolvedDependencies.iterator().next()).getCompiledOpenClass();
    }

    @Test
    public void testMessagesDelegatingFromDependencies() throws Exception {
        CompiledOpenClass compiledRules = getCompiledOpenClassForModule("Rules");
        CompiledOpenClass compiledRules2 = getCompiledOpenClassForModule("Rules2");
        assertTrue(compiledRules2.getAllMessages().size() >= compiledRules.getAllMessages().size());
        assertTrue(compiledRules2.getAllMessages().containsAll(compiledRules.getAllMessages()));
        CompiledOpenClass compiledRules3 = getCompiledOpenClassForModule("Rules3");
        assertTrue(compiledRules3.getAllMessages().size() >= compiledRules.getAllMessages().size());
        assertTrue(compiledRules3.getAllMessages().containsAll(compiledRules.getAllMessages()));
        assertTrue(compiledRules3.getAllMessages().size() >= compiledRules2.getAllMessages().size());
        assertTrue(compiledRules3.getAllMessages().containsAll(compiledRules2.getAllMessages()));
    }

    @Test
    public void testMessagesGatheringInMultiModule() throws Exception {
        List<Module> forGrouping = new ArrayList<>();
        forGrouping.add(findModuleByName("Rules3"));
        forGrouping.add(findModuleByName("Rules4"));
        forGrouping.add(findModuleByName("Rules5"));
        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
                null,
                true,
                null);
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(forGrouping,
                dependencyManager,
                true);
        CompiledOpenClass compiledMultiModule = strategy.compile();
        for (Module module : project.getModules()) {
            CompiledOpenClass compiledModule = getCompiledOpenClassForModule(module.getName());
            compiledMultiModule.getAllMessages().containsAll(compiledModule.getAllMessages());
        }

        assertFalse(hasDuplicatedMethodException(compiledMultiModule),
                "During compilation DuplicatedMethodException must not be thrown");
    }

    @Test
    public void testDuplicateTableDefinitionInMultiModule() throws Exception {
        List<Module> forGrouping = new ArrayList<>();
        forGrouping.add(findModuleByName("Rules"));
        forGrouping.add(findModuleByName("Rules2"));
        forGrouping.add(findModuleByName("Rules3"));
        forGrouping.add(findModuleByName("Rules6"));
        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
                null,
                true,
                null);
        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(forGrouping,
                dependencyManager,
                true);
        CompiledOpenClass compiledMultiModule = strategy.compile();
        for (Module module : project.getModules()) {
            CompiledOpenClass compiledModule = getCompiledOpenClassForModule(module.getName());
            compiledMultiModule.getAllMessages().containsAll(compiledModule.getAllMessages());
        }

        assertTrue(hasDuplicatedMethodException(compiledMultiModule),
                "During compilation DuplicatedMethodException must be thrown");
    }

    private boolean hasDuplicatedMethodException(CompiledOpenClass compiledOpenClass) {
        boolean hasDuplicatedMethodException = false;

        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils
                .filterMessagesBySeverity(compiledOpenClass.getAllMessages(), Severity.ERROR);
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
