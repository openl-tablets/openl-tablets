package org.openl.binding.impl.module;

import java.util.Objects;

import lombok.Getter;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenFieldDelegator;

// A delegating field equals the field it wraps; the module-specific type only narrows the type it reports.
@SuppressWarnings("java:S2160")
public final class ModuleSpecificOpenField extends OpenFieldDelegator {
    @Getter
    private final IOpenClass type;

    public ModuleSpecificOpenField(IOpenField field, IOpenClass type) {
        super(field);
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }
}
