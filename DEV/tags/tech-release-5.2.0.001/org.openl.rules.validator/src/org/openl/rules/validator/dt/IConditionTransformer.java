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
public interface IConditionTransformer
{

	/**
	 * @param class1
	 * @param parameterName
	 * @param parameterDirection
	 * @return
	 */
	IOpenClass transformSignatureType(IParameterDeclaration pd);

	/**
	 * @param declaration
	 * @return
	 */
	IOpenClass transformParameterType(IParameterDeclaration declaration);

	/**
	 * @param parameterName
	 * @param class1
	 * @param c
	 * @return
	 */
	IntVar makeSignatureVar(String parameterName, IOpenClass class1, Constrainer c);

	/**
	 * @param name
	 * @param condition
	 * @param value
	 * @param dtan 
	 * @return
	 */
	Object transformParameterValue(String name, IDTCondition condition, Object value, Constrainer C, DTAnalyzer dtan);

	Object transformSignatureValueBack(String name, int intValue, DTAnalyzer dtan);

	
}
