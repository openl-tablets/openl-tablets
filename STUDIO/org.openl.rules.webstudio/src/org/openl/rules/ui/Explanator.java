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

    public Integer getId(ExplanationNumberValue<?> value) {
        return value2id.get(value);
    }

    public ExplanationNumberValue<?> getValue(Integer id) {
        return id2value.get(id);
    }

    public Integer putIfAbsent(ExplanationNumberValue<?> value) {
        Integer id = getId(value);
        if (id != null) {
            return id;
        }

        id = ++uniqueId;
        value2id.put(value, id);
        id2value.put(id, value);
        return id;
    }

    private static Explanator getCurrent(String requestId) {
        return (Explanator) FacesUtils.getSessionParam(Constants.SESSION_PARAM_EXPLANATOR + requestId);
    }

    private static Explanator getCurrentOrCreate(String requestId) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            explanator = new Explanator();
            FacesUtils.getSessionMap().put(Constants.SESSION_PARAM_EXPLANATOR + requestId, explanator);
        }
        return explanator;
    }

    public static int getUniqueId(String requestId, ExplanationNumberValue<?> value) {
        return getCurrentOrCreate(requestId).putIfAbsent(value);
    }

    public static TreeNode getExplainTree(String requestId, String rootID) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            return null;
        }

        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.getValue(id);
        return new ExplainTreeBuilder().buildWithRoot(root);
    }

    public static List<String[]> getExplainList(String requestId, String rootID) {
        Explanator explanator = getCurrent(requestId);
        if (explanator == null) {
            return null;
        }

        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.getValue(id);
        return new Explanation().build(root);
    }

    public static void remove(String requestId) {
        FacesUtils.getSessionMap().remove(Constants.SESSION_PARAM_EXPLANATOR + requestId);
    }
}
