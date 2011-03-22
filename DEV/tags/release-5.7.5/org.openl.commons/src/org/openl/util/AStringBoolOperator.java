/**
 * Created Apr 19, 2007
 */
package org.openl.util;

import java.lang.reflect.Constructor;

import org.openl.base.NamedThing;

/**
 * Abstract class for comparison two strings. Its children implement
 * different operations for string comparison.
 *  
 * @author snshor
 *
 */
public abstract class AStringBoolOperator extends NamedThing implements IStringBoolOperator {

    public static class ContainsOperator extends AStringBoolOperator {

        public ContainsOperator(String sample) {
            super("contains", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test != null && test.indexOf(sample) >= 0;
        }
    }

    public static class EndsWithOperator extends AStringBoolOperator {

        public EndsWithOperator(String sample) {
            super("ends with", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test.endsWith(sample);
        }
    }

    public static class EqualsIgnoreCaseOperator extends AStringBoolOperator {

        public EqualsIgnoreCaseOperator(String sample) {
            super("equals ignore case", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test.equalsIgnoreCase(sample);
        }
    }

    public static class EqualsOperator extends AStringBoolOperator {

        public EqualsOperator(String sample) {
            super("equals", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test.equals(sample);
        }
    }

    public static class Holder {
        
        private AStringBoolOperator operator;
        private String name = "equals";
        private String sample = "";

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
            return operator.isMatching(test);
        }

        public void setName(String name) {
            this.name = name;
            operator = makeOperator(name, sample);
        }

        public void setSample(String sample) {
            this.sample = sample;
            if (operator != null) {
                operator.setSample(sample);
            }
        }

    }

    public static class MatchesOperator extends AStringBoolOperator {

        public MatchesOperator(String sample) {
            super("matches", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test.matches(sample);
        }
    }

    public static class StartsWithOperator extends AStringBoolOperator {

        public StartsWithOperator(String sample) {
            super("starts with", sample);
        }

        @Override
        public boolean isMatching(String sample, String test) {
            return test.startsWith(sample);
        }
    }

    private static Class<?>[] allTypes = { ContainsOperator.class, MatchesOperator.class, EqualsOperator.class,
            EqualsIgnoreCaseOperator.class, StartsWithOperator.class, EndsWithOperator.class };

    private String sampleStr;

    public static String[] getAllOperatorNames() {
        String[] res = new String[allTypes.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = getOperatorName(allTypes[i]);
        }
        return res;
    }

    public static Class<?> findOperatorClass(String name) {
        for (int i = 0; i < allTypes.length; i++) {
            Class<?> c = allTypes[i];
            if (getOperatorName(c).equals(name)) {
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
            Constructor<?> constructor = c.getConstructor(new Class[] { String.class });
            AStringBoolOperator operator = (AStringBoolOperator) constructor.newInstance(new Object[] { sample });
            return operator;
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    private static String getOperatorName(Class<?> c) {
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

    public boolean isMatching(String test) {
        return isMatching(sampleStr, test);
    }
    
    public abstract boolean isMatching(String op1, String op2);

    public boolean opReverse(String test) {
        return isMatching(test, sampleStr);
    }

    public void setSample(String sample) {
        sampleStr = sample;
    }

}
