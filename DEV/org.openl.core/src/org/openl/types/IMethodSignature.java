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

    final class VoidSignature implements IMethodSignature {
        public int getNumberOfParameters() {
            return 0;
        }

        public String getParameterName(int i) {
            throw new IndexOutOfBoundsException();
        }
        
        public IOpenClass getParameterType(int i) {
            throw new IndexOutOfBoundsException();
        }

        public IOpenClass[] getParameterTypes() {
            return IOpenClass.EMPTY;
        }

    }

    IMethodSignature VOID = new VoidSignature();

    int getNumberOfParameters();

    String getParameterName(int i);
    
    IOpenClass getParameterType(int i);

    IOpenClass[] getParameterTypes();

}
