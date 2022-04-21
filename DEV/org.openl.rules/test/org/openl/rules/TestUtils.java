package org.openl.rules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Ignore;
import org.openl.message.OpenLMessage;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.syntax.exception.CompositeOpenlException;

@Ignore("Auxiliary class")
public class TestUtils {

    public static final int CYCLIC_DEPENDENCY_THRESHOLD = 100;

    public static void assertEx(Exception ex, String... errorMessages) {
        if (ex == null) {
            throw new RuntimeException("Exception is null! It works when should fail.");
        }

        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);

        pw.close();

        String s = sw.toString();
        if (errorMessages.length == 1) {
            if (!s.contains(errorMessages[0])) {
                throw new RuntimeException(String.format("Expect to see '%s' in stack trace.", errorMessages[0]), ex);
            }
        } else {
            boolean[] ok = new boolean[errorMessages.length];
            boolean allOk = true;
            for (int i = 0; i < errorMessages.length; i++) {
                ok[i] = s.contains(errorMessages[i]);
                if (!ok[i]) {
                    allOk = false;
                }
            }

            if (!allOk) {
                StringBuilder sb = new StringBuilder("Not all fragments are present in stack trace:\n");
                for (int i = 0; i < errorMessages.length; i++) {
                    sb.append("  ").append(ok[i] ? '+' : '-').append(" ");
                    sb.append(errorMessages[i]);
                    sb.append("\n");
                }

                throw new RuntimeException(sb.toString(), ex);
            }
        }

    }

    public static OpenLMessage[] collectErrorMessagesFromFileProcessing(String sourceFile) {
        try {
            RulesEngineFactory<Object> engineFactory = new RulesEngineFactory<>(sourceFile);
            engineFactory.newEngineInstance();
        } catch (Exception ex) {
            Throwable throwable = ex;
            int protectionFromCyclicDependency = 0;
            while (protectionFromCyclicDependency < CYCLIC_DEPENDENCY_THRESHOLD
                    && throwable != null
                    && ! (throwable instanceof CompositeOpenlException)) {
                throwable = throwable.getCause();
                protectionFromCyclicDependency++;
            }
            if (throwable instanceof CompositeOpenlException) {
                CompositeOpenlException compositeOpenlException = (CompositeOpenlException) throwable;
                return compositeOpenlException.getErrorMessages();
            }
        }
        return new OpenLMessage[0];
    }

    public static void assertErrorMessagesArePresent(OpenLMessage[] messages, String... expectedMessages) {
        for (String expectedMessage: expectedMessages) {
            boolean messageIsFound = false;
            for (OpenLMessage message: messages) {
                if (message.getSummary().contains(expectedMessage)) {
                    messageIsFound = true;
                    break;
                }
            }
            if (! messageIsFound) {
                Assert.fail("Message \"" + expectedMessage + "\" is expected, but has not been found");
            }
        }
    }

    public static void assertErrorMessagesAreAbsent(OpenLMessage[] messages, String... nonExpectedMessages) {
        for (String nonExpectedMessage: nonExpectedMessages) {
            for (OpenLMessage message: messages) {
                if (message.getSummary().contains(nonExpectedMessage)) {
                    Assert.fail("Message \"" + nonExpectedMessage + "\" is not expected but has been found");
                }
            }
        }
    }

    public static void assertEx(String sourceFile, String... errorMessages) {
        try {
            RulesEngineFactory<Object> engineFactory = new RulesEngineFactory<>(sourceFile);
            engineFactory.newEngineInstance();
        } catch (Exception ex) {
            assertEx(ex, errorMessages);
            return;
        }
        Assert.fail();
    }

    public static <T> T create(String sourceFile, Class<T> tClass) {
        RulesEngineFactory<T> engineFactory = new RulesEngineFactory<>(sourceFile, tClass);
        return engineFactory.newEngineInstance();
    }

    public static <T> T create(String sourceFile) {
        RulesEngineFactory<T> engineFactory = new RulesEngineFactory<>(sourceFile);
        return engineFactory.newEngineInstance();
    }

    public static Object invoke(String sourceFile, String methodName, Object... args) {
        Object instance = create(sourceFile);
        return invoke(instance, methodName, args);
    }

    public static <T> T invoke(Object instance, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method method = instance.getClass().getMethod(methodName, types);
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            RuntimeException exc;
            if (targetException instanceof RuntimeException) {
                exc = (RuntimeException) targetException;
            } else {
                exc = new IllegalStateException(targetException);
            }
            throw exc;
        }
    }

    public static <T> T invoke(Object instance, String methodName, Object... args) {
        Class<?>[] types;
        if (args == null) {
            types = null;
        } else {
            types = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i].getClass();
            }
        }
        return invoke(instance, methodName, types, args);
    }
}
