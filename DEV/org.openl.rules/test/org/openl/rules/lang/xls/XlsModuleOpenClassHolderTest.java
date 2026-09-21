package org.openl.rules.lang.xls;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;

class XlsModuleOpenClassHolderTest {

    private final XlsModuleOpenClassHolder holder = XlsModuleOpenClassHolder.getInstance();

    @AfterEach
    void unbind() {
        holder.setXlsModuleOpenClass(null);
    }

    @Test
    void keepsTheModuleBoundToTheCurrentThread() {
        var module = mock(XlsModuleOpenClass.class);

        holder.setXlsModuleOpenClass(module);

        assertSame(module, holder.getXlsModuleOpenClass());
    }

    @Test
    void unbindsTheModuleWhenNullIsPassed() {
        holder.setXlsModuleOpenClass(mock(XlsModuleOpenClass.class));

        holder.setXlsModuleOpenClass(null);

        assertNull(holder.getXlsModuleOpenClass());
    }

    @Test
    void answersWithNullWhenNothingIsBound() {
        assertNull(holder.getXlsModuleOpenClass());
    }
}
