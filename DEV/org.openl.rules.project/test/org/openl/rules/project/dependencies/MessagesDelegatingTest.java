package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.CompiledOpenClass;
import org.openl.dependency.DependencyType;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.ResolvedDependency;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public class MessagesDelegatingTest {
    private CompiledOpenClass compiledOpenClass;
    private IDependencyManager dependencyManager;
    private List<Module> modules;

    @BeforeEach
    public void init() throws Exception {
        var engine = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/modules_with_errors")
                .build();
        dependencyManager = engine.getDependencyManager();
        compiledOpenClass = engine.getCompiledOpenClass();
        modules = engine.getProjectDescriptor().getModules();
    }

    @Test
    public void testMessagesDelegatingFromDependencies() throws Exception {

        var allMessages = getAllMessageForModule("Rules");
        var allMessages2 = getAllMessageForModule("Rules2");
        var allMessages3 = getAllMessageForModule("Rules3");

        assertMessages(allMessages2, allMessages);
        assertMessages(allMessages3, allMessages);
        assertMessages(allMessages3, allMessages2);
    }

    @Test
    public void testMessagesGatheringInMultiModule() throws Exception {
        var allMessages = compiledOpenClass.getAllMessages();
        for (Module module : modules) {
            assertMessages(allMessages, getAllMessageForModule(module.getName()));
        }
    }

    private Collection<OpenLMessage> getAllMessageForModule(String rules) throws Exception {
        // it is passed through the dependency manager to receive the same
        // instances of OpenLMessages
        IDependency dependency = new Dependency(DependencyType.MODULE,
                new IdentifierNode(IXlsTableNames.DEPENDENCY, null, rules, null));
        Collection<ResolvedDependency> resolvedDependencies = dependencyManager.resolveDependency(dependency, false);
        return dependencyManager.loadDependency(resolvedDependencies.iterator().next()).getCompiledOpenClass().getAllMessages();
    }

    private static void assertMessages(Collection<OpenLMessage> parent, Collection<OpenLMessage> child) {
        assertTrue(parent.size() >= child.size());
        assertTrue(parent.containsAll(child));
    }

}
