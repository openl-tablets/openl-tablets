package org.openl.rules.webstudio.grpc;

import javax.annotation.PreDestroy;

import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component("aiService")
public class AIServiceImpl implements AIService {
    private static final int CONNECTION_CHECK_INTERVAL = 2000;

    private final String grpcAddress;
    private ManagedChannel channel;
    private volatile boolean enabled = false;
    private volatile long lastEnabledCheckTime = 0L;

    @Autowired
    public AIServiceImpl(PropertyResolver environment) {
        this.grpcAddress = environment.getProperty("webstudio.ai.grpc");
    }

    @Override
    public boolean isEnabled() {
        if (System.currentTimeMillis() - lastEnabledCheckTime > CONNECTION_CHECK_INTERVAL) {
            boolean f = grpcAddress != null && grpcAddress.contains(":");
            if (f) {
                try {
                    ManagedChannel managedChannel = getChannel();
                    enabled = managedChannel != null && ConnectivityState.READY.equals(managedChannel.getState(true));
                } catch (Exception e) {
                    enabled = false;
                }
            } else {
                enabled = false;
            }
            lastEnabledCheckTime = System.currentTimeMillis();
        }
        return enabled;
    }

    public ManagedChannel getChannel() {
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
