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
    public IOpenField findField(String fieldName, boolean strictMatch) {
        String name = strictMatch ? fieldName : fieldName.toLowerCase();
        List<IOpenField> ff = strictMatch ? fields.get(name) : lowerCaseFields.get(name);

        if (ff == null) {
            IOpenField field = getRootField().getType().getField(fieldName, strictMatch);
            if (field != null) {
                initializeField(getRootField(), field, 1);
                ff = strictMatch ? fields.get(name) : lowerCaseFields.get(name);
            }
        }

        if (ff == null) {
            return null;
        }
        if (ff.size() > 1) {
            throw new AmbiguousFieldException(fieldName, ff);
        }

        return ff.get(0);
    }

    private IOpenField getRootField() {
        return roots[0];
    }

}
