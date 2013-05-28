package org.openl.rules.testmethod.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

/**
 * Similar to {@link BeanResultComparator} but uses OpenL core API.
 * 
 * @author PUdalau
 * 
 */
public class OpenLBeanResultComparator extends BeanResultComparator {
    private Map<String, IOpenField> fieldMap;

    public OpenLBeanResultComparator(List<IOpenField> fields) {
        super(new ArrayList<String>(makeFieldMap(fields).keySet()));
        fieldMap = makeFieldMap(fields);
    }

    private static Map<String, IOpenField> makeFieldMap(List<IOpenField> fields) {
        Map<String, IOpenField> fieldMap = new HashMap<String, IOpenField>();
        for (IOpenField field : fields) {
            fieldMap.put(field.getName(), field);
        }
        return fieldMap;
    }

    @Override
    protected Object getFieldValue(Object target, String fieldName) {
        IOpenField field = fieldMap.get(fieldName);
        IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
        return field.get(target, env);
    }

    public Collection<IOpenField> getFields() {
        return fieldMap.values();
    }

    public IOpenField getField(String name) {
        return fieldMap.get(name);
    }
}
