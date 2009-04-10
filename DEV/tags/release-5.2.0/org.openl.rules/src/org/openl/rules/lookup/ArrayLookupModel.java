package org.openl.rules.lookup;

import java.util.Map;

public class ArrayLookupModel implements ISingleLookupModel {

    Map map;

    public ArrayLookupModel() {
    }

    public ArrayLookupModel(Map map) {
        this.map = map;
    }

    public Object find(Object lookup) {
        return map.get(lookup);
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

}
