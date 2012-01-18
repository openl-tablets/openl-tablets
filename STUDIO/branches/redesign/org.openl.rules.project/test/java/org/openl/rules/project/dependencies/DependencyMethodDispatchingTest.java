package org.openl.rules.project.dependencies;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.project.instantiation.ApiBasedEngineFactoryInstantiationStrategy;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;
import org.openl.rules.runtime.RulesFileDependencyLoader;

public class DependencyMethodDispatchingTest {

	/**
	 * Checks that one module includes another as dependency. Both of them
	 * contains the identical methods by signatures, without dimension
	 * properties. The expected result: both methods will be wrapped with
	 * dispatcher and ambigious method exception will be thrown at runtime.
	 */
	@Test
	public void testAmbigiousMethodException() {
		final String MODULES_FOLDER = "test/resources/dependencies/testMethodDispatching";
		// AmbigiousMethodException can be retrieved in only the dispatching
		// mode based on methods selecting in java code
		ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
		ProjectDescriptor descr = strategy.resolveProject(new File(
				MODULES_FOLDER));

		RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();

		RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
		ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader(
				MODULES_FOLDER);

		dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
		boolean executionMode = false;
		dependencyManager.setExecutionMode(executionMode);

		ApiBasedEngineFactoryInstantiationStrategy s = new ApiBasedEngineFactoryInstantiationStrategy(
				descr.getModules().get(0), executionMode, dependencyManager);

		Class<?> interfaceClass = s.getServiceClass();
		Method method = null;
		try {
			method = interfaceClass.getMethod("hello1",
					new Class[] { int.class });
		} catch (Throwable e1) {
			fail("Method should exist.");
		}

		try {
			method.invoke(s.instantiate(ReloadType.NO), 10);
			fail("We are waiting for OpenlRuntimeException");
		} catch (Exception e) {
			assertTrue(e.getCause().getMessage()
					.contains("Ambiguous method dispatch"));
		}
	}

	/**
	 * Check that main module contains overloaded by property table. Dependency
	 * contains the invokable table(start) that calls the table that is
	 * overloaded by property. Checks, that on invoke the table from dependency
	 * module will work. As it was compiled separately, and know nothing about
	 * the overloaded table in main module.
	 */
	@Test
	public void testMethodDispatching() {
		final String MODULES_FOLDER = "test/resources/dependencies/testMethodDispatching1";

		ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
		ProjectDescriptor descr = strategy.resolveProject(new File(
				MODULES_FOLDER));

		RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
		ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader(
				MODULES_FOLDER);

		RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
		dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
		boolean executionMode = true;
		dependencyManager.setExecutionMode(executionMode);

		ApiBasedEngineFactoryInstantiationStrategy s = new ApiBasedEngineFactoryInstantiationStrategy(
				descr.getModules().get(0), executionMode, dependencyManager);

		RulesServiceEnhancer enhancer = new RulesServiceEnhancer(s);

		Class<?> interfaceClass = null;
		try {
			interfaceClass = enhancer.getServiceClass();
		} catch (Exception e2) {
			fail("Should instantiate");
		}
		Method method = null;
		try {
			// get the method from dependency module for invoke
			//
			method = interfaceClass.getMethod("start",
					new Class[] { IRulesRuntimeContext.class });
		} catch (Throwable e1) {
			fail("Method should exist.");
		}

		DefaultRulesRuntimeContext context = new DefaultRulesRuntimeContext();

		// set the state as in main module
		//
		context.setUsState(UsStatesEnum.LA);

		try {
			// check that method from dependency will be invoked
			//
			assertEquals(2,
					method.invoke(enhancer.instantiate(ReloadType.NO), context));
			assertTrue(true);
		} catch (Exception e) {
			fail("We should get the right result");
		}
	}

}
