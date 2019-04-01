package org.openl.rules.diff.differs;

import java.util.Set;
import java.util.TreeSet;

public class MergeResult {
    String[] common;
    String[] added;
    String[] removed;

    public MergeResult(String[] common, String[] added, String[] removed) {
        this.common = common;
        this.added = added;
        this.removed = removed;
    }

    public MergeResult(Set<String> common, Set<String> added, Set<String> removed) {
        this(toSA(common), toSA(added), toSA(removed));
    }

    public static MergeResult mergeNames(Set<String> original, Set<String> others) {
        Set<String> common = new TreeSet<>();
        Set<String> originalOnly = new TreeSet<>();
        Set<String> othersOnly = new TreeSet<>();

        for (String s : original) {
            if (others.contains(s)) {
                common.add(s);
            } else {
                originalOnly.add(s);
            }
        }

        for (String s : others) {
            if (!original.contains(s)) {
                othersOnly.add(s);
            }
        }

        return new MergeResult(common, othersOnly, originalOnly);
    }

    public String[] getCommon() {
        return common;
    }

    public String[] getAdded() {
        return added;
    }

    public String[] getRemoved() {
        return removed;
    }

    static String[] toSA(Set<String> set) {
        return set.toArray(new String[set.size()]);
    }
}
