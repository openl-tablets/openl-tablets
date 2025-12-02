package org.openl.studio.mcp.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import org.openl.info.OpenLVersion;
import org.openl.studio.mcp.McpController;

@Configuration
public class McpServerConfig {

    @Bean
    public WebMvcStatelessServerTransport webMvcStatelessServerTransport(ObjectMapper objectMapper) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .messageEndpoint("/mcp")
                .contextExtractor(request -> McpTransportContext.EMPTY)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> webMvcStatelessServerRouterFunction(WebMvcStatelessServerTransport webMvcProvider) {
        return webMvcProvider.getRouterFunction();
    }

    @Bean
    public McpSchema.ServerCapabilities.Builder capabilitiesBuilder() {
        return McpSchema.ServerCapabilities.builder();
    }

    @Bean
    public McpStatelessSyncServer mcpSyncServer(WebMvcStatelessServerTransport transportProvider,
                                                McpSchema.ServerCapabilities.Builder capabilitiesBuilder,
                                                ApplicationContext applicationContext) {
        McpSchema.Implementation serverInfo = new McpSchema.Implementation("openl-studio-mcp-server", OpenLVersion.getVersion());

        var serverBuilder = McpServer.sync(transportProvider);
        serverBuilder.serverInfo(serverInfo);

        // configure mcp tools
        List<Object> beansByAnnotation = applicationContext.getBeansWithAnnotation(McpController.class).values().stream().toList();
        if (!CollectionUtils.isEmpty(beansByAnnotation)) {
            capabilitiesBuilder.tools(true);
            beansByAnnotation.stream()
                    .map(bean -> {
                        try {
                            return ToolCallbacks.from(bean);
                        } catch (IllegalStateException ignored) {
                            // Ignore beans without @Tool methods
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .map(tool -> McpToolUtils.toStatelessSyncToolSpecification(tool, null))
                    .sorted(Comparator.comparing(spec -> spec.tool().name()))
                    .forEach(serverBuilder::tools);

            var promtSpecs = SyncMcpAnnotationProviders.statelessPromptSpecifications(beansByAnnotation);
            if (!promtSpecs.isEmpty()) {
                capabilitiesBuilder.prompts(true);
                promtSpecs.stream()
                        .sorted(Comparator.comparing(spec -> spec.prompt().name()))
                        .forEach(serverBuilder::prompts);
            }
        }

        serverBuilder.capabilities(capabilitiesBuilder.build());
        // It's necessary to share security context between HTTP request and MCP tool execution
        serverBuilder.immediateExecution(true);

        return serverBuilder.build();
    }

}
