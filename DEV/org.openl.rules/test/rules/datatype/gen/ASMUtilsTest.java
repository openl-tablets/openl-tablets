package rules.datatype.gen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.datatype.gen.ASMUtils;

public class ASMUtilsTest {
    @Test
    public void getMethod() {
        assertNotNull(
                ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "iterator", "()Ljava/util/Iterator;"));
        assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "toString", "()Ljava/lang/String;"));
        assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Iterable.class), "iterator", "(I)"));
        assertNotNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "toString", "()Ljava/lang/String;"));
        assertNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "()Ljava/lang/Integer;"));
        assertNotNull(ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(I)Ljava/lang/Integer;"));
        assertNull(
                ASMUtils.findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Lnet/Type;)Ljava/lang/Integer;"));
        assertNotNull(ASMUtils
                .findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Ljava/lang/String;)Ljava/lang/Integer;"));
        assertNull(ASMUtils
                .findMethod(ASMUtils.buildMap(Integer.class), "valueOf", "(Ljava/lang/Integer;)Ljava/lang/Integer;"));
    }
}
