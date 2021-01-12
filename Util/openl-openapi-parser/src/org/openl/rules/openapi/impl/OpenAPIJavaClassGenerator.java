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
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.MethodModel;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.RulesType;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancerHelper;
import org.openl.util.StringUtils;

public class OpenAPIJavaClassGenerator {

    private static final String DEFAULT_JSON_TYPE = "application/json";
    private static final String DEFAULT_SIMPLE_TYPE = "text/plain";
    private static final Class<?> DEFAULT_DATATYPE_CLASS = Object.class;
    public static final String VALUE = "value";
    public static final String DEFAULT_OPEN_API_PATH = "org.openl.generated.services";
    public static final String DEFAULT_RUNTIME_CTX_PARAM_NAME = "runtimeContext";

    private final ProjectModel projectModel;

    public OpenAPIJavaClassGenerator(ProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    /**
     * Make decision whatever if we need to decorate this method or not
     * @param method candidate
     * @return {@code true} if require decoration
     */
    private boolean generateDecision(MethodModel method) {
        if (!method.isInclude()) {
            return false;
        }
        final PathInfo pathInfo = method.getPathInfo();
        StringBuilder sb = new StringBuilder("/" + pathInfo.getFormattedPath());
        method.getParameters().stream().filter(InputParameter::isInPath).map(InputParameter::getName)
                .forEach(name -> sb.append("/{").append(name).append('}'));
        if (!pathInfo.getOriginalPath().equals(sb.toString())) {
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
                if (!parameters.isEmpty() && !DEFAULT_RUNTIME_CTX_PARAM_NAME.equals(pathInfo.getRuntimeContextParameter().getName())) {
                    //if runtimeContext param name is not default
                    return true;
                }
            } else if (parameters.isEmpty()) {
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
            if (parameters.size() > JAXRSOpenLServiceEnhancerHelper.MAX_PARAMETERS_COUNT_FOR_GET) {
                //if more than 3 parameters, POST by default.
                return true;
            } else if (!parameters.stream().allMatch(p -> p.getType().getType() == TypeInfo.Type.PRIMITIVE)) {
                //if there is at least one non-primitive parameter, POST by default.
                return true;
            }
        } else if (HttpMethod.POST.equalsIgnoreCase(pathInfo.getOperation())) {
            if (!projectModel.isRuntimeContextProvided()) {
                if (parameters.isEmpty()) {
                    //if no context and empty params, GET by default.
                    return true;
                } else if (parameters.size() <= JAXRSOpenLServiceEnhancerHelper.MAX_PARAMETERS_COUNT_FOR_GET
                        && parameters.stream().allMatch(p -> p.getType().getType() == TypeInfo.Type.PRIMITIVE)) {
                    //if no context and if there are less than 3 parameters and they are all primitive, GET by default.
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
            .create(DEFAULT_OPEN_API_PATH, "Service");
        boolean hasMethods = false;
        for (MethodModel method : projectModel.getSpreadsheetResultModels()) {
            if (!generateDecision(method)) {
                continue;
            }
            javaInterfaceBuilder.addAbstractMethod(visitInterfaceMethod(method, false).build());
            hasMethods = true;
        }
        for (MethodModel method : projectModel.getDataModels()) {
            if (!generateDecision(method)) {
                continue;
            }
            javaInterfaceBuilder.addAbstractMethod(visitInterfaceMethod(method, false).build());
            hasMethods = true;
        }
        OpenAPIGeneratedClasses.Builder builder = OpenAPIGeneratedClasses.Builder.initialize();
        for (MethodModel extraMethod : projectModel.getNotOpenLModels()) {
            hasMethods = true;
            JavaInterfaceImplBuilder extraMethodBuilder = new JavaInterfaceImplBuilder(ServiceExtraMethodHandler.class, DEFAULT_OPEN_API_PATH);
            JavaClassFile javaClassFile = new JavaClassFile(extraMethodBuilder.getBeanName(),
                extraMethodBuilder.byteCode());
            builder.addCommonClass(javaClassFile);
            MethodDescriptionBuilder methodDesc = visitInterfaceMethod(extraMethod, true);
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

    private MethodDescriptionBuilder visitInterfaceMethod(MethodModel sprModel, boolean extraMethod) {

        final PathInfo pathInfo = sprModel.getPathInfo();
        final TypeInfo returnTypeInfo = pathInfo.getReturnType();
        MethodDescriptionBuilder methodBuilder = MethodDescriptionBuilder.create(pathInfo.getFormattedPath(),
            resolveType(returnTypeInfo));

        InputParameter runtimeCtxParam = sprModel.getPathInfo().getRuntimeContextParameter();
        if (runtimeCtxParam != null) {
            MethodParameterBuilder ctxBuilder = MethodParameterBuilder.create(runtimeCtxParam.getType().getJavaName());
            final String paramName = runtimeCtxParam.getName();
            if (sprModel.getParameters().size() > 0 && !DEFAULT_RUNTIME_CTX_PARAM_NAME.equals(paramName)) {
                ctxBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Name.class)
                        .withProperty(VALUE, paramName)
                        .build());
            }
            methodBuilder.addParameter(ctxBuilder.build());
        }

        for (InputParameter parameter : sprModel.getParameters()) {
            methodBuilder.addParameter(visitMethodParameter(parameter, extraMethod));
        }

        if (returnTypeInfo.getType() == TypeInfo.Type.DATATYPE) {
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
        if (paramType.getType() == TypeInfo.Type.DATATYPE) {
            methodParamBuilder.addAnnotation(AnnotationDescriptionBuilder.create(RulesType.class)
                .withProperty(VALUE, OpenAPITypeUtils.removeArrayBrackets(paramType.getSimpleName()))
                .build());
        }
        if (extraMethod) {
            methodParamBuilder.addAnnotation(AnnotationDescriptionBuilder.create(Name.class)
                    .withProperty(VALUE, parameter.getName())
                    .build());
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
        if (typeInfo.getType() == TypeInfo.Type.DATATYPE) {
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
