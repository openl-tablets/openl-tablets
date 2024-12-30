package org.openl.conf;

import java.util.HashMap;

import org.openl.binding.INodeBinder;

/**
 * A set of binders which are registered in a hierarchy manner. The hierarchy is defined by a dotted separated string.
 * So if a binder is registered by an 'op.binary' node string, then an 'op.binary.lt' will also be found.
 *
 * @author Yury Molchan
 */
public class NodeBinders {

    private final HashMap<String, INodeBinder> map = new HashMap<>();

    public void put(String node, INodeBinder binder) {
        map.put(node, binder);
    }

    public INodeBinder get(String node) {
        var nodeBinder = map.get(node);
        if (nodeBinder != null) {
            // return an existed binder
            return nodeBinder;
        }
        int lastDot = node.lastIndexOf('.');
        if (lastDot == -1) {
            // no parent node can be extracted, so exit
            return null;
        }
        node = node.substring(0, lastDot); // a parent node
        nodeBinder = get(node); // try to get the parent binder
        if (nodeBinder != null) {
            // store the parent node to speed up the search
            map.put(node, nodeBinder);
        }
        return nodeBinder;
    }
}
