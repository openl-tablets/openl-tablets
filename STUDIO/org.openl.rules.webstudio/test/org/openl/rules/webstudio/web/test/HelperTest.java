package org.openl.rules.webstudio.web.test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.StubSpreadSheetResult;
import org.richfaces.model.TreeNode;

public class HelperTest {

    @Test
    public void testGetRoot() {
        Helper helper = new Helper();
        SimpleParameterTreeNode parameter = new SimpleParameterTreeNode("FN", 123, null, null);
        TreeNode root = helper.getRoot(parameter);
        TreeNode child = root.getChild("FN");
        assertSame(parameter, child);
    }

    @Test
    public void testFormat() {
        Helper helper = new Helper();
        assertEquals("null", helper.format(null));
        assertEquals("Str", helper.format("Str"));
        assertEquals("1", helper.format(1));
        assertEquals("0.1", helper.format(0.1));
        assertEquals("true", helper.format(true));
        assertEquals("foo,bar", helper.format(new String[] { "foo", "bar" }));
    }

    @Test
    public void testIsExplanationValue() {
        Helper helper = new Helper();
        assertTrue(helper.isExplanationValue(DoubleValue.ONE));
        assertFalse(helper.isExplanationValue(null));
        assertFalse(helper.isExplanationValue("Str"));
    }

    @Test
    public void testIsSpreadsheetResult() {
        Helper helper = new Helper();
        assertTrue(helper.isSpreadsheetResult(new StubSpreadSheetResult()));
        assertFalse(helper.isSpreadsheetResult(null));
        assertFalse(helper.isSpreadsheetResult("Str"));
    }

    @Test
    public void testGetExplanatorId() {
        FacesContext context = ContextMocker.mockFacesContext();
        try {
            Map<String, Object> session = new HashMap<>();
            ExternalContext ext = mock(ExternalContext.class);
            when(ext.getSessionMap()).thenReturn(session);
            when(context.getExternalContext()).thenReturn(ext);
            String requestId = "id3453209";

            Helper helper = new Helper();
            int id1 = helper.getExplanatorId(requestId, DoubleValue.ONE);
            int id2 = helper.getExplanatorId(requestId, DoubleValue.ZERO);
            int id1r = helper.getExplanatorId(requestId, DoubleValue.ONE);
            int id2r = helper.getExplanatorId(requestId, DoubleValue.ZERO);
            assertEquals(id1r, id1);
            assertEquals(id2r, id2);
            assertNotEquals(id1r, id2);
            assertNotEquals(id2r, id1);

        } finally {
            context.release();
        }

    }
}

abstract class ContextMocker extends FacesContext {
    private ContextMocker() {
    }

    private static final Release RELEASE = new Release();

    private static class Release implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            setCurrentInstance(null);
            return null;
        }
    }

    public static FacesContext mockFacesContext() {
        FacesContext context = mock(FacesContext.class);
        setCurrentInstance(context);
        doAnswer(RELEASE).when(context).release();
        return context;
    }
}
