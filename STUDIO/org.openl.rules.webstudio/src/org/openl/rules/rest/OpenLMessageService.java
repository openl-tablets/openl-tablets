package org.openl.rules.rest;

import java.util.Collection;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.MessageHandler;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Path("/message/")
@Produces(MediaType.APPLICATION_JSON)
public class OpenLMessageService {

    private static final MessageHandler messageHandler = new MessageHandler();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{messageId}/stacktrace")
    public String error(@PathParam("messageId") final long messageId) {
        return Optional.ofNullable(WebStudioUtils.getWebStudio(WebStudioUtils.getSession()))
            .map(WebStudio::getModel)
            .flatMap(model -> getMessageById(model, messageId))
            .filter(OpenLErrorMessage.class::isInstance)
            .map(OpenLErrorMessage.class::cast)
            .map(message -> ExceptionUtils.getStackTrace((Throwable) message.getError()))
            .orElse(null);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{messageId}/url")
    public String messageUrl(@PathParam("messageId") final long messageId) {
        return Optional.ofNullable(WebStudioUtils.getWebStudio(WebStudioUtils.getSession()))
            .flatMap(webStudio -> getMessageById(webStudio.getModel(), messageId).map(message -> {
                String sourceUrl = messageHandler.getSourceUrl(message.getSourceLocation(),
                    message.getSeverity().name(),
                    messageId,
                    webStudio.getModel());
                if (StringUtils.isBlank(sourceUrl)) {
                    sourceUrl = webStudio.url("message?type=" + message.getSeverity().name() + "&summary=" + messageId);
                }
                return sourceUrl;
            }))
            .orElse(null);
    }

    private static Optional<OpenLMessage> getMessageById(ProjectModel model, long messageId) {
        Collection<OpenLMessage> errors = model.isProjectCompilationCompleted() ? model.getModuleMessages()
                                                                                : model.getOpenedModuleMessages();
        return errors.stream().filter(m -> m.getId() == messageId).findFirst();
    }
}
