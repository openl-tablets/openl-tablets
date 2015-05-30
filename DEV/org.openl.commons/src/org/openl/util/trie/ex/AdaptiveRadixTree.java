package org.openl.util.trie.ex;


/**
 * 
 * @author snshor
 * 
 *         Based on http://www-db.in.tum.de/~leis/papers/ART.pdf
 * 
 */

public class AdaptiveRadixTree {

	public static final int MAX_BYTE_NODE_SIZE = 256;

	private boolean usePrefix = false;
	private int[] maxNodeSizes; // per depth

	public AdaptiveRadixTree(boolean usePrefix, int[] maxNodeSizes) {
		super();
		this.usePrefix = usePrefix;
		this.maxNodeSizes = maxNodeSizes;
	}

	public int getMaxNodeSize(int depth) {
		return maxNodeSizes == null || maxNodeSizes.length <= depth ? MAX_BYTE_NODE_SIZE
				: maxNodeSizes[depth];
	}

	public ARTNodeOld search(ARTNodeOld node, byte[] key, int depth) {
		if (node == null)
			return null;
		if (isLeaf(node)) {
			if (leafMatches(node, key, depth))
				return node;
			return null;
		}
		if (usePrefix) {
			if (checkPrefix(node, key, depth) != node.prefixLen())
				return null;
			depth = depth + node.prefixLen();
		}

		ARTNodeOld next = node.findChild(key[depth]);
		return search(next, key, depth + 1);
	}

	private int checkPrefix(ARTNodeOld node, byte[] key, int depth) {
		if (!usePrefix)
			return 0;
		throw new UnsupportedOperationException("Use Prefix");
	}

	private boolean leafMatches(ARTNodeOld node, byte[] key, int depth) {
		// TODO Auto-generated method stub
		return false;
	}

	ARTNodeOld insert(ARTNodeOld node, byte[] key, int depth, int value) {
		// if (node == null) // handle empty tree
		// {
		// replace(node, leaf);
		// return;
		// }
		// if (isLeaf(node)) // expand node
		// {
		// Node newNode=makeNode4();
		// byte[] key2=loadKey(node);
		// int i = depth;
		// for (; key[i]==key2[i]; ++i)
		// {
		// newNode.prefix[i-depth]=key[i];
		// }
		// newNode.prefixLen=i-depth;
		// depth=depth+newNode.prefixLen;
		// addChild(newNode, key[depth], leaf);
		// addChild(newNode, key2[depth], node);
		// replace(node, newNode);
		// return;
		// }
		// int p = checkPrefix(node, key, depth);
		// if (p!=node.prefixLen) // prefix mismatch
		// {
		// Node newNode=makeNode4();
		// addChild(newNode, key[depth+p], leaf);
		// addChild(newNode, node.prefix[p], node);
		// newNode.prefixLen=p;
		// memcpy(newNode.prefix, node.prefix, p);
		// node.prefixLen=node.prefixLen-(p+1);
		// memmove(node.prefix, node.prefix + p + 1 ,node.prefixLen);
		// replace(node, newNode);
		// return;
		// }
		// depth=depth+node.prefixLen;
		byte byteKey = key[depth];
		ARTNodeOld next = node.findChild(byteKey);
		if (next == null) { // recurse
			next = createNewNode(key, depth + 1, value);
			if (node.isFull())
				node = grow(node);
			node.addChild(next, byteKey, value);
		}
		ARTNodeOld newNode = insert(next, key, depth + 1, value);
		if (newNode != next)
			node.replace(newNode, byteKey);
		return node;
	}

	private ARTNodeOld createNewNode(byte[] key, int depth, int value) {
		// TODO Auto-generated method stub
		return null;
	}

	private ARTNodeOld grow(ARTNodeOld node) {
		// TODO Auto-generated method stub
		return null;
	}


	private byte[] loadKey(ARTNodeOld node) {
		// TODO Auto-generated method stub
		return null;
	}


	// private void replace(Node node, Node leaf) {
	// // TODO Auto-generated method stub
	//
	// }

	private boolean isLeaf(ARTNodeOld node) {
		// TODO Auto-generated method stub
		return false;
	}

}
