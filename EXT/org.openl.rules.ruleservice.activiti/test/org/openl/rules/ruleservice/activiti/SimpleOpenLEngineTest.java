package org.openl.rules.ruleservice.activiti;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.task.Task;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false",
        "ruleservice.datasource.dir=test-resources/datasource" })
@ContextConfiguration(locations = { "classpath:activiti.cfg.xml" })
public class SimpleOpenLEngineTest {

    @Autowired
    private ProcessEngine processEngine;

    @Before
    public void deploy() {
        processEngine.getRepositoryService()
            .createDeployment()
            .addClasspathResource("activiti-definition.bpmn20.xml")
            .deploy();
    }

    @Test
    public void test() {
        Assert.assertNotNull(processEngine);
        Map<String, Object> variables = new HashMap<>();

        variables.put("driverAge", "Standard Driver");
        variables.put("driverMaritalStatus", "Single");

        processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();

        Assert.assertEquals("result task 1", task.getName());

        processEngine.getTaskService().complete(task.getId());

        // Test second condition
        variables.put("driverAge", "Senior Driver");
        variables.put("driverMaritalStatus", "Single");

        processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);

        task = processEngine.getTaskService().createTaskQuery().singleResult();

        Assert.assertEquals("result task 2", task.getName());

        processEngine.getTaskService().complete(task.getId());
    }
}
