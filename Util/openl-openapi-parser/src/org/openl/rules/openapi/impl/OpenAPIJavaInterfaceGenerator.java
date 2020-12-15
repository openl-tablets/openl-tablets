package org.openl.rules.openapi.impl;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.openl.gen.AnnotationDescriptionBuilder;
import org.openl.gen.JavaInterfaceByteCodeBuilder;
import org.openl.gen.MethodDescriptionBuilder;
import org.openl.gen.MethodParameterBuilder;
import org.openl.rules.model.scaffolding.GeneratedJavaInterface;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;

/**
 * This is not junit test, it's only for the testing the results of the generation. This class functionality must be
 * used int the OpenAPIProjectCreator for the generation and RepositoryTreeController in case of the regeneration.
 */
public class OpenAPIJavaInterfaceGenerator {

    private final ProjectModel projectModel;

    public OpenAPIJavaInterfaceGenerator(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public GeneratedJavaInterface generate() {
        JavaInterfaceByteCodeBuilder javaInterfaceBuilder = JavaInterfaceByteCodeBuilder
            .createWithDefaultPackage("OpenAPIService");
        boolean hasMethods = writeOpenAPIInterfaceMethods(projectModel.getSpreadsheetResultModels(), javaInterfaceBuilder);
        if (hasMethods) {
            return new GeneratedJavaInterface(javaInterfaceBuilder.getNameWithPackage(),
                    javaInterfaceBuilder.build().byteCode());
        } else {
            return GeneratedJavaInterface.EMPTY;
        }
    }

    private boolean writeOpenAPIInterfaceMethods(List<SpreadsheetModel> spreadsheetResultModels, JavaInterfaceByteCodeBuilder javaInterfaceBuilder) {
        boolean hasMethods = false;
        for (SpreadsheetModel sprModel : spreadsheetResultModels) {
            PathInfo pathInfo = sprModel.getPathInfo();
            String originalPath = pathInfo.getOriginalPath();
            if (originalPath.charAt(0) != '/') {
                originalPath = "/" + originalPath;
            }
            if (pathInfo.getFormattedPath().equals(originalPath)) {
                continue;
            }
            MethodDescriptionBuilder methodBuilder = MethodDescriptionBuilder.create(pathInfo.getFormattedPath(),
                    pathInfo.getReturnType().getJavaName());

            for (InputParameter parameter : sprModel.getParameters()) {
                MethodParameterBuilder methodParameterBuilder = MethodParameterBuilder
                    .create(parameter.getType().getJavaName());
                methodBuilder.addParameter(methodParameterBuilder.build());
            }

            methodBuilder.addAnnotation(
                AnnotationDescriptionBuilder.create(chooseOperationAnnotation(pathInfo.getOperation())).build());
            methodBuilder.addAnnotation(
                AnnotationDescriptionBuilder.create(Path.class).withProperty("value", originalPath).build());
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Consumes.class)
                .withProperty("value", pathInfo.getConsumes(), true)
                .build());
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Produces.class)
                .withProperty("value", pathInfo.getProduces(), true)
                .build());

            javaInterfaceBuilder.addAbstractMethod(methodBuilder.build());
            hasMethods = true;
        }
        return hasMethods;
    }

    private Class<? extends Annotation> chooseOperationAnnotation(String operation) {
        if (HttpMethod.GET.equalsIgnoreCase(operation)) {
            return GET.class;
        } else if (HttpMethod.POST.equalsIgnoreCase(operation)) {
            return POST.class;
        } else if (HttpMethod.PUT.equalsIgnoreCase(operation)) {
            return PUT.class;
        } else if (HttpMethod.DELETE.equalsIgnoreCase(operation)) {
            return DELETE.class;
        } else if (HttpMethod.PATCH.equalsIgnoreCase(operation)) {
            return PATCH.class;
        } else if (HttpMethod.HEAD.equals(operation)) {
            return HEAD.class;
        } else if (HttpMethod.OPTIONS.equals(operation)) {
            return OPTIONS.class;
        }
        throw new IllegalStateException("Unable to find operation annotation.");
    }
}
