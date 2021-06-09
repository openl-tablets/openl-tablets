package org.openl.types;

public class NullParameterDeclaration implements IParameterDeclaration {

    public static final NullParameterDeclaration the = new NullParameterDeclaration();

    public static boolean isAnyNull(IParameterDeclaration... params) {
        for (IParameterDeclaration param : params) {
            if (param == the) {
                return true;
            }
        }
        return false;
    }

    private NullParameterDeclaration() {
    }

    @Override
    public String getDisplayName(int mode) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public IOpenClass getType() {
        return NullOpenClass.the;
    }

    @Override
    public String toString() {
        return "null-Class - null-Name";
    }
}
