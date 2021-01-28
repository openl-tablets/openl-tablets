package org.openl.rules.calc;

import java.util.List;

import org.openl.binding.exception.AmbiguousFieldException;
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
            IOpenField field = getRootField().getType().getField(name);
            if (field != null) {
                initializeField(getRootField(), field, 1);
                ff = fields.get(lowerCaseName);
            }
        }

        if (ff == null) {
            return null;
        }
        if (ff.size() > 1) {
            throw new AmbiguousFieldException(lowerCaseName, ff);
        }

        return ff.get(0);
    }

    private IOpenField getRootField() {
        return roots[0];
    }

}
