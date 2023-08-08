package org.openl.rules.webstudio.web.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.ai.OpenL2TextUtils;
import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;
import org.openl.rules.webstudio.ai.WebstudioAi;
import org.openl.rules.webstudio.grpc.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.grpc.StatusRuntimeException;

@Component
public class AISearchImpl implements AISearch {

    private static final int MAX_ROWS_DT = Integer.MAX_VALUE;
    private static final boolean LLM_FILTERING = true;
    private static final int MAX_RESULTS_COUNT = 10;

    private final AIService aiService;

    @Autowired
    public AISearchImpl(AIService aiService) {
        this.aiService = aiService;
    }

    private WebstudioAi.TableSyntaxNode toProtoTableSyntaxNode(TableSyntaxNode tableSyntaxNode) {
        // TODO Need to use some kind of caching here
        boolean isDt = XlsNodeTypes.XLS_DT.toString().equals(tableSyntaxNode.getType());
        String table = OpenL2TextUtils
            .tableSyntaxNodeToString(tableSyntaxNode, false, false, isDt ? MAX_ROWS_DT : Integer.MAX_VALUE);
        return WebstudioAi.TableSyntaxNode.newBuilder()
            .setUri(tableSyntaxNode.getUri())
            .setTable(table)
            .setType(tableSyntaxNode.getType())
            .build();
    }

    @Override
    public List<TableSyntaxNode> filter(String query, List<TableSyntaxNode> tableSyntaxNodes) {
        if (aiService.isEnabled() && tableSyntaxNodes != null && !tableSyntaxNodes.isEmpty()) {
            try {
                WebstudioAIServiceGrpc.WebstudioAIServiceBlockingStub blockingStub = aiService.getBlockingStub();
                WebstudioAi.SearchRequest.Builder builder = WebstudioAi.SearchRequest.newBuilder()
                    .setQuery(query)
                    .setLimit(MAX_RESULTS_COUNT)
                    .setLlmFiltering(LLM_FILTERING);
                tableSyntaxNodes.stream().map(this::toProtoTableSyntaxNode).forEach(builder::addTableSyntaxNodes);
                WebstudioAi.SearchRequest searchRequest = builder.build();
                WebstudioAi.SearchReply searchReply = blockingStub.search(searchRequest);
                Map<String, TableSyntaxNode> cache = tableSyntaxNodes.stream()
                    .collect(Collectors.toMap(TableSyntaxNode::getUri, p -> p));
                List<TableSyntaxNode> filtered = new ArrayList<>();
                searchReply.getUrisList().stream().map(cache::get).forEach(filtered::add);
                return filtered;
            } catch (StatusRuntimeException ignored) {
                // Fails safe behaviour if AI service is not available
            }
        }
        return Collections.emptyList();
    }
}
