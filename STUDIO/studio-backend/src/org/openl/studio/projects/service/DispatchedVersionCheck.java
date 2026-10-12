package org.openl.studio.projects.service;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.message.Severity;
import org.openl.rules.cloner.Cloner;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.common.exception.ConflictException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

@Slf4j
public final class DispatchedVersionCheck {

    private DispatchedVersionCheck() {
    }

    /**
     * The method a request runs and the table version its input is checked against.
     *
     * @param executable the method that runs, or {@code null} when it cannot be resolved
     * @param version    the version the request selects, or {@code null} when it cannot be selected before the run;
     *                   the input is then not checked in advance
     * @param module     the module the method runs in
     */
    public record Selection(@Nullable IOpenMethod executable,
                            @Nullable IOpenMethod version,
                            @Nullable IOpenClass module) {
    }

    /**
     * Selects the method a request runs and the table version it uses.
     *
     * <p>Without runtime context, the requested table URI selects the version. With runtime context, the project's
     * method runs, and the version is the one the context selects, including context properties taken from the
     * arguments.
     */
    public static Selection select(ProjectModel projectModel,
                                   IOpenMethod method,
                                   String uri,
                                   @Nullable IRulesRuntimeContext runtimeContext,
                                   @Nullable Object[] params,
                                   boolean currentOpenedModule) {
        var module = AbstractMethodExecutorService.compiledOpenClass(projectModel, currentOpenedModule)
                .getOpenClassWithErrors();
        if (runtimeContext == null) {
            var version = projectModel.getCurrentDispatcherMethod(method, uri);
            return new Selection(version, version, module);
        }
        try {
            var executable = AbstractMethodExecutorService
                    .resolveMethod(projectModel, method, currentOpenedModule, runtimeContext);
            return new Selection(executable, contextVersion(executable, runtimeContext, params), module);
        } catch (RuntimeException | LinkageError e) {
            log.debug("The method the runtime context runs for table '{}' was not resolved.", uri, e);
            return new Selection(null, null, module);
        }
    }

    /**
     * Refuses a request whose runtime context selects a version of a dispatched table that does not compile.
     *
     * @throws ConflictException when the selected version has compilation errors
     */
    public static void refuseIfBroken(ProjectModel projectModel, Selection selection, boolean currentOpenedModule) {
        var version = selection.version();
        // Only runtime context makes the project's dispatcher run; a table URI selects its own version.
        if (!(selection.executable() instanceof OpenMethodDispatcher) || version == null) {
            return;
        }
        var info = version.getInfo();
        var uri = info == null ? null : info.getSourceUrl();
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

    private static @Nullable IOpenMethod contextVersion(@Nullable IOpenMethod executable,
                                                        IRulesRuntimeContext runtimeContext,
                                                        @Nullable Object[] params) {
        if (!(executable instanceof OpenMethodDispatcher dispatcher)) {
            return executable;
        }
        try {
            var env = new SimpleRulesVM().getRuntimeEnv();
            env.setContext(Cloner.clone(runtimeContext));
            return WrapperLogic.selectDispatchedVersion(dispatcher, params, env);
        } catch (RuntimeException | LinkageError e) {
            log.debug("The runtime context did not select one version of table '{}'.", executable.getName(), e);
            return null;
        }
    }
}
