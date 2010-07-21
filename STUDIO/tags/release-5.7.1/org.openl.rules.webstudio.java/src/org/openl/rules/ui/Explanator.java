package org.openl.rules.ui;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.IdentityMap;
import org.openl.meta.DoubleValue;

public class Explanator {

    private static ThreadLocal<Explanator> current = new ThreadLocal<Explanator>();

    private static int uniqueId = 0;

    private IdentityMap value2id = new IdentityMap();

    private Map<Integer, Object> id2value = new HashMap<Integer, Object>();

    private Map<String, Explanation> explanators = new HashMap<String, Explanation>();

    public static Explanator getCurrent() {
        return current.get();
    }

    public static void setCurrent(Explanator expl) {
        current.set(expl);
    }

    public DoubleValue find(String expandID) {
        return (DoubleValue) id2value.get(new Integer(Integer.parseInt(expandID)));
    }

    public Explanation getExplanation(String rootID) {
        Explanation expl = explanators.get(rootID);
        if (expl == null) {
            int id = Integer.parseInt(rootID);

            DoubleValue value = (DoubleValue) id2value.get(new Integer(id));
            expl = new Explanation(this);
            expl.root = value;
            explanators.put(rootID, expl);
        }
        return expl;
    }

    public int getUniqueId(DoubleValue value) {
        Integer id = (Integer)value2id.get(value);

        if (id != null) {
            return id.intValue();
        }

        id = new Integer(++uniqueId);
        value2id.put(value, id);
        id2value.put(id, value);
        return id.intValue();
    }

    public void reset() {
        id2value = new HashMap<Integer, Object>();
        value2id = new IdentityMap();
        explanators = new HashMap<String, Explanation>();
    }
}
