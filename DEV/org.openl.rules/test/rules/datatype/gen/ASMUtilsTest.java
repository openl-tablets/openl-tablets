package rules.datatype.gen;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.datatype.gen.ASMUtils;

public class ASMUtilsTest {
    @Test
    public void getMethod() {
        Assert.assertNotNull(ASMUtils.getMethod(Iterable.class, "iterator", "()"));
        Assert.assertNull(ASMUtils.getMethod(Iterable.class, "toString", "()"));
        Assert.assertNull(ASMUtils.getMethod(Iterable.class, "iterator", "(I)"));
        Assert.assertNotNull(ASMUtils.getMethod(Integer.class, "toString", "()"));
        Assert.assertNull(ASMUtils.getMethod(Integer.class, "valueOf", "()"));
        Assert.assertNotNull(ASMUtils.getMethod(Integer.class, "valueOf", "(I)"));
        Assert.assertNull(ASMUtils.getMethod(Integer.class, "valueOf", "(Lnet/Type;)"));
        Assert.assertNotNull(ASMUtils.getMethod(Integer.class, "valueOf", "(Ljava/lang/String;)"));
        Assert.assertNull(ASMUtils.getMethod(Integer.class, "valueOf", "(Ljava/lang/Integer;)"));
    }
}
