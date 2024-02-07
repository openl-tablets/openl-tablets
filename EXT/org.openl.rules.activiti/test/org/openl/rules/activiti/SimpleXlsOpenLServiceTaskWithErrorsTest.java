package org.openl.rules.activiti;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.pvm.PvmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.project.instantiation.RulesInstantiationException;

@SpringJUnitConfig(locations = {"classpath:activiti.cfg.xml"})
public class SimpleXlsOpenLServiceTaskWithErrorsTest {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    private ProcessEngine processEngine;

    @BeforeEach
    public void deploy() {
        processEngine = processEngineConfiguration.buildProcessEngine();
        processEngine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("activiti-definition-errors-test.bpmn20.xml")
                .addClasspathResource("Tutorial1 - Intro to Decision Tables - errors.xlsx")
                .deploy();
    }

    @Test
    public void test() {
        assertNotNull(processEngine);
        Map<String, Object> variables = new HashMap<>();

        variables.put("driverAge", "Standard Driver");
        variables.put("driverMaritalStatus", "Single");

        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);
        } catch (PvmException e) {
            assertTrue(e.getCause() instanceof RulesInstantiationException);
        }
    }
}
