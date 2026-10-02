package org.openl.studio.projects.service;

import java.util.Objects;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.message.Severity;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.common.exception.ConflictException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.MethodSignature;

@Slf4j
public final class DispatchedVersionCheck {

    private DispatchedVersionCheck() {
    }

    public static void refuseIfBroken(ProjectModel projectModel,
                                      IOpenLTable table,
                                      @Nullable IRulesRuntimeContext runtimeContext,
                                      boolean currentOpenedModule) {
        if (runtimeContext == null) {
            return;
        }
        var uri = selectedVersionUri(projectModel, table, runtimeContext, currentOpenedModule);
        if (uri == null || projectModel.getTableByUri(uri) == null) {
            return;
        }
        var errors = currentOpenedModule
                ? projectModel.getOpenedModuleMessagesByTsn(uri, Severity.ERROR)
                : projectModel.getMessagesByTsn(uri, Severity.ERROR);
        if (!errors.isEmpty()) {
            var summary = Objects.toString(errors.getFirst().getSummary(), "").lines().findFirst().orElse("");
            throw new ConflictException("run.dispatched.compile.errors.message", summary);
        }
    }

    private static @Nullable String selectedVersionUri(ProjectModel projectModel,
                                                       IOpenLTable table,
                                                       IRulesRuntimeContext runtimeContext,
                                                       boolean currentOpenedModule) {
        try {
            var method = AbstractMethodExecutorService
                    .resolveMethod(projectModel, table, currentOpenedModule, runtimeContext);
            if (!(method instanceof OpenMethodDispatcher dispatcher) || injectsContextProperties(dispatcher)) {
                return null;
            }
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setContext(Cloner.clone(runtimeContext));
            var info = dispatcher.findMatchingMethod(env).getInfo();
            return info == null ? null : info.getSourceUrl();
        } catch (RuntimeException | LinkageError e) {
            log.debug("The version the runtime context selects for table '{}' was not resolved.", table.getUri(), e);
            return null;
        }
    }

    private static boolean injectsContextProperties(OpenMethodDispatcher dispatcher) {
        return dispatcher.getCandidates()
                .stream()
                .anyMatch(candidate -> injectsContextProperties(candidate.getSignature()));
    }

    private static boolean injectsContextProperties(IMethodSignature signature) {
        return IntStream.range(0, signature.getNumberOfParameters())
                .anyMatch(i -> declaresContextProperty(signature, i)
                        || hasContextPropertyField(signature.getParameterType(i)));
    }

    private static boolean declaresContextProperty(IMethodSignature signature, int parameterIndex) {
        return signature instanceof MethodSignature methodSignature
                && methodSignature.getParameterDeclaration(parameterIndex).getContextProperty() != null;
    }

    private static boolean hasContextPropertyField(IOpenClass type) {
        return type.getFields().stream().anyMatch(IOpenField::isContextProperty);
    }
}
