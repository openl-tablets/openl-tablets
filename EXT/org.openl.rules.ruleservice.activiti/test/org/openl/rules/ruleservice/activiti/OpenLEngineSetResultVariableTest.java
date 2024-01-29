package org.openl.rules.ruleservice.activiti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestPropertySource(properties = {"ruleservice.isProvideRuntimeContext=false",
    "ruleservice.instantiation.strategy.lazy = false",
    "production-repository.uri=test-resources/datasource",
    "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:activiti.cfg.xml"})
public class OpenLEngineSetResultVariableTest {

    @Autowired
    private ProcessEngine processEngine;

    @BeforeEach
    public void deploy() {
        processEngine.getRepositoryService()
            .createDeployment()
            .addClasspathResource("activiti-definition-setResultVariable.bpmn20.xml")
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
