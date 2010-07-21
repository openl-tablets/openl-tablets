package org.openl.engine;

import org.openl.IOpenVM;
import org.openl.OpenL;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.SourceType;
import org.openl.syntax.code.ProcessedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Class that defines OpenL engine manager implementation for evaluate/run
 * operations.
 * 
 */
public class OpenLRunManager extends OpenLHolder {

    private OpenLSourceManager sourceManager;
    private OpenLCompileManager compileManager;

    /**
     * Creates new instance of OpenL engine manager.
     * 
     * @param openl {@link OpneL} instance
     */
    public OpenLRunManager(OpenL openl) {
        super(openl);
        sourceManager = new OpenLSourceManager(openl);
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
    public Object runMethod(IOpenSourceCodeModule source, String methodName, IOpenClass[] paramTypes, Object[] params)
            throws OpenLRuntimeException, MethodNotFoundException, SyntaxNodeException {

        IOpenClass openClass = compileManager.compileModule(source);
        IOpenVM vm = getOpenL().getVm();

        Object target = openClass.newInstance(vm.getRuntimeEnv());

        IOpenMethod method = OpenLUtils.getMethod(methodName, paramTypes, openClass);

        return method.invoke(target, params, vm.getRuntimeEnv());
    }

    /**
     * Compiles and runs OpenL script.
     * 
     * @param source source
     * @return result of script execution
     * @throws OpenLRuntimeException
     */
    public Object runScript(IOpenSourceCodeModule source) throws OpenLRuntimeException {

        return run(source, SourceType.METHOD_BODY);
    }

    /**
     * Compiles source and runs code.
     * 
     * @param source source
     * @param sourceType type of source
     * @return result of execution
     * @throws OpenLRuntimeException
     */
    public Object run(IOpenSourceCodeModule source, SourceType sourceType) throws OpenLRuntimeException {

        ProcessedCode processedCode = sourceManager.processSource(source, sourceType);

        IBoundCode boundCode = processedCode.getBoundCode();
        IBoundNode boundNode = boundCode.getTopNode();

        IOpenVM vm = getOpenL().getVm();

        if (boundNode instanceof IBoundMethodNode) {
            return vm.getRunner().run((IBoundMethodNode) boundNode, new Object[0]);
        }

        if (boundNode instanceof LiteralBoundNode) {
            return ((LiteralBoundNode) boundNode).getValue();
        }

        try {
            throw new Exception("Unrunnable Bound Node Type:" + boundNode.getClass().getName());
        } catch (Exception ex) {
            throw new OpenLRuntimeException(ex, boundNode);
        }
    }

}