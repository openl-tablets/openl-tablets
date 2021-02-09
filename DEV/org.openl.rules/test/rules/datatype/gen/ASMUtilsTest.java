package rules.datatype.gen;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.datatype.gen.ASMUtils;

public class ASMUtilsTest {
    @Test
    public void getMethod() {
        Assert.assertNotNull(
            ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "iterator", "()Ljava/util/Iterator;"));
        Assert.assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "toString", "()Ljava/lang/String;"));
        Assert.assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "iterator", "(I)"));
        Assert.assertNotNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "toString", "()Ljava/lang/String;"));
        Assert.assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "()Ljava/lang/Integer;"));
        Assert
            .assertNotNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(I)Ljava/lang/Integer;"));
        Assert.assertNull(
            ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Lnet/Type;)Ljava/lang/Integer;"));
        Assert.assertNotNull(ASMUtils
            .findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Ljava/lang/String;)Ljava/lang/Integer;"));
        Assert.assertNull(ASMUtils
            .findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Ljava/lang/Integer;)Ljava/lang/Integer;"));
    }
}
