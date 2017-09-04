package org.openl.rules.dt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class FormulasInConditionDTTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "./test/rules/dt/FormulasInConditionDTTest.xls";

    public FormulasInConditionDTTest() {
        super(SRC, true);
    }

    @Test
    public void test() throws Exception {
        
        Class<?> inputClass = getCompiledOpenClass().getClassLoader().loadClass("org.openl.generated.beans.Input");
        Class<?> outputClass = getCompiledOpenClass().getClassLoader().loadClass("org.openl.generated.beans.Output");
        
        IOpenMethod method = getCompiledOpenClass().getOpenClass().getMethod("check",
            new IOpenClass[] { JavaOpenClass.getOpenClass(inputClass), JavaOpenClass.getOpenClass(outputClass) });
        Object instance = newInstance();
        SimpleRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
        
        Object input = inputClass.newInstance();
        Object output = outputClass.newInstance();
        
        inputClass.getMethod("setText", String.class).invoke(input, "X");
        outputClass.getMethod("setResult", String.class).invoke(output, "QWE");
        
        Object result = method.invoke(instance, new Object[] { input, output }, env);
        assertEquals("abc", outputClass.getMethod("getResult").invoke(result));
        
        inputClass.getMethod("setText", String.class).invoke(input, "Y");
        outputClass.getMethod("setResult", String.class).invoke(output, "QWE");
        
        result = method.invoke(instance, new Object[] { input, output }, env);
        assertEquals("zxc", outputClass.getMethod("getResult").invoke(result));

        inputClass.getMethod("setText", String.class).invoke(input, "Q");
        outputClass.getMethod("setResult", String.class).invoke(output, "QWE");
        
        result = method.invoke(instance, new Object[] { input, output }, env);
        assertTrue(result == output);
        
    }

 }

