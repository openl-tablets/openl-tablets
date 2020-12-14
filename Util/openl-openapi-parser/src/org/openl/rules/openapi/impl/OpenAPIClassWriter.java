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
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;

/**
 * This is not junit test, it's only for the testing the results of the generation. This class functionality must be
 * used int the OpenAPIProjectCreator for the generation and RepositoryTreeController in case of the regeneration.
 */
public class OpenAPIClassWriter {

    private final ProjectModel projectModel;
    private byte[] byteCode;

    public OpenAPIClassWriter(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public void generateInterface() {
        JavaInterfaceByteCodeBuilder javaInterfaceBuilder = JavaInterfaceByteCodeBuilder
            .createWithDefaultPackage("OpenAPIService");
        writeOpenAPIInterfaceMethods(projectModel.getSpreadsheetResultModels(), javaInterfaceBuilder);
        byteCode = javaInterfaceBuilder.build().byteCode();
    }

    byte[] getByteCode() {
        return byteCode;
    }

    private void writeOpenAPIInterfaceMethods(List<SpreadsheetModel> spreadsheetResultModels,
            JavaInterfaceByteCodeBuilder javaInterfaceBuilder) {
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
                .withProperty("value", pathInfo.getConsumes())
                .build());
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Produces.class)
                .withProperty("value", pathInfo.getProduces())
                .build());

            javaInterfaceBuilder.addAbstractMethod(methodBuilder.build());
        }
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

    /*
     * @Override public void visit(int version, int access, String name, String signature, String superName, String[]
     * interfaces) { super.visit(version, access, name, signature, superName, interfaces); if
     * (CollectionUtils.isNotEmpty(projectModel.getSpreadsheetResultModels())) { // there is a need to append
     * RuntimeContext for the input parameters writeOpenAPIInterface(projectModel.getSpreadsheetResultModels()); }
     * 
     * if (CollectionUtils.isNotEmpty(projectModel.getNotOpenLModels())) {
     * writeOpenAPIInterface(projectModel.getNotOpenLModels()); } }
     */
}
