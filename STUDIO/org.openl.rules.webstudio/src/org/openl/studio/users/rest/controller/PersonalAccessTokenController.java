package org.openl.studio.users.rest.controller;

import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.security.CurrentUserInfo;
import org.openl.studio.security.NotPatAuth;
import org.openl.studio.security.pat.service.PatGeneratorService;
import org.openl.studio.users.model.pat.CreatePersonalAccessTokenRequest;
import org.openl.studio.users.model.pat.CreatedPersonalAccessTokenResponse;
import org.openl.studio.users.model.pat.PersonalAccessTokenResponse;
import org.openl.studio.users.service.pat.PersonalAccessTokenService;

/**
 * REST controller for Personal Access Token management.
 */
@ConditionalOnExpression("'${user.mode}' == 'oauth2' || '${user.mode}' == 'saml'")
@RestController
@RequestMapping(value = "/users/personal-access-tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@NotPatAuth
@Tag(name = "Users: Personal Access Tokens")
public class PersonalAccessTokenController {

    private final PersonalAccessTokenService crudService;
    private final PatGeneratorService generatorService;
    private final CurrentUserInfo currentUserInfo;

    @Autowired
    public PersonalAccessTokenController(PersonalAccessTokenService crudService,
                                         PatGeneratorService generatorService,
                                         CurrentUserInfo currentUserInfo) {
        this.crudService = crudService;
        this.generatorService = generatorService;
        this.currentUserInfo = currentUserInfo;
    }

    @Operation(summary = "pat.create.summary", description = "pat.create.desc")
    @ApiResponse(responseCode = "201", description = "pat.create.201.desc")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedPersonalAccessTokenResponse createToken(@Valid @RequestBody CreatePersonalAccessTokenRequest request) {
        String loginName = currentUserInfo.getUserName();

        if (crudService.existsByLoginNameAndName(loginName, request.name())) {
            throw new BadRequestException("pat.duplicate.name.message");
        }

        return generatorService.generateToken(loginName, request.name(), request.expiresAt());
    }

    @Operation(summary = "pat.list.summary", description = "pat.list.desc")
    @ApiResponse(responseCode = "200", description = "pat.list.200.desc")
    @GetMapping
    public List<PersonalAccessTokenResponse> listTokens() {
        String loginName = currentUserInfo.getUserName();
        return crudService.getTokensByUser(loginName);
    }

    @Operation(summary = "pat.get.summary", description = "pat.get.desc")
    @ApiResponse(responseCode = "200", description = "pat.get.200.desc")
    @GetMapping("/{publicId}")
    public PersonalAccessTokenResponse getToken(
            @Parameter(description = "pat.param.public-id.desc")
            @PathVariable String publicId) {
        String loginName = currentUserInfo.getUserName();
        var token = crudService.getTokenForUser(publicId, loginName);
        if (token == null) {
            throw new NotFoundException("pat.not.found.message");
        }
        return token;
    }

    @Operation(summary = "pat.delete.summary", description = "pat.delete.desc")
    @ApiResponse(responseCode = "204", description = "pat.delete.204.desc")
    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteToken(
            @Parameter(description = "pat.param.public-id.desc")
            @PathVariable String publicId) {
        String loginName = currentUserInfo.getUserName();
        var token = crudService.getTokenForUser(publicId, loginName);
        if (token == null) {
            throw new NotFoundException("pat.not.found.message");
        }
        crudService.deleteByPublicId(publicId);
    }
}
