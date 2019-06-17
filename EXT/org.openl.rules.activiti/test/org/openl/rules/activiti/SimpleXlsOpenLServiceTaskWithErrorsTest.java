package org.openl.rules.activiti;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.pvm.PvmException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activiti.cfg.xml" })
public class SimpleXlsOpenLServiceTaskWithErrorsTest {

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    private ProcessEngine processEngine;

    @Before
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
        Assert.assertNotNull(processEngine);
        Map<String, Object> variables = new HashMap<>();

        variables.put("driverAge", "Standard Driver");
        variables.put("driverMaritalStatus", "Single");

        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("openLTaskServiceTest", variables);
        } catch (PvmException e) {
            Assert.assertTrue(e.getCause() instanceof RulesInstantiationException);
        }
    }
}
