/**
 * Created Apr 6, 2007
 */
package org.openl.base;

/**
 * @author snshor
 *
 */
public class NameSpacedThing extends NamedThing implements INameSpacedThing {

    String nameSpace;

    public NameSpacedThing() {
    }

    public NameSpacedThing(String name, String namespace) {
        super(name);
        nameSpace = namespace;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

}
