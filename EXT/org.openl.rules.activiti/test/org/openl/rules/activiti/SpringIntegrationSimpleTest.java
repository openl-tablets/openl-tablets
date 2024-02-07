package org.openl.rules.activiti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = {"classpath:activiti-spring.cfg.xml"})
public class SpringIntegrationSimpleTest {

    @Autowired
    private ProcessEngine processEngine;

    @BeforeEach
    public void deploy() {
        processEngine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("activiti-definition-spring-integration-test.bpmn20.xml")
                .addClasspathResource("Tutorial1 - Intro to Decision Tables.xlsx")
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

        assertEquals("result task 1", task.getName());

        processEngine.getTaskService().complete(task.getId());

        // Test second condition
        variables.put("driverAge", "Senior Driver");
        variables.put("driverMaritalStatus", "Single");

        processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);

        task = processEngine.getTaskService().createTaskQuery().singleResult();

        assertEquals("result task 2", task.getName());

        processEngine.getTaskService().complete(task.getId());
    }
}
