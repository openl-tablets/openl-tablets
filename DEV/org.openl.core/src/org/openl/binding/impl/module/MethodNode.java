/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.ABoundNode;
import org.openl.binding.impl.ANodeBinder;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodNode extends ABoundNode implements IBoundMethodNode, IMemberBoundNode {

    int localFrameSize, parametersSize;

    DeferredMethod deferredMethod;

    // MethodParametersNode boundParametersNode;

    /**
     * @param syntaxNode
     * @param elements
     */
    public MethodNode(ISyntaxNode syntaxNode,
    // IBoundNode[] children,// int localFrameSize, int parametersSize,
            DeferredMethod deferredMethod) {
        super(syntaxNode, new IBoundNode[0]);
        // this.localFrameSize = localFrameSize;
        // this.parametersSize = parametersSize;
        this.deferredMethod = deferredMethod;
        // this.boundParametersNode = boundParametersNode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.impl.module.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
     */
    public void addTo(ModuleOpenClass openClass) {
        openClass.addMethod(deferredMethod);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
     */
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        try {
            return children[0].evaluate(env);
        } catch (ControlSignalReturn signal) {
            return signal.getReturnValue();
        } catch (OpenLRuntimeException opex) {
            opex.pushMethodNode(this);
            throw opex;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.impl.module.IMemberBoundNode#finalizeBind(org.openl.binding.IBindingContext)
     */
    public void finalizeBind(IBindingContext cxt) throws Exception {
        MethodBindingContext mbc = new MethodBindingContext(deferredMethod, cxt);

        // mbc.setReturnType(deferredMethod.getType());

        ISyntaxNode bodyNode = deferredMethod.getMethodBodyNode();

        IBoundNode boundBodyNode = ANodeBinder.bindChildNode(bodyNode, mbc);
        deferredMethod.setMethodBodyBoundNode((IBoundMethodNode) boundBodyNode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundMethodNode#getLocalFrameSize()
     */
    public int getLocalFrameSize() {
        return localFrameSize;
    }

    public String getName() {
        return deferredMethod.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundMethodNode#getParametersSize()
     */
    public int getParametersSize() {
        return parametersSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    public IOpenClass getType() {
        return deferredMethod.getType();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        // TODO Auto-generated method stub

    }

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        //nothing to remove
    }

}
