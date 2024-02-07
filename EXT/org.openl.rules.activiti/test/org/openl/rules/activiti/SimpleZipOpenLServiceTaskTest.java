package org.openl.rules.activiti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = {"classpath:activiti.cfg.xml"})
public class SimpleZipOpenLServiceTaskTest {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    private ProcessEngine processEngine;

    @BeforeEach
    public void deploy() {
        processEngine = processEngineConfiguration.buildProcessEngine();
        processEngine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("activiti-definition-openl-zip-test.bpmn20.xml")
                .addClasspathResource("Tutorial 1 - Introduction to Decision Tables-1.zip")
                .deploy();
    }

    @Test
    public void test() {
        assertNotNull(processEngine);
        Map<String, Object> variables = new HashMap<>();

        variables.put("driverAge", "Standard Driver");
        variables.put("driverMaritalStatus", "Single");

        processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();

        Double result = (Double) processEngine.getRuntimeService()
                .getVariable(task.getExecutionId(), "resultVariable");

        assertEquals(500.0d, result, 1e-3);
    }
}
