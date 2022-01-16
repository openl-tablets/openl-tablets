package org.openl.rules.rest;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.message.OpenLErrorMessage;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.MessageHandler;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/message/", produces = MediaType.APPLICATION_JSON_VALUE)
public class OpenLMessageService {

    private static final MessageHandler messageHandler = new MessageHandler();

    @GetMapping(value = "{messageId}/stacktrace", produces = MediaType.TEXT_PLAIN_VALUE)
    public String messageStacktrace(@PathVariable("messageId") final long messageId) {
        return Optional.ofNullable(WebStudioUtils.getWebStudio(WebStudioUtils.getSession()))
            .map(WebStudio::getModel)
            .flatMap(model -> model.getCompilationStatus()
                .getAllMessage()
                .stream()
                .filter(m -> m.getId() == messageId)
                .findFirst())
            .filter(OpenLErrorMessage.class::isInstance)
            .map(OpenLErrorMessage.class::cast)
            .map(message -> ExceptionUtils.getStackTrace((Throwable) message.getError()))
            .orElse(null);
    }

    @GetMapping(value = "{messageId}/url", produces = MediaType.TEXT_PLAIN_VALUE)
    public String messageUrl(@PathVariable("messageId") final long messageId) {
        return Optional.ofNullable(WebStudioUtils.getWebStudio(WebStudioUtils.getSession()))
            .flatMap(webStudio -> webStudio.getModel()
                .getCompilationStatus()
                .getAllMessage()
                .stream()
                .filter(m -> m.getId() == messageId)
                .findFirst()
                .map(message -> {
                    String sourceUrl = messageHandler.getSourceUrl(message.getSourceLocation(),
                        message.getSeverity().name(),
                        messageId,
                        webStudio.getModel());
                    if (StringUtils.isBlank(sourceUrl)) {
                        sourceUrl = webStudio
                            .url("message?type=" + message.getSeverity().name() + "&summary=" + messageId);
                    }
                    return sourceUrl;
                }))
            .orElse(null);
    }
}
