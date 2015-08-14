package org.openl.rules.ui;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.ui.tree.richfaces.ExplainTreeBuilder;
import org.openl.rules.ui.tree.richfaces.TreeNode;
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

    public static TreeNode getExplainTree(String rootID) {
        Explanator explanator = getCurrent();
        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.id2value.get(id);
        TreeNode rfTree = new ExplainTreeBuilder().buildWithRoot(root);
        return rfTree;
    }

    public static List<String[]> getExplainList(String rootID) {
        Explanator explanator = getCurrent();
        int id = Integer.parseInt(rootID);
        ExplanationNumberValue<?> root = explanator.id2value.get(id);
        List<String[]> result = new Explanation().build(root);
        return result;
    }
}
