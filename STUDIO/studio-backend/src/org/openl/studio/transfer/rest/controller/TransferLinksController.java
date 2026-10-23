package org.openl.studio.transfer.rest.controller;

import java.nio.file.InvalidPathException;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.converter.ProjectIdConverter;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.session.ClientSessions;
import org.openl.studio.transfer.ClientTransferLinks;
import org.openl.studio.transfer.TransferOperation;
import org.openl.studio.transfer.TransferServlet;
import org.openl.studio.transfer.model.TransferLinkRequest;
import org.openl.studio.transfer.model.TransferLinkView;

/**
 * Issues transfer links, which move one project file to or from a client with no credential in the request.
 *
 * <p>Single-user mode authenticates nobody, so a client there calls the REST API itself and no link is issued.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/transfer-links", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Transfer Links (BETA)", description = "Short-lived links that move one project file without credentials")
@ConditionalOnExpression("'${user.mode}' != 'single'")
@RequiredArgsConstructor
public class TransferLinksController {

    private final ClientTransferLinks clientLinks;
    private final ClientSessions clientSessions;
    private final ProjectIdConverter projects;

    @Operation(summary = "transfer-links.create.summary", description = "transfer-links.create.desc")
    @ApiResponse(responseCode = "201", description = "transfer-links.create.201.desc")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TransferLinkView create(@Valid @RequestBody TransferLinkRequest request) {
        var clientKey = clientSessions.currentKey();
        if (clientKey == null) {
            throw new BadRequestException("transfer.link.client.message");
        }
        // A link forwards to the address of its file, so a path leading out of the project is refused here
        try {
            Repository.validatePath(request.path());
        } catch (InvalidPathException e) {
            throw new BadRequestException("file.path.invalid.message");
        }
        var projectId = projects.convert(request.projectId()).getProjectId().value();
        var issued = clientLinks.issue(SecurityContextHolder.getContext().getAuthentication().getName(), clientKey,
                operationOf(request, projectId));
        return new TransferLinkView(TransferServlet.PATH.substring(1) + "/" + issued.id(),
                issued.link().expiresAt());
    }

    private static TransferOperation operationOf(TransferLinkRequest request, String projectId) {
        return switch (request.kind()) {
            case DOWNLOAD -> new TransferOperation.Download(projectId, request.path(), request.branch(),
                    request.version(), false);
            case ARCHIVE -> new TransferOperation.Download(projectId, request.path(), request.branch(),
                    request.version(), true);
            case CONFLICT_VERSION -> {
                if (request.side() == null) {
                    throw new BadRequestException("transfer.link.side.message");
                }
                yield new TransferOperation.ConflictVersion(projectId, request.path(), request.side());
            }
            case UPLOAD -> new TransferOperation.Upload(projectId, request.path(), request.branch(),
                    request.conflictPolicy() == null ? ConflictPolicy.FAIL : request.conflictPolicy(),
                    request.createFolders() == null || request.createFolders(), request.size(), request.sha256());
        };
    }
}
