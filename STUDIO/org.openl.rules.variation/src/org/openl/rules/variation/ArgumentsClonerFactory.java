package org.openl.rules.variation;

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
