package org.openl.rules.variation;

/*
 * #%L
 * OpenL - Variation
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import com.rits.cloning.Cloner;
import com.rits.cloning.ObjenesisInstantiationStrategy;

final class ArgumentsClonerFactory {
    private ArgumentsClonerFactory() {
    }

    public static Cloner getCloner() {
        try {
            return (Cloner) Class.forName("org.openl.rules.table.OpenLArgumentsCloner").newInstance();
        } catch (Exception e) {
            return new Cloner(new ObjenesisInstantiationStrategy());
        }
    }
}
