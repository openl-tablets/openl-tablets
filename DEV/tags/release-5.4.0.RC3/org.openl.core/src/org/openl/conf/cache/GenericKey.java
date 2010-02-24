package org.openl.conf.cache;

public final class GenericKey {
    
    private Object[] objects;

    public GenericKey(Object... obj) {
        this.objects = obj;
    }

    public boolean compare(Object[] anObj) {
    
        for (int i = 0; i < anObj.length; i++) {
            
            if (!objects[i].equals(anObj[i])) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean equals(Object x) {
        
        if (x != null && x instanceof GenericKey) {
            return objects.length == ((GenericKey) x).objects.length && compare(((GenericKey) x).objects);
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        
        int x = 17;

        for (int i = 0; i < objects.length; i++) {
            x += objects[i].hashCode() + 37;
        }
        return x;
    }
}