package org.openl.rules.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.CreateUpdateProjectModelValidator;
import org.openl.rules.rest.validation.ZipArchiveValidator;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@javax.ws.rs.Path("/repos")
@Produces(MediaType.APPLICATION_JSON)
public class DesignTimeRepositoryService {

    private final DesignTimeRepository designTimeRepository;
    private final PropertyResolver propertyResolver;
    private final BeanValidationProvider validationProvider;
    private final CreateUpdateProjectModelValidator createUpdateProjectModelValidator;
    private final ZipArchiveValidator zipArchiveValidator;
    private final FolderProjectSaver zipProjectSaver;

    @Inject
    public DesignTimeRepositoryService(DesignTimeRepository designTimeRepository,
            PropertyResolver propertyResolver,
            BeanValidationProvider validationService,
            CreateUpdateProjectModelValidator createUpdateProjectModelValidator,
            ZipArchiveValidator zipArchiveValidator,
            FolderProjectSaver zipProjectSaver) {
        this.designTimeRepository = designTimeRepository;
        this.propertyResolver = propertyResolver;
        this.validationProvider = validationService;
        this.createUpdateProjectModelValidator = createUpdateProjectModelValidator;
        this.zipArchiveValidator = zipArchiveValidator;
        this.zipProjectSaver = zipProjectSaver;
    }

    @GET
    public List<String> getRepositoryList() {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        return designTimeRepository.getRepositories().stream().map(Repository::getId).collect(Collectors.toList());
    }

    @GET
    @javax.ws.rs.Path("/{repo-name}/projects")
    public Response getProjectListByRepository(@PathParam("repo-name") String repoName) {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        Repository repository = getRepositoryByName(repoName);
        List<Map<String, Object>> result = designTimeRepository.getProjects(repoName)
            .stream()
            .filter(proj -> !proj.isDeleted())
            .map(src -> mapProjectResponse(src, repository.supports().mappedFolders()))
            .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    private <T extends AProject> Map<String, Object> mapProjectResponse(T src, boolean foldersSupport) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("name", src.getBusinessName());
        dest.put("modifiedBy", Optional.of(src.getFileData()).map(FileData::getAuthor).orElse(null));
        dest.put("modifiedAt",
            Optional.of(src.getFileData())
                .map(FileData::getModifiedAt)
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null));
        dest.put("branch", Optional.of(src.getFileData()).map(FileData::getBranch).orElse(null));
        dest.put("rev", Optional.of(src.getFileData()).map(FileData::getVersion).orElse(null));
        if (foldersSupport) {
            dest.put("path", src.getRealPath());
        }
        return dest;
    }

    @PUT
    @javax.ws.rs.Path("/{repo-name}/projects/{project-name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object createProjectFromZip(@PathParam("repo-name") String repoName,
            @PathParam("project-name") String projectName,
            @Multipart(value = "path", required = false) String path,
            @Multipart(value = "comment", required = false) String comment,
            @Multipart(value = "template", type = "application/zip") InputStream inZip,
            @Multipart(value = "overwrite", required = false) boolean overwrite) throws IOException {

        getRepositoryByName(repoName);
        SecurityChecker.allow(overwrite ? Privileges.EDIT_PROJECTS : Privileges.CREATE_PROJECTS);

        CreateUpdateProjectModel model = new CreateUpdateProjectModel(repoName,
            getUserName(),
            projectName,
            path,
            StringUtils.isNotBlank(comment) ? comment : createCommentsService(repoName).createProject(projectName),
            overwrite);
        validationProvider.validate(model, createUpdateProjectModelValidator);

        final Path archiveTmp = Files.createTempFile(projectName, ".zip");
        try {
            IOUtils.copyAndClose(inZip, Files.newOutputStream(archiveTmp));
            validationProvider.validate(archiveTmp, zipArchiveValidator);
            FileData data = zipProjectSaver.save(model, archiveTmp);
            return mapFileDataResponse(data);
        } finally {
            FileUtils.delete(archiveTmp);
        }
    }

    private Comments createCommentsService(String repoName) {
        return new Comments(propertyResolver, repoName);
    }

    private Map<String, Object> mapFileDataResponse(FileData src) {
        Map<String, Object> dest = new LinkedHashMap<>();
        dest.put("branch", src.getBranch());
        dest.put("rev", src.getVersion());
        return dest;
    }

    private String getUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    public Repository getRepositoryByName(String repoName) {
        Repository repository = designTimeRepository.getRepository(repoName);
        if (repository != null) {
            return repository;
        }
        throw new NotFoundException(String.format("Design Repository '%s' is not found!", repoName));
    }
}
