package org.openl.rules.openapi.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.openl.gen.AnnotationDescriptionBuilder;
import org.openl.gen.JavaInterfaceByteCodeBuilder;
import org.openl.gen.JavaInterfaceImplBuilder;
import org.openl.gen.MethodDescriptionBuilder;
import org.openl.gen.MethodParameterBuilder;
import org.openl.gen.TypeDescription;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.util.StringUtils;

public class OpenAPIJavaClassGenerator {

    private static final Class<?> DEFAULT_DATATYPE_CLASS = Object.class;
    private static final String RULES_CTX_CLASS = IRulesRuntimeContext.class.getName();
    public static final String VALUE = "value";

    private final ProjectModel projectModel;

    public OpenAPIJavaClassGenerator(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public OpenAPIGeneratedClasses generate() {
        JavaInterfaceByteCodeBuilder javaInterfaceBuilder = JavaInterfaceByteCodeBuilder
            .createWithDefaultPackage("OpenAPIService");
        boolean hasMethods = false;
        for (SpreadsheetModel method : projectModel.getSpreadsheetResultModels()) {
            if (method.getPathInfo().getOriginalPath().equals("/" + method.getPathInfo().getFormattedPath())) {
                continue;
            }
            MethodDescriptionBuilder methodDesc = visitInterfaceMethod(method,
                projectModel.isRuntimeContextProvided(),
                false);
            javaInterfaceBuilder.addAbstractMethod(methodDesc.build());
            hasMethods = true;
        }
        OpenAPIGeneratedClasses.Builder builder = OpenAPIGeneratedClasses.Builder.initialize();
        for (SpreadsheetModel extraMethod : projectModel.getNotOpenLModels()) {
            hasMethods = true;
            JavaInterfaceImplBuilder extraMethodBuilder = new JavaInterfaceImplBuilder(ServiceExtraMethodHandler.class);
            JavaClassFile javaClassFile = new JavaClassFile(extraMethodBuilder.getBeanName(),
                extraMethodBuilder.byteCode());
            builder.addCommonClass(javaClassFile);
            MethodDescriptionBuilder methodDesc = visitInterfaceMethod(extraMethod, false, true);
            methodDesc.addAnnotation(AnnotationDescriptionBuilder.create(ServiceExtraMethod.class)
                .withProperty(VALUE, new TypeDescription(javaClassFile.getJavaNameWithPackage()))
                .build());
            javaInterfaceBuilder.addAbstractMethod(methodDesc.build());
        }

        if (hasMethods) {
            builder.setAnnotationTemplateClass(
                new JavaClassFile(javaInterfaceBuilder.getNameWithPackage(), javaInterfaceBuilder.build().byteCode()));
        }
        return builder.build();
    }

    private MethodDescriptionBuilder visitInterfaceMethod(SpreadsheetModel sprModel,
            boolean runtimeContext,
            boolean extraMethod) {

        final PathInfo pathInfo = sprModel.getPathInfo();
        final TypeInfo returnTypeInfo = pathInfo.getReturnType();
        MethodDescriptionBuilder methodBuilder = MethodDescriptionBuilder.create(pathInfo.getFormattedPath(),
            resolveType(returnTypeInfo));

        if (runtimeContext) {
            methodBuilder.addParameter(MethodParameterBuilder.create(RULES_CTX_CLASS).build());
        }

        for (InputParameter parameter : sprModel.getParameters()) {
            methodBuilder.addParameter(visitMethodParameter(parameter, extraMethod));
        }

        if (returnTypeInfo.isDatatype()) {
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(RulesType.class)
                .withProperty(VALUE, removeArray(returnTypeInfo.getSimpleName()))
                .build());
        }

        writeWebServiceAnnotations(methodBuilder, pathInfo);

        return methodBuilder;
    }

    private TypeDescription visitMethodParameter(InputParameter parameter, boolean extraMethod) {
        final TypeInfo paramType = parameter.getType();
        MethodParameterBuilder methodParamBuilder = MethodParameterBuilder.create(resolveType(paramType));
        if (paramType.isDatatype()) {
            methodParamBuilder.addAnnotation(AnnotationDescriptionBuilder.create(RulesType.class)
                .withProperty(VALUE, removeArray(paramType.getSimpleName()))
                .build());
        }
        if (extraMethod) {
            methodParamBuilder.addAnnotation(
                AnnotationDescriptionBuilder.create(Name.class).withProperty(VALUE, parameter.getName()).build());
        }
        if (parameter.isInPath()) {
            methodParamBuilder.addAnnotation(AnnotationDescriptionBuilder.create(PathParam.class)
                .withProperty(VALUE, parameter.getName())
                .build());
        }
        return methodParamBuilder.build();
    }

    private void writeWebServiceAnnotations(MethodDescriptionBuilder methodBuilder, PathInfo pathInfo) {
        methodBuilder.addAnnotation(
            AnnotationDescriptionBuilder.create(chooseOperationAnnotation(pathInfo.getOperation())).build());
        methodBuilder.addAnnotation(
            AnnotationDescriptionBuilder.create(Path.class).withProperty(VALUE, pathInfo.getOriginalPath()).build());
        if (StringUtils.isNotBlank(pathInfo.getConsumes())) {
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Consumes.class)
                .withProperty(VALUE, pathInfo.getConsumes(), true)
                .build());
        }
        if (StringUtils.isNotBlank(pathInfo.getProduces())) {
            methodBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Produces.class)
                .withProperty(VALUE, pathInfo.getProduces(), true)
                .build());
        }
    }

    static String resolveType(TypeInfo typeInfo) {
        if (typeInfo.isDatatype()) {
            Class<?> type = DEFAULT_DATATYPE_CLASS;
            if (typeInfo.getDimension() > 0) {
                int[] dimensions = new int[typeInfo.getDimension()];
                type = Array.newInstance(type, dimensions).getClass();
            }
            return type.getName();
        } else {
            return typeInfo.getJavaName();
        }
    }

    private static String removeArray(String type) {
        int idx = type.indexOf('[');
        if (idx > 0) {
            return type.substring(0, idx);
        }
        return type;
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
