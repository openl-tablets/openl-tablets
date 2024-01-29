package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.ai.OpenL2TextUtils;
import org.openl.rules.webstudio.ai.WebstudioAi;
import org.openl.rules.webstudio.grpc.AIService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(value = "/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Hidden
public class AssistantController {

    private final static String CHAT_TYPE_ASSISTANT = "ASSISTANT";
    private final static String CHAT_TYPE_USER = "USER";

    private final static boolean REPLACE_ALIAS_TYPES_WITH_BASE = false;
    private final static int MAX_DEPTH_COLLECT_TYPES = 1;
    private final static ObjectMapper objectMapper = createObjectMapper();

    private final AIService aiService;

    @Autowired
    public AssistantController(AIService aiService) {
        this.aiService = aiService;
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    public static class Ref {
        String url;
        String title;

        @JsonCreator
        public Ref(@JsonProperty("url") String url, @JsonProperty("title") String title) {
            this.url = url;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class TSN {
        String uri;
        String table;
        String type;
        String description;
        String businessDimensionProperties;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getBusinessDimensionProperties() {
            return businessDimensionProperties;
        }

        public void setBusinessDimensionProperties(String businessDimensionProperties) {
            this.businessDimensionProperties = businessDimensionProperties;
        }
    }

    public static class Message {
        String type;
        String text;
        List<Ref> refs;
        List<TSN> tableSyntaxNodes = new ArrayList<>();
        List<String> refTypes = new ArrayList<>();
        List<String> refMethods = new ArrayList<>();

        @JsonCreator
        public Message(@JsonProperty("text") String text,
                @JsonProperty("type") String type,
                @JsonProperty("refs") List<Ref> refs) {
            this.text = text;
            this.type = type;
            this.refs = refs;
        }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Ref> getRefs() {
            return refs;
        }

        public void setRefs(List<Ref> refs) {
            this.refs = refs;
        }

        public List<TSN> getTableSyntaxNodes() {
            return tableSyntaxNodes;
        }

        public void setTableSyntaxNodes(List<TSN> tableSyntaxNodes) {
            this.tableSyntaxNodes = tableSyntaxNodes;
        }

        public List<String> getRefTypes() {
            return refTypes;
        }

        public void setRefTypes(List<String> refTypes) {
            this.refTypes = refTypes;
        }

        public List<String> getRefMethods() {
            return refMethods;
        }

        public void setRefMethods(List<String> refMethods) {
            this.refMethods = refMethods;
        }
    }

    public static class MessageArrayWrapper {
        private List<Message> messages;
        private final String tableId;
        private final Boolean rate;

        @JsonCreator
        public MessageArrayWrapper(@JsonProperty("tableId") String tableId,
                @JsonProperty("messages") List<Message> messages,
                @JsonProperty("rate") boolean rate) {
            this.messages = messages;
            this.tableId = tableId;
            this.rate = rate;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public String getTableId() {
            return tableId;
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }

        public boolean getRate() {
            return rate;
        }
    }

    private static List<Ref> buildRefs(List<WebstudioAi.Ref> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        return refs.stream().map(e -> new Ref(e.getUrl(), e.getTitle())).collect(Collectors.toList());
    }

    private static WebstudioAi.ChatRequest getChatRequest(HttpSession httpSession,
            MessageArrayWrapper messageArrayWrapper,
            WebstudioAi.ChatType chatType) {
        var messages = messageArrayWrapper.getMessages().toArray(new Message[0]);
        // get all messages except the last one are ignored
        var history = new Message[messages.length - 1];
        if (history.length > 0) {
            System.arraycopy(messages, 0, history, 0, messages.length - 1);
        }
        var studio = WebStudioUtils.getWebStudio(httpSession);
        var table = StringUtils.isNotBlank(messageArrayWrapper.getTableId()) ? studio.getModel()
            .getTableById(messageArrayWrapper.getTableId()) : null;
        var chatRequestBuilder = WebstudioAi.ChatRequest.newBuilder();
        if (table != null) {
            var tableSyntaxNode = studio.getModel().findNode(table.getUri());
            if (tableSyntaxNode != null) {
                String currentOpenedTable;
                if (tableSyntaxNode.getMember() instanceof ExecutableRulesMethod) {
                    currentOpenedTable = OpenL2TextUtils.methodToString(
                        (ExecutableRulesMethod) tableSyntaxNode.getMember(),
                        false,
                        false,
                        false,
                        Integer.MAX_VALUE);
                } else {
                    currentOpenedTable = OpenL2TextUtils
                        .tableSyntaxNodeToString(tableSyntaxNode, false, false, Integer.MAX_VALUE);
                }
                var types = new HashSet<IOpenClass>();
                for (IOpenClass type : OpenL2TextUtils.methodTypes(tableSyntaxNode)) {
                    OpenL2TextUtils.collectTypes(type, types, MAX_DEPTH_COLLECT_TYPES, REPLACE_ALIAS_TYPES_WITH_BASE);
                }
                var methodRefs = OpenL2TextUtils.methodRefs(tableSyntaxNode);
                for (IOpenMethod method : methodRefs) {
                    OpenL2TextUtils
                        .collectTypes(method.getType(), types, MAX_DEPTH_COLLECT_TYPES, REPLACE_ALIAS_TYPES_WITH_BASE);
                }
                types.stream()
                    .map(e -> OpenL2TextUtils.openClassToString(e, REPLACE_ALIAS_TYPES_WITH_BASE))
                    .forEach(chatRequestBuilder::addRefTypes);
                // Build the request tableRefMethods
                methodRefs.stream()
                    .map(e -> OpenL2TextUtils.methodHeaderToString(e, REPLACE_ALIAS_TYPES_WITH_BASE) + "{}")
                    .forEach(chatRequestBuilder::addRefMethods);
                var tableSyntaxNodeMessageBuilder = WebstudioAi.TableSyntaxNode.newBuilder()
                    .setUri(table.getUri())
                    .setType(tableSyntaxNode.getType())
                    .setTable(currentOpenedTable);
                if (tableSyntaxNode.getMember() instanceof ExecutableRulesMethod) {
                    var dimensionalProperties = OpenL2TextUtils.dimensionalPropertiesToString(
                        (ExecutableRulesMethod) tableSyntaxNode.getMember(),
                        objectMapper);
                    if (dimensionalProperties != null) {
                        tableSyntaxNodeMessageBuilder.setBusinessDimensionProperties(dimensionalProperties);
                    }
                    var description = ((ExecutableRulesMethod) tableSyntaxNode.getMember()).getMethodProperties()
                        .getDescription();
                    if (StringUtils.isNotBlank(description)) {
                        tableSyntaxNodeMessageBuilder.setDescription(description);
                    }
                }
                var tableSyntaxNodeMessage = tableSyntaxNodeMessageBuilder.build();
                chatRequestBuilder.addTableSyntaxNodes(tableSyntaxNodeMessage);
            }
        }

        var lastMessage = messages[messages.length - 1];
        chatRequestBuilder.setChatType(chatType)
            .setMessage(lastMessage.text)
            .addAllHistory(Stream.of(history)
                .map(e -> WebstudioAi.ChatMessage.newBuilder()
                    .setText(e.text)
                    .setType(CHAT_TYPE_USER.equals(e.getType()) ? WebstudioAi.MessageType.USER
                                                                : WebstudioAi.MessageType.ASSISTANT)
                    .build())
                .collect(Collectors.toList()));
        return chatRequestBuilder.build();
    }

    private Message buildMessage(WebstudioAi.ChatRequest chatRequest, String text, String type, List<Ref> refs) {
        var message = new Message(text, type, refs);
        for (var tableSyntaxNode : chatRequest.getTableSyntaxNodesList()) {
            var tsn = new TSN();
            tsn.setUri(tableSyntaxNode.getUri());
            tsn.setTable(tableSyntaxNode.getTable());
            tsn.setType(tableSyntaxNode.getType());
            tsn.setDescription(tableSyntaxNode.getDescription());
            tsn.setBusinessDimensionProperties(tableSyntaxNode.getBusinessDimensionProperties());
            message.getTableSyntaxNodes().add(tsn);
        }
        chatRequest.getRefTypesList().forEach(message.getRefTypes()::add);
        chatRequest.getRefMethodsList().forEach(message.getRefMethods()::add);
        return message;
    }

    @PostMapping(value = "/ask_help")
    public Message[] askHelp(HttpSession httpSession, @RequestBody MessageArrayWrapper messageArrayWrapper) {
        var request = getChatRequest(httpSession, messageArrayWrapper, WebstudioAi.ChatType.KNOWLEDGE);
        var blockingStub = aiService.getBlockingStub();
        var response = blockingStub.chat(request);
        return response.getMessagesList()
            .stream()
            .map(e -> buildMessage(request, e.getText(), CHAT_TYPE_ASSISTANT, buildRefs(e.getRefsList())))
            .toArray(Message[]::new);
    }

    private WebstudioAi.TableSyntaxNode buildGrpcTSN(TSN tsn) {
        var builder = WebstudioAi.TableSyntaxNode.newBuilder();
        builder.setUri(tsn.getUri());
        builder.setTable(tsn.getTable());
        builder.setType(tsn.getType());
        if (tsn.getBusinessDimensionProperties() != null) {
            builder.setBusinessDimensionProperties(tsn.getBusinessDimensionProperties());
        }
        if (tsn.getDescription() != null) {
            builder.setDescription(tsn.getDescription());
        }
        return builder.build();
    }

    @PostMapping(value = "/rate")
    public void rate(@RequestBody MessageArrayWrapper messageArrayWrapper) {
        var chatRequestBuilder = WebstudioAi.ChatRequest.newBuilder();
        chatRequestBuilder.setChatType(WebstudioAi.ChatType.RATE);
        var lastMessage = messageArrayWrapper.getMessages().get(messageArrayWrapper.getMessages().size() - 1);
        chatRequestBuilder.setMessage(lastMessage.getText());
        lastMessage.refTypes.forEach(chatRequestBuilder::addRefTypes);
        lastMessage.refMethods.forEach(chatRequestBuilder::addRefMethods);
        lastMessage.tableSyntaxNodes.forEach(e -> chatRequestBuilder.addTableSyntaxNodes(buildGrpcTSN(e)));
        // get all messages except the last one, build history
        for (int i = 0; i < messageArrayWrapper.getMessages().size() - 1; i++) {
            var message = messageArrayWrapper.getMessages().get(i);
            var chatMessageBuilder = WebstudioAi.ChatMessage.newBuilder();
            chatMessageBuilder.setText(message.getText());
            chatMessageBuilder.setType(CHAT_TYPE_USER.equals(message.getType()) ? WebstudioAi.MessageType.USER
                                                                                : WebstudioAi.MessageType.ASSISTANT);
            chatRequestBuilder.addHistory(chatMessageBuilder.build());
        }

        var chatRequest = chatRequestBuilder.build();
        var rateRequest = WebstudioAi.RateRequest.newBuilder()
            .setRate(messageArrayWrapper.getRate())
            .setChatRequest(chatRequest)
            .build();
        var blockingStub = aiService.getBlockingStub();
        blockingStub.rate(rateRequest);
    }
}
