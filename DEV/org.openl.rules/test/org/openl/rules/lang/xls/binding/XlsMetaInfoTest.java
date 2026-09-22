package org.openl.rules.lang.xls.binding;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

class XlsMetaInfoTest {

    @Test
    void aModuleWithoutSourceHasNoUrl() {
        var moduleNode = new XlsModuleSyntaxNode(new WorkbookSyntaxNode[0], null, List.of());

        assertNull(new XlsMetaInfo(moduleNode).getSourceUrl());
    }
}
