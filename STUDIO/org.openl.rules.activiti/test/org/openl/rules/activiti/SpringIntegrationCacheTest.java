package org.openl.rules.activiti;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activiti-spring.cfg.xml" })
public class SpringIntegrationCacheTest {

    @Autowired
    private ProcessEngine processEngine;

    @Test
    public void test() {
        Assert.assertNotNull(processEngine);

        Deployment deployment = processEngine.getRepositoryService()
            .createDeployment()
            .addClasspathResource("activiti-definition-spring-integration-cache-test.bpmn20.xml")
            .addClasspathResource("Tutorial1 - Intro to Decision Tables.xlsx")
            .deploy();

        Map<String, Object> variables = new HashMap<>();

        variables.put("driverAge", "Standard Driver");
        variables.put("driverMaritalStatus", "Single");

        processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        Assert.assertEquals("result task", task.getName());

        Double result = (Double) processEngine.getRuntimeService().getVariable(task.getExecutionId(), "resultVariable");
        Assert.assertEquals(500d, result, 1e-8);

        processEngine.getTaskService().complete(task.getId());

        processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);

    }
}
