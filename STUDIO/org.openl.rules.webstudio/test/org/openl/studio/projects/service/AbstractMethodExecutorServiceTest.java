package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.CompiledOpenClass;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

@ExtendWith(MockitoExtension.class)
class AbstractMethodExecutorServiceTest {

    private static final String TABLE_URI = "file://test.xlsx#Sheet1!A1";
    private static final String METHOD_NAME = "testMethod";

    @Mock
    private ProjectModel projectModel;

    @Mock
    private IOpenLTable table;

    @Mock
    private ExecutionProgressListener listener;

    private AbstractMethodExecutorService service;

    @BeforeEach
    void setUp() {
        service = mock(AbstractMethodExecutorService.class, CALLS_REAL_METHODS);
    }

    private void stubTableUri() {
        when(table.getUri()).thenReturn(TABLE_URI);
    }

    // --- resolveMethod ---

    @Test
    void resolveMethod_fromCurrentModule() {
        stubTableUri();
        var method = mock(IOpenMethod.class);
        when(projectModel.getOpenedModuleMethod(TABLE_URI)).thenReturn(method);

        var result = AbstractMethodExecutorService.resolveMethod(projectModel, table, true, null);

        assertSame(method, result);
    }

    @Test
    void resolveMethod_fromProject() {
        stubTableUri();
        var method = mock(IOpenMethod.class);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);

        var result = AbstractMethodExecutorService.resolveMethod(projectModel, table, false, null);

