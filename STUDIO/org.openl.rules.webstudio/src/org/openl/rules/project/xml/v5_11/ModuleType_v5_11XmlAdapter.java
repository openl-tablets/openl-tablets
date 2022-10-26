package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.ModuleType_v5_11;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ModuleType_v5_11XmlAdapter extends XmlAdapter<String, ModuleType_v5_11> {
    @Override
    public ModuleType_v5_11 unmarshal(String name) {
        return ModuleType_v5_11.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(ModuleType_v5_11 moduleType_v5_11) {
        return moduleType_v5_11.toString().toLowerCase();
    }
}
