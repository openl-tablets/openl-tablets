package org.openl.rules.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.ai.OpenL2TextUtils;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;
import org.openl.rules.webstudio.ai.WebstudioAi;
import org.openl.rules.webstudio.grpc.AIService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping(value = "/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Hidden
public class AssistantController {

    private final static String CHAT_TYPE_ASSISTANT = "ASSISTANT";
    private final static String CHAT_TYPE_USER = "USER";

    private final static boolean REPLACE_ALIAS_TYPES_WITH_BASE = false;
    private final static int MAX_DEPTH_COLLECT_TYPES = 1;

    private final AIService aiService;

    @Autowired
    public AssistantController(AIService aiService) {
        this.aiService = aiService;
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

    public static class Message {
        String type;
        String text;
        private List<Ref> refs;

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
    }

    public static class MessageArrayWrapper {
        private List<Message> messages;
        private final String tableId;

        @JsonCreator
        public MessageArrayWrapper(@JsonProperty("tableId") String tableId,
                @JsonProperty("messages") List<Message> messages) {
            this.messages = messages;
            this.tableId = tableId;
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
    }

    private static List<Ref> buildRefs(List<WebstudioAi.Ref> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        return refs.stream().map(e -> new Ref(e.getUrl(), e.getTitle())).collect(Collectors.toList());
    }

    @PostMapping(value = "/ask_help")
    public Message[] askHelp(HttpSession httpSession, @RequestBody MessageArrayWrapper messageArrayWrapper) {
        Message[] messages = messageArrayWrapper.getMessages().toArray(new Message[0]);
        // get all messages except the last one are ignored
        Message[] history = new Message[messages.length - 1];
        if (history.length > 0) {
            System.arraycopy(messages, 0, history, 0, messages.length - 1);
        }
        WebStudio studio = WebStudioUtils.getWebStudio(httpSession);
        IOpenLTable table = StringUtils.isNotBlank(messageArrayWrapper.getTableId()) ? studio.getModel()
            .getTableById(messageArrayWrapper.getTableId()) : null;
        WebstudioAi.ChatRequest.Builder chatRequestBuilder = WebstudioAi.ChatRequest.newBuilder();
        if (table != null) {
            TableSyntaxNode tableSyntaxNode = studio.getModel().findNode(table.getUri());
            if (tableSyntaxNode != null) {
                String currentOpenedTable = OpenL2TextUtils.methodToString(
                    (ExecutableRulesMethod) tableSyntaxNode.getMember(),
                    false,
                    false,
                    false,
                    Integer.MAX_VALUE);

                Set<IOpenClass> types = new HashSet<>();
                Set<IOpenMethod> methodRefs = OpenL2TextUtils
                    .methodRefs((ExecutableRulesMethod) tableSyntaxNode.getMember());
                for (IOpenClass type : OpenL2TextUtils
                    .methodTypes((ExecutableRulesMethod) tableSyntaxNode.getMember())) {
                    OpenL2TextUtils.collectTypes(type, types, MAX_DEPTH_COLLECT_TYPES, REPLACE_ALIAS_TYPES_WITH_BASE);
                }
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
                WebstudioAi.TableSyntaxNode tableSyntaxNodeMessage = WebstudioAi.TableSyntaxNode.newBuilder()
                    .setUri(table.getUri())
                    .setType(tableSyntaxNode.getType())
                    .setTable(currentOpenedTable)
                    .build();
                chatRequestBuilder.addTableSyntaxNodes(tableSyntaxNodeMessage);
            }
        }

        Message lastMessage = messages[messages.length - 1];
        chatRequestBuilder.setChatType(WebstudioAi.ChatType.KNOWLEDGE)
            .setMessage(lastMessage.text)
            .addAllHistory(Stream.of(history)
                .map(e -> WebstudioAi.ChatMessage.newBuilder()
                    .setText(e.text)
                    .setType(CHAT_TYPE_USER.equals(e.getType()) ? WebstudioAi.MessageType.USER
                                                                : WebstudioAi.MessageType.ASSISTANT)
                    .build())
                .collect(Collectors.toList()));
        WebstudioAi.ChatRequest request = chatRequestBuilder.build();
        WebstudioAIServiceGrpc.WebstudioAIServiceBlockingStub blockingStub = aiService.getBlockingStub();
        WebstudioAi.ChatReply response = blockingStub.chat(request);
        return response.getMessagesList()
            .stream()
            .map(e -> new Message(e.getText(), CHAT_TYPE_ASSISTANT, buildRefs(e.getRefsList())))
            .toArray(Message[]::new);
    }
}
