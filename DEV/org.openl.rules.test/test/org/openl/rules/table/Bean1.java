/*
 * Created on Oct 24, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 */
public class Bean1 {

    private String key;
    private String name;

    private int intvalue;
    private Bean1 bref;

    public Bean1(String s) {
        throw new RuntimeException("zopa");
    }

    public Bean1() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String string) {
        key = string;
    }

    public int getIntvalue() {
        return intvalue;
    }

    public String getName() {
        return name;
    }

    public void setIntvalue(int i) {
        intvalue = i;
    }

    public void setName(String string) {
        name = string;
    }

    public Bean1 getBref() {
        return bref;
    }

    public void setBref(Bean1 bean1) {
        bref = bean1;
    }

    @Override
    public String toString() {
        return key + ":" + name + ":" + intvalue + ":" + (bref == null ? "n/a" : bref.key);
    }
}
