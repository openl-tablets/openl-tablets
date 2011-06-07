package org.openl.ruleservice.loader;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.resolving.EclipseBasedResolvingStrategy;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class RulesLoaderTest {
	@Test
	public void testResolveModulesForProject() {
		IDataSource jcrDataSource = new JcrDataSource();
		List<Deployment> deployments = jcrDataSource.getDeployments();
		assertTrue(deployments.size() > 0);
		Deployment deployment = deployments.get(0);
		
		RulesProjectResolver projectResolver = new RulesProjectResolver();
		List<ResolvingStrategy> resolvingStrategies = new ArrayList<ResolvingStrategy>();
		resolvingStrategies.add(new org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy());
		EclipseBasedResolvingStrategy eclipseBasedResolvingStrategy = new org.openl.rules.project.resolving.EclipseBasedResolvingStrategy();
		eclipseBasedResolvingStrategy.setTreeAdaptor(new org.openl.util.tree.FileTreeIterator.FileTreeAdaptor());
		resolvingStrategies.add(eclipseBasedResolvingStrategy);
		resolvingStrategies.add(new org.openl.rules.project.resolving.SimpleXlsResolvingStrategy());
		projectResolver.setResolvingStrategies(resolvingStrategies);
		RulesLoader rulesLoader = new RulesLoader(jcrDataSource, new LocalTemporaryDeploymentsStorage(), projectResolver);

		rulesLoader.resolveModulesForProject("org.openl.tablets.tutorial4", deployment.getCommonVersion(), "org.openl.tablets.tutorial4");
	}
}
