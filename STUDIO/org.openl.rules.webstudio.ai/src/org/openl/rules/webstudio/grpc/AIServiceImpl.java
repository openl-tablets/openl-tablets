package org.openl.rules.webstudio.grpc;

import javax.annotation.PreDestroy;

import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component("aiService")
public class AIServiceImpl implements AIService {

    private final String grpcAddress;
    private ManagedChannel channel;

    @Autowired
    public AIServiceImpl(PropertyResolver environment) {
        this.grpcAddress = environment.getProperty("webstudio.ai.grpc");
    }

    @Override
    public boolean isEnabled() {
        return grpcAddress != null && grpcAddress.contains(":");
    }

    private ManagedChannel getChannel() {
        if (channel == null || channel.isTerminated()) {
            synchronized (this) {
                if (channel == null || channel.isTerminated()) {
                    // Take the host and port from the environment variable
                    String[] parts = grpcAddress.split(":");
                    // Create a channel to connect to the server
                    this.channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1]))
                        .usePlaintext()
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
        channel.shutdown();
    }

}
