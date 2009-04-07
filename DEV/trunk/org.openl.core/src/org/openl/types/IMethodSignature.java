/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 *
 */
public interface IMethodSignature {

    static final class VoidSignature implements IMethodSignature {
        public int getNumberOfArguments() {
            return 0;
        }

        public int getParameterDirection(int i) {
            throw new IndexOutOfBoundsException();
        }

        public String getParameterName(int i) {
            throw new IndexOutOfBoundsException();
        }

        public IOpenClass[] getParameterTypes() {
            return IOpenClass.EMPTY;
        }

    }

    public static final IMethodSignature VOID = new VoidSignature();

    public int getNumberOfArguments();

    public int getParameterDirection(int i);

    public String getParameterName(int i);

    public IOpenClass[] getParameterTypes();

}
