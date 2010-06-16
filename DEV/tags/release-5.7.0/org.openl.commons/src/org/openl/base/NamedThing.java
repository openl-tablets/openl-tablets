/**
 * Created Jan 19, 2007
 */
package org.openl.base;

/**
 * @author snshor
 *
 */
public class NamedThing implements INamedThing {

    private String name;

    public NamedThing() {
    }

    public NamedThing(String name) {
        this.name = name;
    }

    public String getDisplayName(int mode) {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
