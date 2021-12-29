package org.openl.rules.ruleservice.loader;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuleServiceLoaderImplTest {

    @Test
    public void testCleanUp() {
        assertTrue(RuleServiceLoaderImpl.cleanUp("a-B.3_").startsWith("a-B.3_"));

        String str1 = RuleServiceLoaderImpl.cleanUp("abc:DEF/345\\ghi?KLM");
        assertTrue(str1.startsWith("abc_DEF_345_ghi_KLM"));

        String str2 = RuleServiceLoaderImpl.cleanUp("abc_DEF:345?ghi/KLM");
        assertTrue(str2.startsWith("abc_DEF_345_ghi_KLM"));

        assertNotEquals(str1, str2);
    }
}
