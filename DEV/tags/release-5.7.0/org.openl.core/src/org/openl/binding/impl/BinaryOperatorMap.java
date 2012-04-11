package org.openl.binding.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.util.StringTool;

public class BinaryOperatorMap {
    static Map<String, BinaryOperatorMap> map;
    String name;
    boolean symmetrical;
    String[] synonims;
    String inverse;

    String unaryInverseMethodAdaptor;

    static void add(String name, boolean symmetrical, String synonims, String inverse, String unaryInverseMethodAdaptor) {
        String[] syntokens = null;
        if (synonims != null) {
            syntokens = StringTool.tokenize(synonims, ",");
        }

        map.put(name, new BinaryOperatorMap(name, symmetrical, syntokens, inverse, unaryInverseMethodAdaptor));
    }

    public static BinaryOperatorMap findOp(String name) {
        return getMap().get(name);
    }

    static synchronized Map<String, BinaryOperatorMap> getMap() {
        if (map != null) {
            return map;
        }

        map = new HashMap<String, BinaryOperatorMap>();

        add("le", false, null, "ge", null);
        add("lt", false, null, "gt", null);
        add("ge", false, null, "le", null);
        add("gt", false, null, "lt", null);
        add("eq", true, null, null, null);
        add("add", true, null, null, null);

        return map;

    }

    public BinaryOperatorMap(String name, boolean symmetrical, String[] synonims, String inverse,
            String unaryInverseMethodAdaptor) {
        this.name = name;
        this.symmetrical = symmetrical;
        this.synonims = synonims;
        this.inverse = inverse;
        this.unaryInverseMethodAdaptor = unaryInverseMethodAdaptor;
    }

    public String getInverse() {
        return inverse;
    }

    public String getName() {
        return name;
    }

    public String[] getSynonims() {
        return synonims;
    }

    public String getUnaryInverseMethodAdaptor() {
        return unaryInverseMethodAdaptor;
    }

    public boolean isSymmetrical() {
        return symmetrical;
    }

}
