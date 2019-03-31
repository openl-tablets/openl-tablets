package org.openl.rules.ui.tree;

/**
 * Class that represent key of node.
 */
public class NodeKey implements Comparable<Object> {

    /**
     * Triple of names.
     */
    private String[] value;

    /**
     * Node weight.
     */
    private int weight;

    /**
     * Creates new node key.
     *
     * @param weight node weight
     * @param value  triple of names
     */
    public NodeKey(int weight, String[] value) {
        this.weight = weight;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Object arg0) {

        NodeKey key = (NodeKey) arg0;

        if (weight == key.weight) {
            if (value[0] == null) {
                return key.value[0] == null ? 0 : -1;
            } else {
                return value[0].compareTo(key.value[0]);
            }
        }

        return weight - key.weight;
    }

    /**
     * Gets the node names.
     *
     * @return triple of names
     */
    public String[] getValue() {
        return value;
    }

    /**
     * Gets weight value of node.
     *
     * @return weight value of node
     */
    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeKey nodeKey = (NodeKey) o;

        // Make this method consistent with compareTo()
        return compareTo(nodeKey) == 0;
    }

    @Override
    public int hashCode() {
        // Make this method consistent with compareTo() and equals()
        int result = value[0].hashCode();
        result = 31 * result + weight;
        return result;
    }
}
