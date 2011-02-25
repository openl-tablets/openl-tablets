package org.openl.rules.democase;

import java.text.SimpleDateFormat;

import junit.framework.Assert;

import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.message.OpenLMessage;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

import static junit.framework.Assert.*;

public class LEIntegrationTest {

    private static final String TEST_DATE = "2010/05/21-08:00";
    private static final String src = "test/openl-project/SimpleLiveExcellUsage.xls";
    private OpenClassJavaWrapper wrapper;

    public LEIntegrationTest() {
        buildXlsModuleSyntaxNode(src);        
    }
    
    protected void buildXlsModuleSyntaxNode(String fileToBuildWrapper) {        
        buildJavaWrapper(fileToBuildWrapper);
    }
    
    protected OpenClassJavaWrapper buildJavaWrapper(String fileToBuildWrapper) {        
        UserContext ucxt = initUserContext();
        wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, fileToBuildWrapper, false, null);
        return wrapper;
    }

    protected UserContext initUserContext() {
        return new UserContext(Thread.currentThread().getContextClassLoader(), ".");        
    }

    @Test
    public void test() throws Exception {
        if (wrapper.getCompiledClass().hasErrors()) {
            for (OpenLMessage message : wrapper.getCompiledClass().getMessages()) {
                System.out.println(message);
            }
            assertFalse(true);
        } else {
            assertEquals(282.19, invokeMethodWithEnvProperties("testLE"));
        }
    }

    protected Object invokeMethodWithEnvProperties(String methodName) throws Exception{
        IOpenClass __class = wrapper.getOpenClassWithErrors();
        IOpenMethod testMethod = __class.getMatchingMethod(methodName, new IOpenClass[] {});

        Assert.assertNotNull(String.format("Method with name %s exists", methodName), testMethod);

        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        DefaultRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCurrentDate(new SimpleDateFormat(com.exigen.le.smodel.Type.DATE_FORMAT).parse(TEST_DATE));
        environment.setContext(context);
        Object __myInstance = __class.newInstance(environment);

        Object result = testMethod.invoke(__myInstance, new Object[0], environment);

        return result;
    }
}
