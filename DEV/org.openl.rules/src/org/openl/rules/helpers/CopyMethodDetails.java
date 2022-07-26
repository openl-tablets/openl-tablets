package org.openl.rules.helpers;

import java.util.Objects;

import org.openl.binding.impl.cast.MethodDetails;

import com.rits.cloning.Cloner;

public class CopyMethodDetails implements MethodDetails {
    private final Cloner cloner;

    public CopyMethodDetails(Cloner cloner) {
        this.cloner = Objects.requireNonNull(cloner, "cloner cannot be null");
    }

    public Cloner getCloner() {
        return cloner;
    }
}
