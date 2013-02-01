package org.openl.rules.variation;

import com.rits.cloning.Cloner;

final class ArgumentsClonerFactory {
    private ArgumentsClonerFactory() {
    }

    public static Cloner getCloner() {
        try {
            return (Cloner) Class.forName("org.openl.rules.table.InputArgumentsCloner").newInstance();
        } catch (Exception e) {
            return new Cloner();
        }
    }
}
