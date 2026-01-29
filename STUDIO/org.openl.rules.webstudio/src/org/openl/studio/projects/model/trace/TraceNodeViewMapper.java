package org.openl.studio.projects.model.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

import org.openl.base.INamedThing;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TraceFormatter;
import org.openl.rules.webstudio.web.trace.node.ATableTracerNode;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.rules.webstudio.web.trace.node.SpreadsheetTracerLeaf;
import org.openl.studio.projects.service.trace.TraceParameterRegistry;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;

/**
 * Mapper for converting trace objects to TraceNodeView DTOs.
 */
public class TraceNodeViewMapper {

    private static final TraceNodeView NULL_NODE = TraceNodeView.builder()
            .key(-1)
            .title("null")
            .tooltip("null")
            .type("value")
            .lazy(false)
            .extraClasses("value")
            .build();

    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final TraceParameterRegistry parameterRegistry;

    public TraceNodeViewMapper(ObjectMapper objectMapper, TraceParameterRegistry parameterRegistry) {
        this.objectMapper = objectMapper;
        this.schemaGenerator = initSchemaGenerator(objectMapper);
        this.parameterRegistry = parameterRegistry;
    }

    private static SchemaGenerator initSchemaGenerator(ObjectMapper objectMapper) {
        var config = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new Swagger2Module())
                .build();
        return new SchemaGenerator(config);
    }

    public List<TraceNodeView> createSimpleNodes(Iterable<ITracerObject> children,
                                                  TraceHelper traceHelper,
                                                  boolean showRealNumbers) {
        List<TraceNodeView> nodes = new ArrayList<>();
        for (ITracerObject child : children) {
            nodes.add(createSimpleNode(child, traceHelper, showRealNumbers));
        }
        return nodes;
    }

    public TraceNodeView createSimpleNode(ITracerObject element,
                                          TraceHelper traceHelper,
                                          boolean showRealNumbers) {
        if (element == null) {
            return NULL_NODE;
        }

        return mapSimpleNode(element, traceHelper, showRealNumbers).build();
    }

    private TraceNodeView.Builder mapSimpleNode(ITracerObject element,
                                          TraceHelper traceHelper,
                                          boolean showRealNumbers) {
        var type = getType(element);
        return TraceNodeView.builder()
                .key(traceHelper.getNodeKey(element))
                .title(TraceFormatter.getDisplayName(element, !showRealNumbers))
                .tooltip(TraceFormatter.getDisplayName(element, !showRealNumbers))
                .type(type)
                .lazy(!element.isLeaf())
                .extraClasses(type)
                .error(hasErrors(element));
    }

    private boolean hasErrors(ITracerObject element) {
        if (element instanceof ATableTracerNode tableNode) {
            return tableNode.getError() != null;
        } else if (element instanceof RefToTracerNodeObject refNode) {
            return hasErrors(refNode.getOriginalTracerNode());
        }
        return false;
    }

    public TraceNodeView createDetailedNode(ITracerObject element,
                                            TraceHelper traceHelper,
                                            boolean showRealNumbers) {
        if (element == null) {
            return NULL_NODE;
        }

        return mapSimpleNode(element, traceHelper, showRealNumbers)
                .parameters(buildInputParameters(element))
                .context(buildContext(element))
                .result(buildResult(element))
                .errors(buildErrors(element))
                .build();
    }

    public TraceParameterValue buildParameterValue(ParameterWithValueDeclaration param, boolean preferLazy) {
        if (param == null) {
            return null;
        }

        var type = param.getType();
        var rawValue = param.getValue();
        var isSimple = type != null && type.isSimple();
        var shouldBeLazy = preferLazy && rawValue != null && !isSimple;

        if (shouldBeLazy && parameterRegistry != null) {
            return TraceParameterValue.builder()
                    .name(param.getName())
                    .description(type != null ? type.getDisplayName(INamedThing.SHORT) : null)
                    .lazy(true)
                    .parameterId(parameterRegistry.register(param))
                    .schema(generateSchema(type))
                    .build();
        }

        return TraceParameterValue.builder()
                .name(param.getName())
                .description(type != null ? type.getDisplayName(INamedThing.SHORT) : null)
                .lazy(false)
                .value(serializeValue(rawValue, type))
                .schema(generateSchema(type))
                .build();
    }

    private List<TraceParameterValue> buildInputParameters(ITracerObject tto) {
        var tracerNode = getTableTracerNode(tto);
        if (tracerNode == null || tracerNode.getTraceObject() == null) {
            return Collections.emptyList();
        }

        ExecutableRulesMethod method = tracerNode.getTraceObject();
        Object[] params = tracerNode.getParameters();

        List<TraceParameterValue> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            var param = new ParameterWithValueDeclaration(
                    method.getSignature().getParameterName(i),
                    params[i],
                    method.getSignature().getParameterType(i)
            );
            result.add(buildParameterValue(param, true));
        }
        return result;
    }

    private TraceParameterValue buildContext(ITracerObject tto) {
        var tracerNode = getTableTracerNode(tto);
        if (tracerNode == null || tracerNode.getContext() == null) {
            return null;
        }
        return buildParameterValue(new ParameterWithValueDeclaration("context", tracerNode.getContext()), false);
    }

    private TraceParameterValue buildResult(ITracerObject tto) {
        var tracerNode = getTableTracerNode(tto);
        if (tracerNode == null || tracerNode.getTraceObject() == null) {
            return null;
        }
        var resultValue = tto.getResult();
        if (resultValue == null) {
            return null;
        }
        IOpenClass type = tracerNode.getTraceObject().getType();
        if (tracerNode instanceof SpreadsheetTracerLeaf spreadsheetTracerLeaf) {
            type = spreadsheetTracerLeaf.getSpreadsheetCell().getType();
        }
        var result = new ParameterWithValueDeclaration("return",
                resultValue,
                type);
        return buildParameterValue(result, true);
    }

    private List<MessageDescription> buildErrors(ITracerObject tto) {
        List<OpenLMessage> messages = getErrors(tto);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageDescription> result = new ArrayList<>();
        for (OpenLMessage msg : messages) {
            result.add(new MessageDescription(msg.getId(), msg.getSummary(), msg.getSeverity()));
        }
        return result;
    }

    private List<OpenLMessage> getErrors(ITracerObject tto) {
        if (tto instanceof ATableTracerNode tableNode) {
            Throwable error = tableNode.getError();
            if (error != null) {
                Throwable cause = error.getCause();
                return OpenLMessagesUtils.newErrorMessages(Objects.requireNonNullElse(cause, error));
            }
        } else if (tto instanceof RefToTracerNodeObject refNode) {
            return getErrors(refNode.getOriginalTracerNode());
        }
        return Collections.emptyList();
    }

    private ObjectNode generateSchema(IOpenClass type) {
        if (type == null || type.getInstanceClass() == null) {
            return null;
        }
        try {
            return schemaGenerator.generateSchema(type.getInstanceClass());
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode serializeValue(Object value, IOpenClass type) {
        if (value == null) {
            return null;
        }
        try {
            Class<?> toType = null;
            if (type instanceof CustomSpreadsheetResultOpenClass csrOpenClass) {
                toType = csrOpenClass.getBeanClass();
            }
            value = SpreadsheetResult.convertSpreadsheetResult(value, toType, type, null);
            return objectMapper.valueToTree(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getType(ITracerObject element) {
        var type = element.getType();
        if (type == null) {
            type = StringUtils.EMPTY;
        }
        if (element instanceof DTRuleTraceObject condition) {
            if (!condition.isSuccessful()) {
                return type + " fail";
            }
            if (findResult(element.getChildren()) != null) {
                return type + " result";
            }
            return type + " no_result";
        }
        return type;
    }

    private ITracerObject findResult(Iterable<ITracerObject> children) {
        for (ITracerObject child : children) {
            if (child instanceof DTRuleTracerLeaf) {
                return child;
            }
            var result = findResult(child.getChildren());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private ATableTracerNode getTableTracerNode(ITracerObject tto) {
        if (tto instanceof RefToTracerNodeObject refNode) {
            return getTableTracerNode(refNode.getOriginalTracerNode());
        } else if (tto instanceof ATableTracerNode tableNode) {
            return tableNode;
        } else if (tto != null && tto.getParent() instanceof ATableTracerNode tableNode) {
            return tableNode;
        }
        return null;
    }
}
