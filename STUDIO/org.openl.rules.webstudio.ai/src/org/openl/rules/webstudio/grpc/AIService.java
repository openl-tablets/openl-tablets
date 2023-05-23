package org.openl.rules.webstudio.grpc;

import org.openl.rules.webstudio.ai.WebstudioAIServiceGrpc;

public interface AIService {
    boolean isEnabled();

    WebstudioAIServiceGrpc.WebstudioAIServiceBlockingStub getBlockingStub();
}
