package org.openl.types;

/**
 * The signature of a method without parameters.
 *
 * @see IMethodSignature#VOID
 */
final class VoidSignature implements IMethodSignature {
    @Override
    public int getNumberOfParameters() {
        return 0;
    }

    @Override
    public String getParameterName(int i) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public IOpenClass getParameterType(int i) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return IOpenClass.EMPTY;
    }

}
