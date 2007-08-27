/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.util.Vector;

import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public interface IBindingContext extends ICastFactory
{

  public void addError(ISyntaxError error);

  public void addAllErrors(Vector errors);
  
  
  /**
   * Used for doing temporary processing within current context
   */
  void pushErrors();
  
  Vector popErrors();

  public IMethodCaller findMethodCaller(
    String namespace,
    String name,
    IOpenClass[] parTypes)
    throws AmbiguousMethodException;

  public IOpenField findVar(String namespace, String name)
    throws AmbiguousVarException;

  public ILocalVar addVar(String namespace, String name, IOpenClass type)
    throws DuplicatedVarException;
  public ILocalVar addParameter(String namespace, String name, IOpenClass type)
    throws DuplicatedVarException;

  public IOpenClass findType(String namespace, String typeName);
  
  public void addType(String namespace, IOpenClass type) throws Exception;
  

  public int getParamFrameSize();
  public int getLocalVarFrameSize();

  public INodeBinder findBinder(ISyntaxNode node);

  public ISyntaxError[] getError();
  public int getNumberOfErrors();
  
  

  public void pushLocalVarContext();

  public void popLocalVarContext();

  public IOpenCast getCast(IOpenClass from, IOpenClass to);
  
  public IOpenClass getReturnType();
  
  public void addAlias(String name, String value);
  
  public String getAlias(String name);

	/**
	 * @param type
	 */
	public void setReturnType(IOpenClass type);

}
