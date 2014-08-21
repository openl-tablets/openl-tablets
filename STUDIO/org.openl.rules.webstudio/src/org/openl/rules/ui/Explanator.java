package org.openl.rules.ui;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.webstudio.web.util.Constants;

public class Explanator {

    private static int uniqueId = 0;

    private IdentityHashMap<ExplanationNumberValue<?>, Integer> value2id = new IdentityHashMap<ExplanationNumberValue<?>, Integer>();

    private Map<Integer, ExplanationNumberValue<?>> id2value = new HashMap<Integer, ExplanationNumberValue<?>>();

    private Map<String, Explanation> explanators = new HashMap<String, Explanation>();

    public static Explanator getCurrent() {
        Explanator explanator = (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR, explanator);
        }
        return explanator;
    }

    public ExplanationNumberValue<?> find(String expandID) {
        return id2value.get(Integer.parseInt(expandID));
    }

    public Explanation getExplanation(String rootID) {
        Explanation expl = explanators.get(rootID);
        if (expl == null) {
            int id = Integer.parseInt(rootID);

            ExplanationNumberValue<?> value = id2value.get(id);
            expl = new Explanation(this);
            expl.setRoot(value);
            explanators.put(rootID, expl);
        }
        return expl;
    }

    public int getUniqueId(ExplanationNumberValue<?> value) {
        Integer id = value2id.get(value);

        if (id != null) {
            return id;
        }

        id = ++uniqueId;
        value2id.put(value, id);
        id2value.put(id, value);
        return id;
    }

    public void reset() {
        id2value = new HashMap<Integer, ExplanationNumberValue<?>>();
        value2id = new IdentityHashMap<ExplanationNumberValue<?>, Integer>();
        explanators = new HashMap<String, Explanation>();
    }
}
