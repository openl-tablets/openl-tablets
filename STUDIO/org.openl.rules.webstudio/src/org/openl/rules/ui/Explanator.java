package org.openl.rules.ui;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.ui.tree.richfaces.ExplainTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.util.Constants;

import java.util.List;

public class Explanator extends ObjectRegistry<ExplanationNumberValue<?>> {

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
