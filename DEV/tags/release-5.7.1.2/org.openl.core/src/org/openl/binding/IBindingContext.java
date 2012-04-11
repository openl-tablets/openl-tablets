/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.util.List;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public interface IBindingContext extends ICastFactory {

    void addAlias(String name, String value);

    void addError(SyntaxNodeException error);

    // public void addAllErrors(Vector errors);

    ILocalVar addParameter(String namespace, String name, IOpenClass type) throws DuplicatedVarException;

    void addType(String namespace, IOpenClass type) throws Exception;

    ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException;

    INodeBinder findBinder(ISyntaxNode node);

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
    IOpenField findFieldFor(IOpenClass type, String fieldName, boolean strictMatch);

    IMethodCaller findMethodCaller(String namespace, String name, IOpenClass[] parTypes)
            throws AmbiguousMethodException;

    IOpenClass findType(String namespace, String typeName);

    /**
     *
     * @param namespace
     * @param name
     * @param strictMatch
     * @return
     * @throws AmbiguousVarException
     * @see {@link IOpenClass#getField(String, boolean)}
     */
    IOpenField findVar(String namespace, String vname, boolean strictMatch) throws AmbiguousVarException;

    String getAlias(String name);

    IOpenCast getCast(IOpenClass from, IOpenClass to);

    SyntaxNodeException[] getErrors();

    int getLocalVarFrameSize();

    int getNumberOfErrors();

    OpenL getOpenL();

    int getParamFrameSize();

    IOpenClass getReturnType();

    List<SyntaxNodeException> popErrors();

    void popLocalVarContext();

    /**
     * Used for doing temporary processing within current context
     */
    void pushErrors();

    void pushLocalVarContext();

    /**
     * @param type
     */
    void setReturnType(IOpenClass type);

}
