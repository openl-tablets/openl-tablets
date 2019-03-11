package org.openl.rules;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Ignore;
import org.openl.rules.runtime.RulesEngineFactory;

@Ignore("Auxiliary class")
public class TestUtils {
    public static void assertEx(Exception ex, String... errorMessages) {
        if (ex == null) {
            throw new RuntimeException("Exception is null! It works when should fail!!!");
        }

        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);

        pw.close();

        String s = sw.toString();
        if (errorMessages.length == 1) {
            if (!s.contains(errorMessages[0])) {
                throw new RuntimeException("Expect to see '" + errorMessages[0] + "' in stack trace.", ex);
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

    public static void assertEx(Runnable closure, String... errorMessages) {
        Exception ex = null;
        try {
            closure.run();
        } catch (Exception e) {
            ex = e;
        }
        assertEx(ex, errorMessages);
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
}
