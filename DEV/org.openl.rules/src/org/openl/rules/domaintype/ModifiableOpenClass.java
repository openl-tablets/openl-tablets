package org.openl.rules.domaintype;

import java.util.HashMap;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenClassDelegator;

public class ModifiableOpenClass extends OpenClassDelegator {

    HashMap<String, IOpenField> modifiedFields = new HashMap<String, IOpenField>();

    public ModifiableOpenClass(IOpenClass baseClass, String name) {
        super(name, baseClass, baseClass.getMetaInfo());
    }

    public void addField(IOpenField f) {
        modifiedFields.put(f.getName(), f);
    }

    @Override
    public IOpenField getField(String name, boolean strictMatch) {
        // TODO this does not take strictMatch correctly
        IOpenField f = modifiedFields.get(name);
        return f != null ? f : super.getField(name, strictMatch);
    }

    @Override
    public IOpenField getIndexField() {
        // TODO Auto-generated method stub
        return super.getIndexField();
    }

}
