/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.BitSet;
import java.util.Iterator;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 */
public class EnumDomain<T> implements IDomain<T> {

    class EnumDomainIterator extends AOpenIterator<T> {

        private BitSetIterator bsi = new BitSetIterator(bits);

        @Override
        public boolean hasNext() {
            return bsi.hasNext();
        }

        @Override
        public T next() {
            int idx = bsi.nextInt();
            return enumeration.allObjects[idx];
        }

    }

    private BitSet bits;

    private Enum<T> enumeration;

    public EnumDomain(Enum<T> enumeration, BitSet bits) {
        this.bits = bits;
        this.enumeration = enumeration;
    }

    public EnumDomain(Enum<T> enumeration, T[] objs) {
        bits = new BitSet(enumeration.size());
        this.enumeration = enumeration;

        for (int i = 0; i < objs.length; i++) {
            int idx = enumeration.getIndex(objs[i]);
            bits.set(idx);
        }
    }

    public EnumDomain(T[] elements) {
        this(new Enum<>(elements), elements);
    }

    public EnumDomain<T> and(EnumDomain<T> sd) {
        checkOperand(sd);

        if (bits.equals(sd.bits)) {
            return this;
        }

        BitSet copy = (BitSet) bits.clone();
        copy.and(sd.bits);
        return new EnumDomain<>(enumeration, copy);

    }

    void checkOperand(EnumDomain<T> sd) {

        if (sd.getEnum() != enumeration) {
            throw new RuntimeException("Can not use subsets of different domains");
        }

    }

    public boolean contains(T obj) {
        try {
            int idx = enumeration.getIndex(obj);
            return bits.get(idx);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (!(obj instanceof EnumDomain)) {
            return false;
        }

        EnumDomain<T> ed = (EnumDomain<T>) obj;

        return enumeration.equals(ed.enumeration) && bits.equals(ed.bits);
    }

    @Override
    public IType getElementType() {
        return null;
    }

    public Enum<T> getEnum() {
        return enumeration;
    }

    @Override
    public int hashCode() {
        return enumeration.hashCode() * 37 + bits.hashCode();
    }

    @Override
    public Iterator<T> iterator() {
        return new EnumDomainIterator();
    }

    public EnumDomain<T> not() {
        int size = enumeration.size();

        BitSet bs = (BitSet) bits.clone();

        bs.flip(0, size);

        return new EnumDomain<>(enumeration, bs);
    }

    public EnumDomain<T> or(EnumDomain<T> sd) {
        checkOperand(sd);

        if (bits.equals(sd.bits)) {
            return this;
        }

        BitSet copy = (BitSet) bits.clone();
        copy.or(sd.bits);
        return new EnumDomain<>(enumeration, copy);
    }

    @Override
    public boolean selectObject(T obj) {
        return contains(obj);
    }

    public int size() {
        return bits.cardinality();
    }

    public EnumDomain<T> sub(EnumDomain<T> sd) {
        checkOperand(sd);

        if (bits.equals(sd.bits)) {
            return this;
        }

        BitSet copy = (BitSet) bits.clone();
        copy.andNot(sd.bits);
        return new EnumDomain<>(enumeration, copy);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean f = false;
        for (Object o : enumeration.getAllObjects()) {
            if (f) {
                sb.append(",");
            } else {
                f = true;
            }
            sb.append(o.toString());
        }
        return "[" + sb.toString() + "]";
    }

}
