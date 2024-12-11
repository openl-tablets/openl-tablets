package org.openl.rules.project.xml.v5_11;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.openl.rules.project.model.v5_11.ModuleType_v5_11;

public class ModuleTypeXmlAdapter_v5_11 extends XmlAdapter<String, ModuleType_v5_11> {
    @Override
    public ModuleType_v5_11 unmarshal(String name) {
        return ModuleType_v5_11.valueOf(name.toUpperCase());
    }

    @Override
    public String marshal(ModuleType_v5_11 moduleType_v5_11) {
        return moduleType_v5_11.toString().toLowerCase();
    }
}
