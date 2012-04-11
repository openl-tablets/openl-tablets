/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.rules.dt.IDTCondition;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntVar;

/**
 * @author snshor
 *
 */
public interface IConditionTransformer {
    
    IntVar makeSignatureVar(String parameterName, IOpenClass class1, Constrainer c);

    IOpenClass transformParameterType(IParameterDeclaration declaration);

    Object transformParameterValue(String name, IDTCondition condition, Object value, Constrainer C, DTAnalyzer dtan);

    IOpenClass transformSignatureType(IParameterDeclaration pd);

    Object transformSignatureValueBack(String name, int intValue, DTAnalyzer dtan);

}
