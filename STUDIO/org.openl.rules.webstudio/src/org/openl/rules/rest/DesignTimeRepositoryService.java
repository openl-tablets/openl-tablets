package org.openl.rules.rest;

import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/repos")
@Produces(MediaType.APPLICATION_JSON)
public class DesignTimeRepositoryService {

    private final DesignTimeRepository repository;

    @Autowired
    public DesignTimeRepositoryService(DesignTimeRepository repository) {
        this.repository = repository;
    }

    @GET
    public List<String> getRepositoryList() {
        SecurityChecker.allow(Privileges.ADMIN);
        return repository.getRepositories().stream().map(Repository::getId).collect(Collectors.toList());
    }

    @GET
    @Path("/{repo-name}/projects")
    public Response getProjectListByRepository(@PathParam("repo-name") String repoName) {
        if (repository.getRepository(repoName) == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Repository is not found.").build();
        }
        List<Map<String, Object>> result = repository.getProjects(repoName)
            .stream()
            .map(this::mapProjectResponse)
            .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    private <T extends AProject> Map<String, Object> mapProjectResponse(T src) {
        Map<String, Object> dest = new HashMap<>();
        dest.put("name", src.getName());
        dest.put("modifiedBy", Optional.of(src.getFileData()).map(FileData::getAuthor).orElse(null));
        dest.put("modifiedAt",
            Optional.of(src.getFileData())
                .map(FileData::getModifiedAt)
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null));
        dest.put("branch", Optional.of(src.getFileData()).map(FileData::getBranch).orElse(null));
        dest.put("rev", Optional.of(src.getFileData()).map(FileData::getVersion).orElse(null));
        Function<String, String> pathRelativizer = other -> Optional.of(src.getFileData())
            .map(FileData::getName)
            .map(Paths::get)
            .map(root -> root.relativize(Paths.get(other)))
            .map(java.nio.file.Path::toString)
            .map(str -> str.replace('\\', '/'))
            .orElse(null);
        dest.put("elements",
            src.getArtefacts()
                .stream()
                .map(AProjectArtefact::getFileData)
                .map(FileData::getName)
                .map(pathRelativizer)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList()));
        return dest;
    }
}
