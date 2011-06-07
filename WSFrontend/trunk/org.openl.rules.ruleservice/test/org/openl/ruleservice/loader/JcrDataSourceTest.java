package org.openl.ruleservice.loader;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openl.rules.project.abstraction.Deployment;

public class JcrDataSourceTest {
	@Test
	public void testGetDeloyments() {
		IDataSource jcrDataSource = new JcrDataSource();
		List<Deployment> deployments = jcrDataSource.getDeployments();
		assertTrue(deployments.size() > 0);
	}
}
