package org.openl.rules.variation;

import com.rits.cloning.Cloner;
import com.rits.cloning.ObjenesisInstantiationStrategy;

@Deprecated
final class ArgumentsClonerFactory {
    private ArgumentsClonerFactory() {
    }

    public static Cloner getCloner() {
        try {
            return (Cloner) Class.forName("org.openl.rules.table.OpenLArgumentsCloner").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new Cloner(new ObjenesisInstantiationStrategy());
        }
    }
}
