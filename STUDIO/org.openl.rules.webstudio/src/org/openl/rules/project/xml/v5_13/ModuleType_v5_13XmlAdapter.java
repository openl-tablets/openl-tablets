package org.openl.rules.project.xml.v5_13;

import org.openl.rules.project.model.v5_13.ModuleType_v5_13;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ModuleType_v5_13XmlAdapter extends XmlAdapter<String, ModuleType_v5_13> {
    @Override
    public ModuleType_v5_13 unmarshal(String name) {
        return ModuleType_v5_13.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(ModuleType_v5_13 moduleType_v5_13) {
        return moduleType_v5_13.toString().toLowerCase();
    }
}
