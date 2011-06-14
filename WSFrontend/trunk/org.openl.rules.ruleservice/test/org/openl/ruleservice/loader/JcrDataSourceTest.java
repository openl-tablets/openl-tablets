package org.openl.ruleservice.loader;

import static org.junit.Assert.*;                                                                                                     


import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;

public class JcrDataSourceTest {
    
    private static IDataSource dataSource;
    
    @BeforeClass
    public static void setDataSource(){
        dataSource = new JcrDataSource();
    }
    
	@Test
	public void testJcrDataSource() {
		assertNotNull(dataSource);
	}
	
	@Test
    public void testGetDeployments() {
        List<Deployment> deployments = dataSource.getDeployments();
        assertTrue(deployments.size() > 0);
    }

	@Test
    public void testGetDeployment() {
	    CommonVersion commonVersion = new CommonVersionImpl("0.0.1");
	    Deployment deployment = dataSource.getDeployment("org.openl.tablets.tutorial4", commonVersion);
        assertNotNull(deployment);
        assertEquals("org.openl.tablets.tutorial4", deployment.getDeploymentName());
        assertEquals("0.0.1", deployment.getCommonVersion().getVersionName());
    }
}
