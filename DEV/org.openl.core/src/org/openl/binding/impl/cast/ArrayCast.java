package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenClass;

public class ArrayCast implements IOpenCast {

    private IOpenClass toComponentType;
    private IOpenCast openCast;
    private int dim;
    private int distance;

    public ArrayCast(IOpenClass to, IOpenCast openCast, int dim) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        if (to.isArray()) {
            throw new IllegalArgumentException("to arg can't be array type!");
        }
        this.toComponentType = to;
        this.openCast = openCast;
        this.distance = CastFactory.ARRAY_CAST_DISTANCE + openCast.getDistance();
        this.dim = dim;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        Class<?> c = from.getClass();
        Object f = from;
        List<Integer> dims = new ArrayList<Integer>();
        while (c.isArray()) {
            int length = Array.getLength(f);
            dims.add(length);
            c = c.getComponentType();
            if (length > 0) {
                f = Array.get(f, 0);
                if (Object.class.equals(c) && f != null) {
                    c = f.getClass();
                }
            } 
        }
        if (dim == dims.size()) {
            int[] dimensions = new int[dims.size()];
            for (int i = 0; i < dims.size(); i++) {
                dimensions[i] = dims.get(i);
            }
            Object convertedArray = Array.newInstance(toComponentType.getInstanceClass(), dimensions);
            int[] x = new int[dimensions.length];
            boolean g = dimensions[0] > 0;
            while (g) {
                int j = 0;
                Object p = from;
                Object w = convertedArray;
                while (j < dimensions.length - 1 && dimensions[j + 1] > 0) {
                    p = Array.get(p, x[j]);
                    w = Array.get(w, x[j]);
                    j++;
                }
                Object t = Array.get(p, x[j]);
                if (t != null && t.getClass().isArray() && j < dimensions.length - 1 && dimensions[j + 1] == 0) {
                    int[] y = new int[dimensions.length - j - 1];
                    t = Array.newInstance(toComponentType.getInstanceClass(), y);
                } else {
                    t = openCast.convert(t);
                }
                Array.set(w, x[j], t);
                j = 0;
                x[j]++;
                while (x[j] >= dimensions[j]) {
                    x[j] = 0;
                    if (j + 1 >= dimensions.length || dimensions[j + 1] == 0) {
                        g = false;
                        break;
                    }
                    x[j + 1]++;
                    j++;
                }
            }
            return convertedArray;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dim; i++) {
            sb.append("[]");
        }
        throw new ClassCastException(from.getClass().getSimpleName() + " can't be cast to " + toComponentType
            .getInstanceClass().getCanonicalName() + sb.toString());
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