        assertSame(method, result);
    }

    @Test
    void resolveMethod_dispatcher() {
        stubTableUri();
        var dispatcher = mock(OpenMethodDispatcher.class);
        var resolved = mock(IOpenMethod.class);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(dispatcher);
        when(projectModel.getCurrentDispatcherMethod(dispatcher, TABLE_URI)).thenReturn(resolved);

        var result = AbstractMethodExecutorService.resolveMethod(projectModel, table, false, null);

        assertSame(resolved, result);
    }

    @Test
    void resolveMethod_dispatcherUnresolved() {
        stubTableUri();
        var dispatcher = mock(OpenMethodDispatcher.class);
        when(projectModel.getMethod(TABLE_URI)).thenReturn(dispatcher);
        when(projectModel.getCurrentDispatcherMethod(dispatcher, TABLE_URI)).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> AbstractMethodExecutorService.resolveMethod(projectModel, table, false, null));
    }

    @Test
    void resolveMethod_withRuntimeContext_fromProject() {
        stubTableUri();
        var method = mock(IOpenMethod.class);
        var runtimeContext = mock(IRulesRuntimeContext.class);
        var compiledOpenClass = mock(CompiledOpenClass.class);
        var openClass = mock(IOpenClass.class);
        var projectMethod = mock(IOpenMethod.class);
        var signature = mock(IMethodSignature.class);
        var paramTypes = IOpenClass.EMPTY;

        when(projectModel.getMethod(TABLE_URI)).thenReturn(method);
        when(method.getName()).thenReturn(METHOD_NAME);
        when(method.getSignature()).thenReturn(signature);
        when(signature.getParameterTypes()).thenReturn(paramTypes);
        when(projectModel.getCompiledOpenClass()).thenReturn(compiledOpenClass);
        when(compiledOpenClass.getOpenClassWithErrors()).thenReturn(openClass);
        when(openClass.getMethod(METHOD_NAME, paramTypes)).thenReturn(projectMethod);

        var result = AbstractMethodExecutorService.resolveMethod(projectModel, table, false, runtimeContext);

        assertSame(projectMethod, result);
    }

    @Test
    void resolveMethod_withRuntimeContext_fromCurrentModule() {
        stubTableUri();
        var method = mock(IOpenMethod.class);
        var runtimeContext = mock(IRulesRuntimeContext.class);
        var compiledOpenClass = mock(CompiledOpenClass.class);
        var openClass = mock(IOpenClass.class);
        var projectMethod = mock(IOpenMethod.class);
        var signature = mock(IMethodSignature.class);
        var paramTypes = IOpenClass.EMPTY;

        when(projectModel.getOpenedModuleMethod(TABLE_URI)).thenReturn(method);
        when(method.getName()).thenReturn(METHOD_NAME);
        when(method.getSignature()).thenReturn(signature);
        when(signature.getParameterTypes()).thenReturn(paramTypes);
        when(projectModel.getOpenedModuleCompiledOpenClass()).thenReturn(compiledOpenClass);
        when(compiledOpenClass.getOpenClassWithErrors()).thenReturn(openClass);
        when(openClass.getMethod(METHOD_NAME, paramTypes)).thenReturn(projectMethod);

        var result = AbstractMethodExecutorService.resolveMethod(projectModel, table, true, runtimeContext);

        assertSame(projectMethod, result);
    }

    // --- getDb ---

    @Test
    void getDb_nullProjectModel() {
        assertNull(AbstractMethodExecutorService.getDb(null, false));
    }

    @Test
    void getDb_xlsModuleOpenClass() {
        var compiledOpenClass = mock(CompiledOpenClass.class);
        var xlsModule = mock(XlsModuleOpenClass.class);
        var dataBase = mock(IDataBase.class);

        when(projectModel.getCompiledOpenClass()).thenReturn(compiledOpenClass);
        when(compiledOpenClass.getOpenClassWithErrors()).thenReturn(xlsModule);
        when(xlsModule.getDataBase()).thenReturn(dataBase);

        assertSame(dataBase, AbstractMethodExecutorService.getDb(projectModel, false));
    }

    @Test
    void getDb_xlsModuleOpenClass_currentModule() {
        var compiledOpenClass = mock(CompiledOpenClass.class);
        var xlsModule = mock(XlsModuleOpenClass.class);
        var dataBase = mock(IDataBase.class);

        when(projectModel.getOpenedModuleCompiledOpenClass()).thenReturn(compiledOpenClass);
        when(compiledOpenClass.getOpenClassWithErrors()).thenReturn(xlsModule);
        when(xlsModule.getDataBase()).thenReturn(dataBase);

        assertSame(dataBase, AbstractMethodExecutorService.getDb(projectModel, true));
    }

    @Test
    void getDb_nonXlsModuleOpenClass() {
        var compiledOpenClass = mock(CompiledOpenClass.class);
        var openClass = mock(IOpenClass.class);

        when(projectModel.getCompiledOpenClass()).thenReturn(compiledOpenClass);
        when(compiledOpenClass.getOpenClassWithErrors()).thenReturn(openClass);

        assertNull(AbstractMethodExecutorService.getDb(projectModel, false));
    }

    // --- executeWithLifecycle ---

    @Test
    void executeWithLifecycle_success() throws Exception {
        var result = "OK";
        var future = service.executeWithLifecycle(listener, () -> result);

        assertEquals(result, future.get());

        var order = inOrder(listener);
        order.verify(listener).onStatusChanged(ExecutionStatus.STARTED);
        order.verify(listener).onStatusChanged(ExecutionStatus.COMPLETED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void executeWithLifecycle_exception() {
        var error = new RuntimeException("test error");
        var future = service.executeWithLifecycle(listener, () -> {
            throw error;
        });

        assertTrue(future.isCompletedExceptionally());

        var order = inOrder(listener);
        order.verify(listener).onStatusChanged(ExecutionStatus.STARTED);
        order.verify(listener).onError("test error", error);
        order.verifyNoMoreInteractions();
    }

    @Test
    void executeWithLifecycle_interruptedException() throws Exception {
        var future = service.executeWithLifecycle(listener, () -> {
            throw new InterruptedException("interrupted");
        });

        assertNull(future.get());

        var order = inOrder(listener);
        order.verify(listener).onStatusChanged(ExecutionStatus.STARTED);
        order.verify(listener).onStatusChanged(ExecutionStatus.INTERRUPTED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void executeWithLifecycle_wrappedInterruptedException() throws Exception {
        var cause = new InterruptedException("interrupted");
        var wrapper = new RuntimeException("wrapped", cause);

        var future = service.executeWithLifecycle(listener, () -> {
            throw wrapper;
        });

        assertNull(future.get());

        var order = inOrder(listener);
        order.verify(listener).onStatusChanged(ExecutionStatus.STARTED);
        order.verify(listener).onStatusChanged(ExecutionStatus.INTERRUPTED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void executeWithLifecycle_threadInterrupted() throws Exception {
        var future = service.executeWithLifecycle(listener, () -> {
            Thread.currentThread().interrupt();
            return "partial";
        });

        assertEquals("partial", future.get());

        var order = inOrder(listener);
        order.verify(listener).onStatusChanged(ExecutionStatus.STARTED);
        order.verify(listener).onStatusChanged(ExecutionStatus.INTERRUPTED);
        order.verifyNoMoreInteractions();

        // Clear interrupt flag
        assertTrue(Thread.interrupted());
    }

    @Test
    void executeWithLifecycle_failedFuture() {
        var error = new IllegalArgumentException("bad input");
        var future = service.executeWithLifecycle(listener, () -> {
            throw error;
        });

        var ex = assertThrows(ExecutionException.class, future::get);
        assertSame(error, ex.getCause());
    }

    // --- isInterruptedException ---

    @Test
    void isInterruptedException_direct() {
        assertTrue(AbstractMethodExecutorService.isInterruptedException(new InterruptedException()));
    }

    @Test
    void isInterruptedException_wrapped() {
        assertTrue(AbstractMethodExecutorService.isInterruptedException(
                new RuntimeException(new InterruptedException())));
    }

    @Test
    void isInterruptedException_deeplyWrapped() {
        assertTrue(AbstractMethodExecutorService.isInterruptedException(
                new RuntimeException(new IllegalStateException(new InterruptedException()))));
    }

    @Test
    void isInterruptedException_notInterrupted() {
        assertFalse(AbstractMethodExecutorService.isInterruptedException(new RuntimeException("error")));
    }

    @Test
    void isInterruptedException_nullCause() {
        assertFalse(AbstractMethodExecutorService.isInterruptedException(new RuntimeException()));
    }
}
