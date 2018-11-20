package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

@RunWith(MockitoJUnitRunner.class)
public class TestDescriptionTest {
    @Mock
    private IRuntimeEnv env;
    @Mock
    private Object target;

    private SomeArgument[] arguments;
    private TestRunner testRunner = new TestRunner(TestUnit.Builder.getInstance());
    private OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();

    @Before
    public void setUp() {
        arguments = new SomeArgument[] { new SomeArgument("test") };
    }

    @Test
    public void testNotModifyInputParameters() {

        TestDescription description = new TestDescription(createTestMethodMock(), arguments, null);

        testRunner.runTest(description, target, env, cloner, 1);
        assertEquals("test", arguments[0].value);

        testRunner.runTest(description, target, env, cloner, 5);
        assertEquals("test", arguments[0].value);
    }

    private IOpenMethod createTestMethodMock() {
        IMethodSignature signature = createMethodSignatureMock();
        IOpenMethod method = mock(IOpenMethod.class);

        when(method.getSignature()).thenReturn(signature);

        when(method.invoke(any(), any(Object[].class), any(IRuntimeEnv.class))).thenAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] params = (Object[]) invocation.getArguments()[1];

                // Modify method input parameters
                SomeArgument someArgument = (SomeArgument) params[0];
                someArgument.value = "modified" + someArgument.value;
                return null;
            }
        });

        return method;
    }

    private IMethodSignature createMethodSignatureMock() {
        IMethodSignature signature = mock(IMethodSignature.class);
        when(signature.getNumberOfParameters()).thenReturn(arguments.length);

        when(signature.getParameterName(anyInt())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "param" + invocation.getArguments()[0];
            }
        });

        return signature;
    }

    private static class SomeArgument {
        public String value;

        public SomeArgument(String value) {
            this.value = value;
        }
    }
}
