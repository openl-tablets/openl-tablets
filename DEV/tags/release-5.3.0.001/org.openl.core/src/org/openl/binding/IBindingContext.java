/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.util.List;

import org.openl.OpenL;
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
public interface IBindingContext extends ICastFactory {

    public void addAlias(String name, String value);

    public void addError(ISyntaxError error);

    // public void addAllErrors(Vector errors);

    public ILocalVar addParameter(String namespace, String name, IOpenClass type) throws DuplicatedVarException;

    public void addType(String namespace, IOpenClass type) throws Exception;

    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException;

    public INodeBinder findBinder(ISyntaxNode node);

    /**
     * This method is implemented by default by calling type.getFiled(fieldName,
     * strictMatch), but some context may override it to provide dynamic mapping
     * functionality
     *
     * @param type
     * @param fieldName
     * @param strictMatch
     * @return
     */

    public IOpenField findFieldFor(IOpenClass type, String fieldName, boolean strictMatch);

    public IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes)
            throws AmbiguousMethodException;

    public IOpenClass findType(String namespace, String typeName);

    /**
     *
     * @param namespace
     * @param name
     * @param strictMatch
     * @return
     * @throws AmbiguousVarException
     * @see {@link IOpenClass#getField(String, boolean)}
     */

    public IOpenField findVar(String namespace, String vname, boolean strictMatch) throws AmbiguousVarException;

    public String getAlias(String name);

    public IOpenCast getCast(IOpenClass from, IOpenClass to);

    public ISyntaxError[] getError();

    public int getLocalVarFrameSize();

    public int getNumberOfErrors();

    OpenL getOpenL();

    public int getParamFrameSize();

    public IOpenClass getReturnType();

    List<ISyntaxError> popErrors();

    public void popLocalVarContext();

    /**
     * Used for doing temporary processing within current context
     */
    void pushErrors();

    public void pushLocalVarContext();

    /**
     * @param type
     */
    public void setReturnType(IOpenClass type);

}
