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
	IOpenClass transformSignatureType(IOpenClass class1, String parameterName, int parameterDirection);

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
	 * @return
	 */
	Object transformParameterValue(String name, IDTCondition condition, Object value, Constrainer C);

	Object transformSignatureValueBack(String name, int i);

	
}
