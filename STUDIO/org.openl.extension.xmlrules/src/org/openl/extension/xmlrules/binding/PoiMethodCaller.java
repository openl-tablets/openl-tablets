package org.openl.extension.xmlrules.binding;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.eval.*;
import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class PoiMethodCaller implements IMethodCaller {
    private final Function function;
    private final FreeRefFunction functionRef;

    private final boolean returnsArray;
    private final boolean hasArrayParameter;
    private final JavaOpenMethod javaOpenMethod;

    private PoiMethodCaller(Function function, FunctionMetadata metaData) {
        this.function = function;
        this.functionRef = null;

        if (metaData != null) {
            byte returnClassCode = metaData.getReturnClassCode();
            returnsArray = returnClassCode == Ptg.CLASS_ARRAY || returnClassCode == Ptg.CLASS_REF;

            boolean hasArray = false;
            for (byte classCode : metaData.getParameterClassCodes()) {
                if (classCode == Ptg.CLASS_ARRAY || classCode == Ptg.CLASS_REF) {
                    hasArray = true;
                    break;
                }
            }

            hasArrayParameter = hasArray;
        } else {
            returnsArray = false;
            hasArrayParameter = false;
        }

        Method method;
        try {
            method = function.getClass().getMethod("evaluate", ValueEval[].class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        javaOpenMethod = new JavaOpenMethod(method);
    }

    private PoiMethodCaller(FreeRefFunction functionRef) {
        this.function = null;
        this.functionRef = functionRef;
        this.returnsArray = false;
        this.hasArrayParameter = false;

        Method method;
        try {
            method = functionRef.getClass().getMethod("evaluate", ValueEval[].class, OperationEvaluationContext.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        javaOpenMethod = new JavaOpenMethod(method);
    }

    public static PoiMethodCaller create(FreeRefFunction functionRef) {
        if (functionRef instanceof Function) {
            return create((Function) functionRef, null);
        } else {
            return new PoiMethodCaller(functionRef);
        }
    }

    public static PoiMethodCaller create(Function function, FunctionMetadata metaData) {
        return new PoiMethodCaller(function, metaData);
    }

    @Override
    public IOpenMethod getMethod() {
        return javaOpenMethod;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        List<ValueEval> poiArgs = new ArrayList<ValueEval>();
        fillArgs(params, poiArgs);
        ValueEval[] args = poiArgs.toArray(new ValueEval[poiArgs.size()]);
        ValueEval result;
        if (function != null) {
            result = function.evaluate(args, 0, 0);
        } else {
            // Only simplest case is supported for now
            OperationEvaluationContext ec = new OperationEvaluationContext(null, null, 0, 0, 0, null);
            result = functionRef.evaluate(args, ec);
        }
        return convertToObject(result);
    }

    public boolean isReturnsArray() {
        return returnsArray;
    }

    public boolean isHasArrayParameter() {
        return hasArrayParameter;
    }

    private Object convertToObject(ValueEval result) {
        if (result instanceof NumberEval) {
            return ((NumberEval) result).getNumberValue();
        } else if (result instanceof StringEval) {
            return ((StringEval) result).getStringValue();
        } else if (result instanceof BoolEval) {
            return ((BoolEval) result).getBooleanValue();
        } else if (result instanceof BlankEval) {
            return "";
        } else if (result instanceof ObjectValueEval) {
            return ((ObjectValueEval) result).getValue();
        } else if (result instanceof RefEval) {
            return convertToObject(((RefEval) result).getInnerValueEval(0));
        } else if (result instanceof AreaEval) {
            if (!(result instanceof WrappedAreaEval)) {
                throw new UnsupportedOperationException("Unsupported type of AreaEval");
            }
            return ((WrappedAreaEval) result).getArray();
        } else if (result instanceof ErrorEval) {
            // TODO Choose correct exception
            throw new IllegalArgumentException(((ErrorEval) result).getErrorString());
        } else {
            throw new UnsupportedOperationException("Unsupported type of ValueEval");
        }
    }

    private void fillArgs(Object[] params, List<ValueEval> poiArgs) {
        for (final Object param : params) {
            ValueEval value = getValueEval(param);
            if (value != null) {
                poiArgs.add(value);
            }
        }
    }

    private ValueEval getValueEval(final Object param) {
        ValueEval value;
        if (param == null) {
            value = BlankEval.instance;
        } else if (param instanceof String) {
            try {
                value = new NumberEval(Double.parseDouble((String) param));
            } catch (NumberFormatException e) {
                value = new StringEval((String) param);
            }
        } else if (param instanceof Number) {
            value = new NumberEval(((Number) param).doubleValue());
        } else if (param instanceof Boolean) {
            value = BoolEval.valueOf((Boolean) param);
        } else if (param instanceof Object[][]) {
            value = new WrappedAreaEval((Object[][]) param);
        } else {
            value = new WrappedRefEval(param);
        }
        return value;
    }

    private static class ObjectValueEval implements ValueEval {
        private final Object param;

        public ObjectValueEval(Object param) {
            this.param = param;
        }

        public Object getValue() {
            return param;
        }
    }

    private static class WrappedRefEval extends RefEvalBase {
        private final Object param;

        public WrappedRefEval(Object param) {
            super(0, 0, 0);
            this.param = param;
        }

        @Override
        public ValueEval getInnerValueEval(int sheetIndex) {
            return new ObjectValueEval(param);
        }

        @Override
        public AreaEval offset(int relFirstRowIx,
                int relLastRowIx,
                int relFirstColIx,
                int relLastColIx) {
            throw new UnsupportedOperationException("offset() isn't supported in RefEvalBase");
        }
    }

    private class WrappedAreaEval extends AreaEvalBase {
        private final Object[][] array;

        public WrappedAreaEval(Object[][] array) {
            super(0, 0, array.length - 1, array[0].length - 1);
            this.array = array;
        }

        @Override
        public ValueEval getRelativeValue(int relativeRowIndex, int relativeColumnIndex) {
            return getValueEval(array[relativeRowIndex][relativeColumnIndex]);
        }

        @Override
        public ValueEval getRelativeValue(int sheetIndex,
                int relativeRowIndex,
                int relativeColumnIndex) {
            if (sheetIndex > 0) {
                throw new UnsupportedOperationException("Sheet index > 0 isn't supported in XmlRules");
            }
            return getValueEval(array[relativeRowIndex][relativeColumnIndex]);
        }

        @Override
        public AreaEval offset(int relFirstRowIx,
                int relLastRowIx,
                int relFirstColIx,
                int relLastColIx) {
            Class<?> componentType = array.getClass().getComponentType().getComponentType();
            int height = relLastRowIx - relFirstRowIx + 1;
            int width = relLastColIx - relFirstColIx + 1;

            Object[][] subarray = (Object[][]) Array.newInstance(componentType, height, width);

            for (int i = 0; i < height; i++) {
                System.arraycopy(array[i + relFirstRowIx], relFirstColIx, subarray[i], 0, width);
            }

            return new WrappedAreaEval(subarray);
        }

        @Override
        public TwoDEval getRow(int rowIndex) {
            return offset(rowIndex, rowIndex, 0, array[0].length - 1);
        }

        @Override
        public TwoDEval getColumn(int columnIndex) {
            return offset(0, array.length - 1, columnIndex, columnIndex);
        }

        public Object[][] getArray() {
            return array;
        }
    }
}
