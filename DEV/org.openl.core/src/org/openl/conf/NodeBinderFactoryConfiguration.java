/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.CategorizedMap;

/**
 * @author snshor
 *
 */
public class NodeBinderFactoryConfiguration extends AConfigurationElement {

    static public class SingleBinderFactory extends ClassFactory {
        String node;

        public SingleBinderFactory() {
            singleton = true;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.newconf.ClassFactory#getExtendsClassName()
         */
        @Override
        public String getExtendsClassName() {
            return INodeBinder.class.getName();
        }

        /**
         * @return
         */
        public String getNode() {
            return node;
        }

        /**
         * @param string
         */
        public void setNode(String string) {
            node = string;
        }

    }

    CategorizedMap map = new CategorizedMap();

    public void addConfiguredBinder(SingleBinderFactory factory) {
        map.put(factory.getNode(), factory);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinderFactory#getNodeBinder(org.openl.syntax.ISyntaxNode)
     */
    public INodeBinder getNodeBinder(ISyntaxNode node, IConfigurableResourceContext cxt) {
        SingleBinderFactory factory = (SingleBinderFactory) map.get(node.getType());

        return factory == null ? null : (INodeBinder) factory.getResource(cxt);
    }

    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (Object factory : map.values()) {
            ((SingleBinderFactory) factory).validate(cxt);
        }
    }

}
