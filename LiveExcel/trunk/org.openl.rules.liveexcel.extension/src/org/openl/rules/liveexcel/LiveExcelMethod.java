package org.openl.rules.liveexcel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

import com.exigen.le.LE_Value;
import com.exigen.le.LE_Value.Type;
import com.exigen.le.LiveExcel;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;

public class LiveExcelMethod extends AMethod {

    private String functionName;

    private LiveExcel liveExcel;

    public LiveExcelMethod(IOpenMethodHeader header, String functionName, LiveExcel liveExcel) {
        super(header);
        this.functionName = functionName;
        this.liveExcel = liveExcel;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        Map<String, String> envProps = new HashMap<String, String>();
        if (env.getContext() != null && ((IRulesRuntimeContext) env.getContext()).getCurrentDate() != null) {
            envProps.put(Function.EFFECTIVE_DATE, new SimpleDateFormat(com.exigen.le.smodel.Type.DATE_FORMAT)
                    .format(((IRulesRuntimeContext) env.getContext()).getCurrentDate()));
        }
        List<Object> args = new ArrayList<Object>(params.length);
        for (int i = 0; i < params.length; i++) {
            if (getSignature().getParameterType(i) instanceof JavaOpenClass
                    || getSignature().getParameterType(i) instanceof DatatypeOpenClass) {
                args.add(new BeanWrapper(params[i], liveExcel.getServiceModel().getType(
                        getSignature().getParameterName(i))));
            } else {
                args.add(params[i]);
            }
        }
        LE_Value result = liveExcel.calculate(functionName, args, envProps);
        return getValueByLEValue(result);
    }

    private Object getValueByLEValue(LE_Value value) {
        switch (value.getType()) {
            case Type.BOOLEAN:
                return Boolean.valueOf(value.getValue());
            case Type.DATE:
                return value.getDateValue();
            case Type.NUMERIC:
                return Double.valueOf(value.getValue());
            case Type.ERROR:
                return null;
            case Type.VALUE_HOLDER:
                // TODO
                if (value.getValueHolder() instanceof BeanWrapper) {
                    return ((BeanWrapper) value.getValueHolder()).getHolder();
                }
                return null;
            case Type.ARRAY:
                LE_Value[][] array = value.getArray();
                Object[][] result = new Object[array.length][array[0].length];
                for (int i = 0; i < array.length; i++) {
                    for (int j = 0; j < array[0].length; j++) {
                        result[i][j] = getValueByLEValue(array[i][j]);
                    }
                }
                return result;
            case Type.STRING:
            default:
                return value.getValue();
        }
    }

}
