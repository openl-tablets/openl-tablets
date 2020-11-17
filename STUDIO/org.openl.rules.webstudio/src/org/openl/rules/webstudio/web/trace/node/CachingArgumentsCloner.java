package org.openl.rules.webstudio.web.trace.node;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.StubSpreadSheetResult;
import org.openl.rules.table.OpenLCloner;

import com.rits.cloning.IDeepCloner;
import com.rits.cloning.IFastCloner;

/**
 * This cloner is based on assumption that hashCode() and equals() methods are cheaper than cloning huge objects.
 *
 * If the object overrides hasCode() and equals() methods, it's clone will be reused if it's not changed. If the object
 * does not override hasCode() and equals() methods, then cloned object and original one always will be not equal. So
 * original object can be safely modified, it does not break logic. If equal object is not found in the cache then it
 * will be cloned and stored in the cache.
 *
 * If after clone() new instance is not created (for example, object is immutable), such object is not cached.
 *
 * Clones cannot be changed! If cloned object can be changed in future, this cloner cannot be used. For example, in the
 * trace all arguments are cloned and that cloned objects are never changed, they are used only to store arguments state
 * and show them to the user later. In this case we can safely reuse already cloned object in other method invocation if
 * it's not changed since that.
 */
public class CachingArgumentsCloner extends OpenLCloner {
    private static final ThreadLocal<CachingArgumentsCloner> instance = new ThreadLocal<>();

    private final Map<Object, Object> cache = new HashMap<>();

    private CachingArgumentsCloner() {
        dontCloneInstanceOf(StubSpreadSheetResult.class);
        registerFastCloner(SpreadsheetResult.class, new SpreadsheetResultFastCloner());
    }

    @Override
    public <T> T cloneInternal(T o, Map<Object, Object> clones) throws IllegalAccessException {
        if (o == null) {
            return null;
        } else if (o instanceof SpreadsheetResult) {
            return super.cloneInternal(o, clones);
        }

        @SuppressWarnings("unchecked")
        T clone = (T) cache.get(o);
        if (clone != null) {
            return clone;
        }

        T t = super.cloneInternal(o, clones);
        if (t != null && t != o && !t.getClass().isArray()) {
            cache.put(t, t);
        }
        return t;
    }

    public static CachingArgumentsCloner getInstance() {
        return instance.get();
    }

    public static void initInstance() {
        instance.set(new CachingArgumentsCloner());
    }

    public static void removeInstance() {
        instance.remove();
    }

    private static class SpreadsheetResultFastCloner implements IFastCloner {
        @Override
        public Object clone(Object t, IDeepCloner cloner, Map<Object, Object> clones) {
            SpreadsheetResult spr = (SpreadsheetResult) t;

            Object[][] clonedResults = cloner.deepClone(spr.getResults(), clones);

            SpreadsheetResult clonedSpr = new SpreadsheetResult(spr);
            clonedSpr.setResults(clonedResults);
            return clonedSpr;
        }
    }
}
