package org.openl.rules.ruleservice.simple;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.main.OpenLWrapper;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.RuleService;
import org.openl.rules.ruleservice.loader.JcrRulesLoader;
import org.openl.rules.ruleservice.publish.DeploymentListener;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.publish.JavaClassDeploymentAdmin;
import org.openl.rules.workspace.production.client.JcrRulesClient;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM;

public class RulesFrontendImpl extends RuleService implements RulesFrontend {
    private static final Log LOG = LogFactory.getLog(RulesFrontendImpl.class);

    private Thread frontendExecutor;

    private Map<String, Map<String, OpenLWrapper>> runningDeployments = new HashMap<String, Map<String, OpenLWrapper>>();

    public RulesFrontendImpl() {
        init();
        startFrontend();
    }

    public void close() {
        if (!frontendExecutor.isInterrupted()) {
            frontendExecutor.interrupt();
        }
    }

    public Object execute(String deployment, String ruleModule, String ruleName, Class<?>[] inputParamsTypes,
            Object[] params) {
        Object result = null;

        if (runningDeployments.containsKey(deployment)) {
            OpenLWrapper wrapper = runningDeployments.get(deployment).get(ruleModule);
            if (wrapper != null) {

                IOpenClass[] methodParamTypes = new IOpenClass[inputParamsTypes.length];
                for (int i = 0; i < inputParamsTypes.length; i++) {
                    methodParamTypes[i] = JavaOpenClass.getOpenClass(inputParamsTypes[i]);
                }

                IOpenClass openlClass = wrapper.getCompiledOpenClass().getOpenClassWithErrors();
                IOpenMethod openlMethod = openlClass.getMatchingMethod(ruleName, methodParamTypes);

                if (openlMethod != null) {
                    result = openlMethod.invoke(wrapper.getInstance(), params, new SimpleVM().getRuntimeEnv());
                }
            }
        }

        return result;
    }

    public Object execute(String deployment, String ruleModule, String ruleName, Object... params) {
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }

        return execute(deployment, ruleModule, ruleName, paramTypes, params);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public Object getValues(String deployment, String ruleModule, String fieldName) {
        Object result = null;

        if (runningDeployments.containsKey(deployment)) {
            OpenLWrapper wrapper = runningDeployments.get(deployment).get(ruleModule);
            if (wrapper != null) {
                IOpenClass openlClass = wrapper.getCompiledOpenClass().getOpenClassWithErrors();
                IOpenField openlField = openlClass.getField(fieldName);

                if (openlField != null) {
                    result = openlField.get(wrapper.getInstance(), new SimpleVM().getRuntimeEnv());
                }
            }
        }

        return result;
    }

    private void init() {
        try {
            loader = new JcrRulesLoader(new JcrRulesClient());
        } catch (RRepositoryException e) {
            LOG.error("Failed to intialize rules loader from JCR.", e);
        }
        publisher = new RulesPublisher();

        JavaClassDeploymentAdmin deploymentAdmin = new JavaClassDeploymentAdmin();

        DeploymentListener deploymentListener = new DeploymentListener() {
            public void afterDeployment(String deploymentName, Map<String, OpenLWrapper> ruleModules) {
                registerProjects(deploymentName, ruleModules);
            }

            public void afterUndeployment(String deploymentName) {
                // do not need this
            }

            public void beforeDeployment(String deploymentName) {
                // do not need this
            }

            public void beforeUndeployment(String deploymentName) {
                unregisterProjects(deploymentName);
            }
        };

        deploymentAdmin.addDeploymentListener(deploymentListener);
        publisher.setDeployAdmin(deploymentAdmin);
    }

    protected void registerProjects(String deploymentName, Map<String, OpenLWrapper> ruleModules) {
        runningDeployments.put(deploymentName, ruleModules);
        LOG.info(String.format("Started exposing deployment \"%s\" with rules modules %s", deploymentName,
                ruleModules.keySet().toString()));
    }

    private void startFrontend() {
        frontendExecutor = new Thread(this);

        frontendExecutor.setDaemon(true);
        frontendExecutor.start();
    }

    protected void unregisterProjects(String deploymentName) {
        runningDeployments.remove(deploymentName);
        LOG.info(String.format("Stoped exposing deployment \"%s\" ", deploymentName));
    }

}
