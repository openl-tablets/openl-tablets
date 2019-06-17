package org.openl.rules.activiti;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.task.Task;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.meta.DoubleValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activiti.cfg.xml" })
public class SimpleXlsOpenLServiceTaskTest {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    private ProcessEngine processEngine;

    @Before
    public void deploy() {
        processEngine = processEngineConfiguration.buildProcessEngine();
        processEngine.getRepositoryService()
            .createDeployment()
            .addClasspathResource("activiti-definition-openl-xls-test.bpmn20.xml")
            .addClasspathResource("Tutorial1 - Intro to Decision Tables.xlsx")
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

        DoubleValue result = (DoubleValue) processEngine.getRuntimeService()
            .getVariable(task.getExecutionId(), "resultVariable");

        Assert.assertEquals(500.0d, result.doubleValue(), 1e-3);
    }
}
