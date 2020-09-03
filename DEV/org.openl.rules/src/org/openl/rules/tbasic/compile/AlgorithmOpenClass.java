package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenField;

public class AlgorithmOpenClass extends ComponentOpenClass {

    private Set<String> invisibleFields = new HashSet<>();

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
    public Collection<IOpenField> getFields() {
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

    private Collection<IOpenField> filterFields(Collection<IOpenField> fields) {
        Collection<IOpenField> visibleFields = new ArrayList<>();
        for (IOpenField field : fields) {
            if (!invisibleFields.contains(field.getName())) {
                visibleFields.add(field);
            }
        }
        return visibleFields;
    }

    @Override
    public Collection<IOpenField> getDeclaredFields() {
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
