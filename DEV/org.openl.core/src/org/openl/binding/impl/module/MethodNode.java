package org.openl.binding.impl.module;

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

    public MethodNode(ISyntaxNode syntaxNode, DeferredMethod deferredMethod) {
        super(syntaxNode);
        this.deferredMethod = deferredMethod;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.impl.module.IMemberBoundNode#addTo(org.openl.binding.impl.module.ModuleOpenClass)
     */
    @Override
    public void addTo(ModuleOpenClass openClass) {
        openClass.addMethod(deferredMethod);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
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
    @Override
    public void finalizeBind(IBindingContext cxt) throws Exception {
        MethodBindingContext mbc = new MethodBindingContext(deferredMethod, cxt);

        ISyntaxNode bodyNode = deferredMethod.getMethodBodyNode();

        IBoundNode boundBodyNode = ANodeBinder.bindChildNode(bodyNode, mbc);
        deferredMethod.setMethodBodyBoundNode((IBoundMethodNode) boundBodyNode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundMethodNode#getLocalFrameSize()
     */
    @Override
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
    @Override
    public int getParametersSize() {
        return parametersSize;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBoundNode#getType()
     */
    @Override
    public IOpenClass getType() {
        return deferredMethod.getType();
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        // nothing to remove
    }

}
