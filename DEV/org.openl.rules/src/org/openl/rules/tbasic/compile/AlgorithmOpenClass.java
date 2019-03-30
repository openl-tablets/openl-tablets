package org.openl.rules.tbasic.compile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenField;

public class AlgorithmOpenClass extends ComponentOpenClass {

    private Set<String> invisibleFields = new HashSet<String>();

    public AlgorithmOpenClass(String name, OpenL openl) {
        super(name, openl);
    }

    @Override
    public IOpenField getField(String fname) {
        if (!invisibleFields.contains(fname)) {
            return super.getField(fname);
        }
        return null;
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) {
        if (!invisibleFields.contains(fname)) {
            return super.getField(fname, strictMatch);
        }
        return null;
    }

    @Override
    public Map<String, IOpenField> getFields() {
        return filterFields(super.getFields());
    }

    @Override
    public void addField(IOpenField field) {
        try {
            super.addField(field);
        } catch (DuplicatedFieldException e) {
            throw new DuplicatedVarException("", e.getFieldName());
        }
    }

    private Map<String, IOpenField> filterFields(Map<String, IOpenField> fields) {
        Map<String, IOpenField> visibleFields = new HashMap<>();
        for (Entry<String, IOpenField> entry : fields.entrySet()) {
            if (!invisibleFields.contains(entry.getKey())) {
                visibleFields.put(entry.getKey(), entry.getValue());
            }
        }
        return visibleFields;
    }

    @Override
    public Map<String, IOpenField> getDeclaredFields() {
        return filterFields(super.getDeclaredFields());
    }

    public void setFieldToInvisibleState(String fname) {
        invisibleFields.add(fname);
    }

    public void setFieldToVisibleState(String fname) {
        invisibleFields.remove(fname);
    }

    public void allFieldsToVisible() {
        invisibleFields.clear();
    }

}
