package org.openl.rules.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        return repository.getRepositories().stream()
                .map(Repository::getId)
                .collect(Collectors.toList());
    }
}
