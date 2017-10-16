package org.openl.codegen.functions.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.openl.codegen.tools.CodeGenTools;
import org.openl.codegen.tools.GenRulesCode;
import org.openl.codegen.tools.VelocityTool;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;

/**
 * Generates common functions for children of {@link org.openl.meta.explanation.ExplanationNumberValue}.
 * 
 * @author DLiauchuk
 * 
 */
public class GenNumberValueFunctions extends GenRulesCode {

    /**
     * key - the type for which methods need to be generated. value - inner so
     * called primitive type of the wrapper class
     **/
    private static final Map<Class<?>, Class<?>> types = new HashMap<Class<?>, Class<?>>();

    /** first array of functions with equal implementation **/
    private static final NumberOperations[] MATH_FUNCTIONS1 = new NumberOperations[3];

    /** second array of functions with equal implementation **/
    private static final NumberOperations[] MATH_FUNCTIONS2 = new NumberOperations[2];

    /** third array of functions with equal implementation **/
    private static final NumberOperations[] MATH_FUNCTIONS3 = new NumberOperations[2];

    /** array of primitive java numeric types **/
    protected static final Class<?>[] primitiveNumericTypes = new Class<?>[] { byte.class, short.class, int.class,
            long.class, float.class, double.class };

    protected static final Class<?>[] wrapperNumericTypes = new Class<?>[] { Byte.class, Short.class, Integer.class,
            Long.class, Float.class, Double.class };

    protected static final Class<?>[] BIG_NUMERIC_TYPES = new Class<?>[] { BigInteger.class, BigDecimal.class };

    public static void main(String[] arg) throws Exception {
        new GenNumberValueFunctions().run();
    }

    public void run() throws Exception {
        
        //VALUE TYPES HAS BEEN CHANGES MANUALLY!!!!! GENERATOR INVALID!!
        
        //init();
        //generateFunctions();
    }

    @SuppressWarnings("unused")
    private void init() {
        MATH_FUNCTIONS1[0] = NumberOperations.AVG;
        MATH_FUNCTIONS1[1] = NumberOperations.SUM;
        MATH_FUNCTIONS1[2] = NumberOperations.MEDIAN;

        MATH_FUNCTIONS2[0] = NumberOperations.MAX;
        MATH_FUNCTIONS2[1] = NumberOperations.MIN;

        MATH_FUNCTIONS3[0] = NumberOperations.MAX_IN_ARRAY;
        MATH_FUNCTIONS3[1] = NumberOperations.MIN_IN_ARRAY;

        types.put(ByteValue.class, byte.class);
        types.put(ShortValue.class, short.class);
        types.put(IntValue.class, int.class);
        types.put(LongValue.class, long.class);
        types.put(FloatValue.class, float.class);
        types.put(DoubleValue.class, double.class);
        types.put(BigIntegerValue.class, BigInteger.class);
        types.put(BigDecimalValue.class, BigDecimal.class);
    }

    @SuppressWarnings("unused")
    private void generateFunctions() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        // generate functions for each type
        for (Class<?> clazz : types.keySet()) {
            String sourceFilePath = CodeGenTools.getClassSourcePathInCoreModule(clazz);
            variables.put("tool", new VelocityTool());
            variables.put("addFormula", Formulas.ADD);
            variables.put("multiplyFormula", Formulas.MULTIPLY);
            variables.put("subtractFormula", Formulas.SUBTRACT);
            variables.put("divideFormula", Formulas.DIVIDE);

            variables.put("remFormula", Formulas.REM);

            // variables.put("formulas", Formulas.values());
            variables.put("mathFunctions1", MATH_FUNCTIONS1);
            variables.put("mathFunctions2", MATH_FUNCTIONS2);
            variables.put("mathFunctions3", MATH_FUNCTIONS3);
            variables.put("copyFunction", NumberOperations.COPY);
            variables.put("productFunction", NumberOperations.PRODUCT);
            variables.put("modFunction", NumberOperations.MOD);
            variables.put("smallFunction", NumberOperations.SMALL);
            variables.put("bigFunction", NumberOperations.BIG);
            variables.put("powFunction", NumberOperations.POW);
            variables.put("absFunction", NumberOperations.ABS);
            variables.put("negativeFunction", NumberOperations.NEGATIVE);
            variables.put("incFunction", NumberOperations.INC);
            variables.put("positiveFunction", NumberOperations.POSITIVE);
            variables.put("decFunction", NumberOperations.DEC);
            variables.put("quaotientFunction", NumberOperations.QUOTIENT);
            variables.put("type", clazz);
            variables.put("primitiveType", types.get(clazz));
            variables.put("primitiveNumericTypes", primitiveNumericTypes);
            processSourceCode(sourceFilePath, "NumberValueChildren-functions.vm", variables);
        }

    }

}
