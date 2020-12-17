package org.openl.rules.openapi.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
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

    private static final String DEFAULT_JSON_TYPE = "application/json";
    private static final String DEFAULT_SIMPLE_TYPE = "text/plain";
    private static final Class<?> DEFAULT_DATATYPE_CLASS = Object.class;
    private static final String RULES_CTX_CLASS = IRulesRuntimeContext.class.getName();
    public static final String VALUE = "value";

    private final ProjectModel projectModel;

    public OpenAPIJavaClassGenerator(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    /**
     * Make decision whatever if we need to decorate this method or not
     * @param method candidate
     * @return {@code true} if require decoration
     */
    private boolean generateDecision(SpreadsheetModel method) {
        final PathInfo pathInfo = method.getPathInfo();
        if (!pathInfo.getOriginalPath().equals("/" + pathInfo.getFormattedPath())) {
            //if method name doesn't match expected path
            return true;
        }
        if (StringUtils.isNotBlank(pathInfo.getProduces())) {
            final TypeInfo typeInfo = pathInfo.getReturnType();
            if (typeInfo.isReference()) {
                if (!DEFAULT_JSON_TYPE.equals(pathInfo.getProduces())) {
                    //if return type is not simple, application/json by default
                    return true;
                }
            } else if (!DEFAULT_SIMPLE_TYPE.equals(pathInfo.getProduces())) {
                //if return type is simple, text/plain by default
                return true;
            }
        }
        final List<InputParameter> parameters = method.getParameters();
        if (StringUtils.isNotBlank(pathInfo.getConsumes())) {
            if (projectModel.isRuntimeContextProvided()) {
                if (!DEFAULT_JSON_TYPE.equals(pathInfo.getConsumes())) {
                    //if context, application/json by default
                    return true;
                }
            }else if (parameters.isEmpty()) {
                if (!DEFAULT_SIMPLE_TYPE.equals(pathInfo.getConsumes())) {
                    //if no prams, text/plan by default
                    return true;
                }
            } else {
                if (parameters.size() == 1) {
                    if (parameters.get(0).getType().isReference()) {
                        if (!DEFAULT_JSON_TYPE.equals(pathInfo.getConsumes())) {
                            //if one not simple param, application/json by default
                            return true;
                        }
                    } else if (!DEFAULT_SIMPLE_TYPE.equals(pathInfo.getConsumes())) {
                        //if one simple pram, text/plain by default
                        return true;
                    }
                } else if (!DEFAULT_JSON_TYPE.equals(pathInfo.getConsumes())) {
                    //if more than one param, application/json by default
                    return true;
                }
            }
        }
        if (HttpMethod.GET.equalsIgnoreCase(pathInfo.getOperation())) {
            if (projectModel.isRuntimeContextProvided()) {
                //if RuntimeContext is provided, POST by default.
                return true;
            }
            if (parameters.size() > 1) {
                //if more than one parameter, POST by default.
                return true;
            } else if (parameters.size() == 1 && parameters.get(0).getType().isReference()) {
                //if there is one not simple parameter, POST by default.
                return true;
            }
        } else if (HttpMethod.POST.equalsIgnoreCase(pathInfo.getOperation())) {
            if (!projectModel.isRuntimeContextProvided()) {
                if (parameters.isEmpty()) {
                    //if no context and empty params, GET by default.
                    return true;
                } else if (parameters.size() == 1 && !parameters.get(0).getType().isReference()) {
                    //if no context and there is one simple parameter, GET by default.
                    return true;
                }
            }
        } else {
            //if not POST and not GET
            return true;
        }
        return false;
    }

    public OpenAPIGeneratedClasses generate() {
        JavaInterfaceByteCodeBuilder javaInterfaceBuilder = JavaInterfaceByteCodeBuilder
            .createWithDefaultPackage("OpenAPIService");
        boolean hasMethods = false;
        for (SpreadsheetModel method : projectModel.getSpreadsheetResultModels()) {
            if (!generateDecision(method)) {
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
                .withProperty(VALUE, OpenAPITypeUtils.removeArrayBrackets(returnTypeInfo.getSimpleName()))
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
                .withProperty(VALUE, OpenAPITypeUtils.removeArrayBrackets(paramType.getSimpleName()))
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
