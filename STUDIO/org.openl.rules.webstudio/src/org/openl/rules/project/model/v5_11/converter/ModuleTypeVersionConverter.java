package org.openl.rules.project.model.v5_11.converter;

import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.v5_11.ModuleType_v5_11;

public class ModuleTypeVersionConverter implements ObjectVersionConverter<ModuleType, ModuleType_v5_11> {
    @Override
    public ModuleType fromOldVersion(ModuleType_v5_11 oldVersion) {
        if (oldVersion == null) {
            return null;
        }

        switch (oldVersion) {
            case STATIC:
                return ModuleType.WRAPPER;
            case DYNAMIC:
            case API:
                return ModuleType.API;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ModuleType_v5_11 toOldVersion(ModuleType currentVersion) {
        if (currentVersion == null) {
            return null;
        }

        switch (currentVersion) {
            case WRAPPER:
                return ModuleType_v5_11.STATIC;
            case API:
                return ModuleType_v5_11.API;
            default:
                throw new IllegalArgumentException();
        }
    }
}
