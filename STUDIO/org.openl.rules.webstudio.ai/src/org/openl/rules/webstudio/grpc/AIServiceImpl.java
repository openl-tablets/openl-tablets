package org.openl.rules.webstudio.grpc;

import javax.annotation.PreDestroy;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;

@Component("aiService")
public class AIServiceImpl implements AIService {

    private final String grpcAddress;
    private ManagedChannel channel;

    @Autowired
    public AIServiceImpl(@Value("${openl.assistant.url}") String grpcAddress) {
        this.grpcAddress = grpcAddress;
    }

    @Override
    public boolean isEnabled() {
        return StringUtils.hasText(grpcAddress);
    }

    private ManagedChannel getChannel() {
        if (channel == null || channel.isTerminated()) {
            synchronized (this) {
                if (channel == null || channel.isTerminated()) {
                    // Create a channel to connect to the server
                    this.channel = ManagedChannelBuilder.forTarget(grpcAddress)
                            .usePlaintext()
                            .maxInboundMessageSize(1024 * 1024 * 1024) // 1GB
                            .build();
                }
            }
        }
        return channel;
    }

    @Override
    public WebstudioAIServiceGrpc.WebstudioAIServiceBlockingStub getBlockingStub() {
        return WebstudioAIServiceGrpc.newBlockingStub(getChannel());
    }

    @PreDestroy
    void destroy() {
        // Close the channel
        if (channel != null) {
            channel.shutdown();
        }
    }

}
