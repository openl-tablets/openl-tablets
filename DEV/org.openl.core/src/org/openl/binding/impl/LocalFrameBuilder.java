/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.LinkedList;

import org.openl.binding.ILocalVar;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class LocalFrameBuilder {
    static class LocalVar implements ILocalVar {
        String namespace;
        String name;
        int indexInLocalFrame;
        IOpenClass type;

        LocalVar(String namespace, String name, int indexInLocalFrame, IOpenClass type) {
            this.namespace = namespace;
            this.name = name;
            this.indexInLocalFrame = indexInLocalFrame;
            this.type = type;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenField#get(java.lang.Object)
         */
        @Override
        public Object get(Object target, IRuntimeEnv env) {
            Object res = env.getLocalFrame()[indexInLocalFrame];

            return res != null ? res : getType().nullObject();

        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenMember#getDeclaringClass()
         */
        @Override
        public IOpenClass getDeclaringClass() {
            return NullOpenClass.the;
        }

        @Override
        public String getDisplayName(int mode) {
            return name;
        }

        /**
         * @return
         */
        @Override
        public int getIndexInLocalFrame() {
            return indexInLocalFrame;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenMember#getInfo()
         */
        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.base.INamedThing#getName()
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * @return
         */
        @Override
        public String getNamespace() {
            return namespace;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenMember#getType()
         */
        @Override
        public IOpenClass getType() {
            return type;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenField#isConst()
         */
        @Override
        public boolean isConst() {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenField#isReadable()
         */
        @Override
        public boolean isReadable() {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenMember#isStatic()
         */
        @Override
        public boolean isStatic() {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenField#isWritable()
         */
        @Override
        public boolean isWritable() {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.types.IOpenField#set(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            env.getLocalFrame()[indexInLocalFrame] = value;
        }

        @Override
        public String toString() {
            return "~" + name;
        }

    }

    static public class LocalVarFrameElement extends ArrayList<ILocalVar> {
    }

    LinkedList<LocalVarFrameElement> localFrames = new LinkedList<>();

    int localVarFrameSize = 0;

    /**
     *
     */
    public LocalFrameBuilder() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#addVar(java.lang.String,
     *      java.lang.String)
     */
    public ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException {
        ILocalVar var = findLocalVar(namespace, name);
        if (var != null) {
            throw new DuplicatedVarException(null, name);
        }

        var = new LocalVar(namespace, name, currentFrameSize(), type);
        localFrames.peek().add(var);
        return var;
    }

    public int currentFrameSize() {
        int sum = 0;
        for (LocalVarFrameElement element : localFrames) {
            sum += element.size();
        }
        return sum;
    }

    public ILocalVar findLocalVar(String namespace, String varname) {
        for (LocalVarFrameElement frame : localFrames) {
            for (ILocalVar var : frame) {
                String s1 = var.getNamespace();
                if (var.getName().equals(varname) && (s1 != null ? s1.equals(namespace) : namespace == null)) {
                    return var;
                }
            }
        }

        return null;

    }

    public int getLocalVarFrameSize() {
        return localVarFrameSize;
    }

    public LocalVarFrameElement getTopFrame() {
        return localFrames.peek();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#popLocalVarcontext()
     */
    public void popLocalVarcontext() {
        localVarFrameSize = Math.max(localVarFrameSize, currentFrameSize());
        localFrames.pop();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IBindingContext#pushLocalVarContext(org.openl.binding.ILocalVarContext)
     */
    public void pushLocalVarContext() {
        localFrames.push(new LocalVarFrameElement());
    }

}
