package org.openl.rules.project.instantiation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.model.Module;

public class RulesProjectHelper {

    public static Map<String, Module> makeModulesMap(List<Module> modules) {
        
        Map<String, Module> map = new HashMap<String, Module>();
        
        for (Module module : modules) {
            map.put(module.getName(), module);
        }
        
        return map;
    }
}
