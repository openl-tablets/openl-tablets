package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;

/**
 * Collapses String both for marshall and unmarshall.
 */
public class CollapsedStringAdapter2 extends CollapsedStringAdapter {
    @Override
    public String marshal(String s) {
        return super.unmarshal(s);
    }
}
