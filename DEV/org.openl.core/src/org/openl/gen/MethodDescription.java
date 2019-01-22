package org.openl.gen;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Vladyslav Pikus
 */
public class MethodDescription {

    private final String name;
    private final TypeDescription returnType;
    private final TypeDescription argsTypes[];

    public MethodDescription(String name, Class<?> returnType, Class<?>[] argsTypes) {
        this.name = name;
        this.returnType = new TypeDescription(returnType.getName());
        this.argsTypes = new TypeDescription[argsTypes.length];
        for (int i = 0; i < argsTypes.length; i++) {
            this.argsTypes[i] = new TypeDescription(argsTypes[i].getName());
        }
    }

    public TypeDescription getReturnType() {
        return returnType;
    }

    public TypeDescription[] getArgsTypes() {
        return argsTypes;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDescription that = (MethodDescription) o;
        return Objects.equals(name, that.name) && Arrays.equals(argsTypes, that.argsTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(argsTypes);
        return result;
    }
}
