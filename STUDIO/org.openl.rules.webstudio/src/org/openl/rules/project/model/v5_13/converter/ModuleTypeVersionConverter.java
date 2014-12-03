package org.openl.rules.project.model.v5_13.converter;

import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.v5_13.ModuleType_v5_13;

public class ModuleTypeVersionConverter implements ObjectVersionConverter<ModuleType, ModuleType_v5_13> {
    @Override
    public ModuleType fromOldVersion(ModuleType_v5_13 oldVersion) {
        if (oldVersion == null) {
            return null;
        }

        switch (oldVersion) {
            case WRAPPER:
                return ModuleType.WRAPPER;
            case API:
                return ModuleType.API;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ModuleType_v5_13 toOldVersion(ModuleType currentVersion) {
        if (currentVersion == null) {
            return null;
        }

        switch (currentVersion) {
            case WRAPPER:
                return ModuleType_v5_13.WRAPPER;
            case API:
                return ModuleType_v5_13.API;
            default:
                throw new IllegalArgumentException();
        }
    }
}
