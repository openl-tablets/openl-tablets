package org.openl.rules.webstudio.web.trace;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Request scope managed bean providing logic for trace tree page of OpenL Studio.
 */
@RestController
@RequestMapping(value = "/trace", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Trace")
public class TraceController {

    @Operation(summary = "trace.get-nodes.summary", description = "trace.get-nodes.desc")
    @GetMapping("/nodes")
    public List<TraceNode> getNodes(
            @Parameter(description = "trace.field.id") @RequestParam(value = "id", required = false) Integer id,
            @Parameter(description = "trace.field.showRealNumbers") @RequestParam(value = "showRealNumbers", required = false) Boolean showRealNumbers,
            HttpServletRequest request) {
        TraceHelper traceHelper = WebStudioUtils.getTraceHelper(request.getSession());
        ITracerObject element = traceHelper.getTableTracer(id == null ? 0 : id);
        return createNodes(element.getChildren(), traceHelper, showRealNumbers != null && showRealNumbers);
    }

    private List<TraceNode> createNodes(Iterable<ITracerObject> children,
            TraceHelper traceHelper,
            boolean showRealNumbers) {
        List<TraceNode> nodes = new ArrayList<>(16);
        for (ITracerObject child : children) {
            nodes.add(createNode(child, traceHelper, showRealNumbers));
        }
        return nodes;
    }

    private TraceNode createNode(ITracerObject element, TraceHelper traceHelper, boolean showRealNumbers) {

        TraceNode node = new TraceNode();
        if (element == null) {
            node.setTitle("null");
            node.setExtraClasses("value");
            return node;
        }
        String name = TraceFormatter.getDisplayName(element, !showRealNumbers);
        node.setTitle(name);
        node.setTooltip(name);

        int key = traceHelper.getNodeKey(element);
        node.setKey(key);

        String type = getType(element);
        node.setExtraClasses(type);

        boolean leaf = element.isLeaf();
        node.setLazy(!leaf);
        return node;
    }

    private String getType(ITracerObject element) {
        String type = element.getType();
        if (type == null) {
            type = StringUtils.EMPTY;
        }
        if (element instanceof DTRuleTraceObject) {
            DTRuleTraceObject condition = (DTRuleTraceObject) element;
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
