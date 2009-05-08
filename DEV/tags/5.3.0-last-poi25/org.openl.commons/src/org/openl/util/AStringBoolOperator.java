/**
 * Created Apr 19, 2007
 */
package org.openl.util;

import java.lang.reflect.Constructor;

import org.openl.base.NamedThing;

/**
 * @author snshor
 *
 */
public abstract class AStringBoolOperator extends NamedThing implements IStringBoolOperator {

    static public class ContainsOperator extends AStringBoolOperator {

        public ContainsOperator(String sample) {
            super("contains", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.indexOf(sample) >= 0;
        }
    }

    static public class EndsWithOperator extends AStringBoolOperator {

        public EndsWithOperator(String sample) {
            super("ends with", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.endsWith(sample);
        }
    }

    static public class EqualsIgnoreCaseOperator extends AStringBoolOperator {

        public EqualsIgnoreCaseOperator(String sample) {
            super("equals ignore case", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.equalsIgnoreCase(sample);
        }
    }

    static public class EqualsOperator extends AStringBoolOperator {

        public EqualsOperator(String sample) {
            super("equals", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.equals(sample);
        }
    }

    static public class Holder {
        AStringBoolOperator op;

        String name = "equals";

        String sample = "";

        /**
         * @param opType
         * @param value2
         */
        public Holder(String opType, String value2) {
            setName(opType);
        }
        public String getName() {
            return name;
        }

        public String getSample() {
            return sample;
        }

        public boolean op(String test) {
            return op.op(test);
        }

        public void setName(String name) {
            this.name = name;
            op = makeOperator(name, sample);
        }

        public void setSample(String sample) {
            this.sample = sample;
            if (op != null) {
                op.setSample(sample);
            }
        }

    }

    static public class MatchesOperator extends AStringBoolOperator {

        public MatchesOperator(String sample) {
            super("matches", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.matches(sample);
        }
    }

    static public class StartsWithOperator extends AStringBoolOperator {

        public StartsWithOperator(String sample) {
            super("starts with", sample);
        }

        @Override
        public boolean op(String sample, String test) {
            return test.startsWith(sample);
        }
    }

    static public Class<?>[] allTypes = { ContainsOperator.class, MatchesOperator.class, EqualsOperator.class,
            EqualsIgnoreCaseOperator.class, StartsWithOperator.class, EndsWithOperator.class };

    String sampleStr;

    static public String[] allNames() {
        String[] res = new String[allTypes.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = opName(allTypes[i]);
        }
        return res;
    }

    public static Class<?> findOperatorClass(String name) {
        for (int i = 0; i < allTypes.length; i++) {
            Class<?> c = allTypes[i];
            if (opName(c).equals(name)) {
                return c;
            }

        }
        return null;
    }

    public static AStringBoolOperator makeOperator(String name, String sample) {

        Class<?> c = findOperatorClass(name);

        if (c == null) {
            throw new RuntimeException("Operator not found: " + name);
        }

        try {
            Constructor<?> ctr = c.getConstructor(new Class[] { String.class });
            AStringBoolOperator op = (AStringBoolOperator) ctr.newInstance(new Object[] { sample });
            return op;
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    static String opName(Class<?> c) {
        String cname = StringTool.lastToken(c.getName(), "$");
        String name = StringTool.decapitalizeName(cname, " ");
        name = name.substring(0, name.length() - " operator".length());
        return name;
    }

    public AStringBoolOperator(String name, String sample) {
        super(name);
        sampleStr = sample;
    }

    public String getSample() {
        return sampleStr;
    }

    public boolean op(String test) {
        return op(sampleStr, test);
    }

    public abstract boolean op(String op1, String op2);

    public boolean opReverse(String test) {
        return op(test, sampleStr);
    }

    public void setSample(String sample) {
        sampleStr = sample;
    }

}
