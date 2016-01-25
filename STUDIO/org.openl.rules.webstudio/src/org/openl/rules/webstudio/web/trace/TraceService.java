package org.openl.rules.webstudio.web.trace;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.openl.base.INamedThing;
import org.openl.rules.dtx.trace.DTConditionTraceObject;
import org.openl.rules.dtx.trace.DTRuleTracerLeaf;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.openl.vm.trace.ITracerObject;
import org.springframework.stereotype.Service;

/**
 * Request scope managed bean providing logic for trace tree page of OpenL
 * Studio.
 */
@Service
@Path("/trace/")
@Produces(MediaType.APPLICATION_JSON)
public class TraceService {

    @GET
    @Path("nodes")
    public List<TraceNode> getNodes(@QueryParam("id") Integer id, @Context HttpServletRequest request) {
        TraceHelper traceHelper = WebStudioUtils.getTraceHelper(request.getSession());
        ITracerObject element = traceHelper.getTableTracer(id == null ? 0 : id);
        return createNodes(element.getChildren(), traceHelper);
    }

    private List<TraceNode> createNodes(Iterable<ITracerObject> children, TraceHelper traceHelper) {
        List<TraceNode> nodes = new ArrayList<TraceNode>(16);
        for (ITracerObject child : children) {
            nodes.add(createNode(child, traceHelper));
        }
        return nodes;
    }

    private TraceNode createNode(ITracerObject element, TraceHelper traceHelper) {

        TraceNode node = new TraceNode();
        if (element == null) {
            node.setTitle("null");
            node.setExtraClasses("value");
            return node;
        }
        String name = getDisplayName(element, INamedThing.SHORT);
        node.setTitle(name);

        String title = getDisplayName(element, INamedThing.REGULAR);
        node.setTooltip(title);

        int key = traceHelper.getNodeKey(element);
        node.setKey(key);

        String type = getType(element);
        node.setExtraClasses(type);

        boolean leaf = element.isLeaf();
        node.setLazy(!leaf);
        return node;
    }

    private String getDisplayName(ITracerObject obj, int mode) {
        return obj.getDisplayName(mode);
    }

    private String getType(ITracerObject element) {
        String type = element.getType();
        if (type == null) {
            type = StringUtils.EMPTY;
        }
        if (element instanceof DTConditionTraceObject) {
            DTConditionTraceObject condition = (DTConditionTraceObject) element;
            if (!condition.isSuccessful()) {
                return type + " fail";
            } else {
                ITracerObject result = findResult(element.getChildren());
                if (result != null) {
                    return type + " result";
                }
                return type + " no_result";
            }
        }
        return type;
    }

    private ITracerObject findResult(Iterable<ITracerObject> children) {
        for (ITracerObject child : children) {
            if (child instanceof DTRuleTracerLeaf) {
                return child;
            }
            ITracerObject result = findResult(child.getChildren());
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
