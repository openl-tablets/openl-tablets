package org.openl.engine;

import java.util.List;

import org.openl.IOpenBinder;
import org.openl.IOpenVM;
import org.openl.OpenL;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;

/**
 * Class that defines OpenL engine manager implementation for evaluate/run operations.
 *
 */
public class OpenLRunManager {

    private OpenLCompileManager compileManager;
    private OpenL openl;

    /**
     * Creates new instance of OpenL engine manager.
     *
     * @param openl {@link OpneL} instance
     */
    public OpenLRunManager(OpenL openl) {
        this.openl = openl;
        compileManager = new OpenLCompileManager(openl);
    }

    /**
     * Compiles and runs specified method.
     *
     * @param source source
     * @param methodName method name
     * @param paramTypes parameters types
     * @param params parameters values
     * @return result of method execution
     * @throws OpenLRuntimeException
     * @throws MethodNotFoundException
     * @throws SyntaxNodeException
     */
    public Object runMethod(IOpenSourceCodeModule source,
            final String methodName,
            IOpenClass[] paramTypes,
            Object[] params) throws AmbiguousMethodException {

        IOpenClass openClass = compileManager.compileModule(source, false, null);
        IOpenVM vm = openl.getVm();

        Object target = openClass.newInstance(vm.getRuntimeEnv());

        IOpenMethod method = null;

        if (paramTypes != null) {
            method = openClass.getMethod(methodName, paramTypes);
        } else {
            List<IOpenMethod> list = CollectionUtils.findAll(openClass.getMethods(),
                method11 -> methodName.equals(method11.getName()));
            if (list.size() > 1) {
                throw new AmbiguousMethodException(methodName, IOpenClass.EMPTY, list);
            } else if (list.size() == 1) {
                method = list.get(0);
            }
        }

        if (method == null) {
            throw new MethodNotFoundException(methodName, paramTypes);
        }

        return method.invoke(target, params, vm.getRuntimeEnv());
    }

    /**
     * Compiles source and runs code.
     *
     * @param source source
     * @return result of execution
     * @throws OpenLRuntimeException
     */
    public Object run(IOpenSourceCodeModule source) {

        IParsedCode parsedCode = openl.getParser().parseAsMethodBody(source);
        SyntaxNodeException[] parsingErrors = parsedCode.getErrors();
        if (parsingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Parsing Error:", parsingErrors);
        }
        IOpenBinder binder = openl.getBinder();
        IBoundCode boundCode = binder.bind(parsedCode, null);
        SyntaxNodeException[] bindingErrors = boundCode.getErrors();

        if (bindingErrors.length > 0) {
            throw new CompositeSyntaxNodeException("Binding Error:", bindingErrors);
        }

        IBoundNode boundNode = boundCode.getTopNode();

        IOpenVM vm = openl.getVm();

        return vm.getRunner().run((IBoundMethodNode) boundNode, new Object[0]);
    }

}
