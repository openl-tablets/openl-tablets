package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;

/**
 * Trims and collapses whitespace for both marshal and unmarshal. A blank result
 * (empty or whitespace-only) collapses to {@code null} so the surrounding XML element
 * is omitted from the output.
 */
public class CollapsedStringAdapter2 extends CollapsedStringAdapter {
    @Override
    public String unmarshal(String s) {
        return toNullIfBlank(super.unmarshal(s));
    }

    @Override
    public String marshal(String s) {
        return toNullIfBlank(super.unmarshal(s));
    }

    private static String toNullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
