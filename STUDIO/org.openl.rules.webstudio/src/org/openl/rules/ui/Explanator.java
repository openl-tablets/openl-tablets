package org.openl.rules.ui;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.ui.tree.richfaces.ExplainTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.util.Constants;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Explanator {

    private int uniqueId = 0;

    private IdentityHashMap<ExplanationNumberValue<?>, Integer> value2id = new IdentityHashMap<>();

    private Map<Integer, ExplanationNumberValue<?>> id2value = new HashMap<>();

    private static Explanator getCurrent(String requestId) {
        return (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR + requestId);
    }

    public static int getUniqueId(String requestId, ExplanationNumberValue<?> value) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR + requestId, explanator);
        }

        Integer id = explanator.value2id.get(value);
        if (id != null) {
            return id;
        }

        id = ++explanator.uniqueId;
        explanator.value2id.put(value, id);
        explanator.id2value.put(id, value);
        return id;
    }

    public static TreeNode getExplainTree(String requestId, String rootID) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            return null;
        }

        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.id2value.get(id);
        return new ExplainTreeBuilder().buildWithRoot(root);
    }

    public static List<String[]> getExplainList(String requestId, String rootID) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            return null;
        }

        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.id2value.get(id);
        return new Explanation().build(root);
    }

    public static void remove(String requestId) {
        FacesUtils.getSessionMap().remove(Constants.SESSION_PARAM_EXPLANATOR + requestId);
    }
}
