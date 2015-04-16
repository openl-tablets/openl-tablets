package org.openl.rules.calc;

import java.util.List;

import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.types.IOpenField;

public class SpreadsheetResultRootDictionaryContext extends RootDictionaryContext {
    public SpreadsheetResultRootDictionaryContext(IOpenField localVar, int maxDepthLevel) {
        super(new IOpenField[] { localVar }, maxDepthLevel);
    }

    @Override
    public IOpenField findField(String name) {
        String lowerCaseName = name.toLowerCase();
        List<IOpenField> ff = fields.get(lowerCaseName);
        if (ff == null) {
            IOpenField field = getTypeClass().getField(lowerCaseName);
            if (field != null) {
                initializeField(getRootField(), field, 1);
            }
            return field;
        }
        if (ff.size() > 1) {
            throw new AmbiguousVarException(lowerCaseName, ff);
        }

        return ff.get(0);
    }

    private SpreadsheetResultOpenClass getTypeClass() {
        return (SpreadsheetResultOpenClass) getRootField().getType();
    }

    private IOpenField getRootField() {
        return roots[0];
    }

}
