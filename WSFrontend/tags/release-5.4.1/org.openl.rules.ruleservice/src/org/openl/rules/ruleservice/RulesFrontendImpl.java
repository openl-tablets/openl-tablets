package org.openl.rules.ruleservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.main.OpenLWrapper;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.DeploymentListener;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.publish.JavaClassDeploymentAdmin;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM;

public class RulesFrontendImpl extends RuleServiceBase implements RulesFrontend {
    private static final Log log = LogFactory.getLog(RulesFrontendImpl.class);

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
    protected void finalize() {
        close();
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
        loader = new RulesLoader();
        resolver = new RulesProjectResolver();
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
        deployAdmin = deploymentAdmin;
    }

    protected void registerProjects(String deploymentName, Map<String, OpenLWrapper> ruleModules) {
        runningDeployments.put(deploymentName, ruleModules);
        log.info(String.format("Started exposing deployment \"{1}\" with rules modules {2}", deploymentName,
                ruleModules.keySet().toString()));
    }

    private void startFrontend() {
        frontendExecutor = new Thread(new Runnable() {
            public void run() {
                try {
                    runFrontend();
                } catch (RRepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        frontendExecutor.setDaemon(true);
        frontendExecutor.start();
    }

    protected void unregisterProjects(String deploymentName) {
        runningDeployments.remove(deploymentName);
        log.info(String.format("Stoped exposing deployment \"{1}\" ", deploymentName));
    }

}
