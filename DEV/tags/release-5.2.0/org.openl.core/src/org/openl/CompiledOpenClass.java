/**
 * Created Jan 11, 2007
 */
package org.openl;

import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class CompiledOpenClass
{

	ISyntaxError[] parsingErrors;

	ISyntaxError[] bindingErrors;

	IOpenClass openClass;


	public void throwErrorExceptionsIfAny()
	{
		if (parsingErrors.length > 0)
		{
			throw new SyntaxErrorException("Parsing Error(s):", parsingErrors);
		}

		if (bindingErrors.length > 0)
		{
			throw new SyntaxErrorException("Binding Error(s):", bindingErrors);
		}
		
	}
	
	public IOpenClass getOpenClass()
	{
		throwErrorExceptionsIfAny();
		return openClass;
	}

	public CompiledOpenClass(IOpenClass openClass, ISyntaxError[] parsingErrors, 
			ISyntaxError[] bindingErrors)
	{
		this.openClass = openClass;
		this.parsingErrors = parsingErrors;
		this.bindingErrors = bindingErrors;
	}


	
	
	public boolean hasErrors()
	{
		return (parsingErrors.length > 0) ||  (bindingErrors.length > 0);
	}

	public ISyntaxError[] getBindingErrors()
	{
		return this.bindingErrors;
	}

	public ISyntaxError[] getParsingErrors()
	{
		return this.parsingErrors;
	}

	/**
	 * @return
	 */
	public IOpenClass getOpenClassWithErrors()
	{
		return openClass;
	}
	
}
