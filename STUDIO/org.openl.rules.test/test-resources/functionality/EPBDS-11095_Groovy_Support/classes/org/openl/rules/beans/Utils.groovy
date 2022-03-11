package org.openl.rules.beans

class Utils {
    static def String hello(org.openl.generated.beans.OpenLDefinedType type) {
        return type.getName() + "_hello";
    }
}
