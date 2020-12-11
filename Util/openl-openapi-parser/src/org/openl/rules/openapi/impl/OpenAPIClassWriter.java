package org.openl.rules.openapi.impl;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.TypeDescription;
import org.openl.rules.model.scaffolding.InputParameter;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.util.CollectionUtils;

/**
 * This is not junit test, it's only for the testing the results of the generation.
 * This class functionality must be used int the OpenAPIProjectCreator for the generation and RepositoryTreeController in case of the regeneration.
 */
public class OpenAPIClassWriter extends ClassVisitor {

    private final ProjectModel projectModel;

    public OpenAPIClassWriter(ClassVisitor delegatedClassVisitor, ProjectModel projectModel) {
        super(Opcodes.ASM5, delegatedClassVisitor);
        this.projectModel = projectModel;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (CollectionUtils.isNotEmpty(projectModel.getSpreadsheetResultModels())) {
            // there is a need to append RuntimeContext for the input parameters
            writeOpenAPIInterface(projectModel.getSpreadsheetResultModels());
        }

        if (CollectionUtils.isNotEmpty(projectModel.getNotOpenLModels())) {
            writeOpenAPIInterface(projectModel.getNotOpenLModels());
        }
    }

    private void writeOpenAPIInterface(List<SpreadsheetModel> spreadsheetResultModels) {
        for (SpreadsheetModel sprModel : spreadsheetResultModels) {
            PathInfo pathInfo = sprModel.getPathInfo();
            if (pathInfo.getFormattedPath().equals("/" + pathInfo.getOriginalPath())) {
                continue;
            }
            MethodVisitor methodVisitor = cv.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                pathInfo.getFormattedPath(),
                buildMethodDescriptor(pathInfo.getReturnType(), sprModel.getParameters()),
                null,
                null);
            AnnotationVisitor av = methodVisitor
                .visitAnnotation(Type.getDescriptor(findOperation(pathInfo.getOperation())), true);
            av.visitEnd();

            AnnotationVisitor av1 = methodVisitor.visitAnnotation(Type.getDescriptor(Path.class), true);
            av1.visit("value", pathInfo.getOriginalPath());
            av1.visitEnd();

            AnnotationVisitor consumesVisitor = methodVisitor.visitAnnotation(Type.getDescriptor(Consumes.class), true);
            consumesVisitor.visit("value", pathInfo.getConsumes());
            consumesVisitor.visitEnd();

            AnnotationVisitor producesVisitor = methodVisitor.visitAnnotation(Type.getDescriptor(Produces.class), true);
            producesVisitor.visit("value", pathInfo.getProduces());
            producesVisitor.visitEnd();
        }
    }

    private Class<? extends Annotation> findOperation(String operationName) {
        Class<? extends Annotation> result = GET.class;
        List<Class<? extends Annotation>> classes = Arrays
            .asList(POST.class, GET.class, PUT.class, DELETE.class, PATCH.class);
        for (Class<? extends Annotation> clazz : classes) {
            HttpMethod[] httpMethods = clazz.getAnnotationsByType(HttpMethod.class);
            if (CollectionUtils.isNotEmpty(httpMethods)) {
                HttpMethod method = Arrays.stream(httpMethods).iterator().next();
                if (method.value().equalsIgnoreCase(operationName)) {
                    result = clazz;
                    break;
                }
            }
        }
        return result;
    }

    private String buildMethodDescriptor(String returnType, List<InputParameter> parameters) {
        StringBuilder builder = new StringBuilder("(");
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (InputParameter parameter : parameters) {
                String td = new TypeDescription(parameter.getType()).getTypeDescriptor();
                builder.append(td);
            }
        }
        builder.append(')').append(new TypeDescription(returnType).getTypeDescriptor());
        return builder.toString();
    }
}
