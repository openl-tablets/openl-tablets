package org.openl.studio.projects.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import org.openl.rules.serialization.ObjectMapperConfigurationParsingException;
import org.openl.studio.common.exception.ConflictException;

@Service
@RequiredArgsConstructor
public class ProjectObjectMapperService {

    private final WorkspaceProjectService projectService;
    private final Environment environment;

    public ObjectMapper createObjectMapper() {
        try {
            var objectMapperFactory = projectService.getWebStudio().getCurrentProjectJacksonObjectMapperFactoryBean();
            objectMapperFactory.setEnvironment(environment);
            return objectMapperFactory.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) {
            throw missingClass(e);
        } catch (ObjectMapperConfigurationParsingException e) {
            if (e.getCause() instanceof ClassNotFoundException cause) {
                throw missingClass(cause);
            }
            throw e;
        }
    }

    private static ConflictException missingClass(ClassNotFoundException cause) {
        return new ConflictException("object.mapper.configuration.failed.message", cause.getMessage());
    }
}
