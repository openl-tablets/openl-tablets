package org.openl.rules.model.scaffolding;

import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ParameterModel implements InputParameter {

    @Getter
    private final TypeInfo type;
    @Getter
    private final String formattedName;
    @Getter
    private final String originalName;
    @Getter
    @Setter
    private In in;

    public ParameterModel(TypeInfo type, String formattedName) {
        this.type = type;
        this.formattedName = formattedName;
        this.originalName = formattedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParameterModel that)) {
            return false;
        }
        return Objects.equals(type, that.type) && Objects.equals(formattedName, that.formattedName) && Objects
                .equals(originalName, that.originalName) && in == that.in;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, formattedName, in, originalName);
    }
}
