package org.openl.rules.lang.xls;

import java.io.File;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class TablePartsTest {

    public interface ITestI {
        String bigLookup(String code, int amt);

        BigData[] getData();
    }

    @Test
    public void test() throws Exception {
        SimpleProjectEngineFactoryBuilder<ITestI> factoryBuilder = new SimpleProjectEngineFactoryBuilder<ITestI>();
        SimpleProjectEngineFactory<ITestI> factory = factoryBuilder.setProject("src/main/openl")
                .setInterfaceClass(ITestI.class)
                .build();
        ITestI instance = factory.newInstance();
        String s = instance.bigLookup("A1C", 1000);
        assertEquals("A1C1000", s);

        assertEquals("A60000X4000", instance.bigLookup("A60000X", 4000));
        assertEquals("A65530X3000", instance.bigLookup("A65530X", 3000));

        assertEquals("A70000X4000", instance.bigLookup("A70000X", 4000));
        assertEquals("A135529X4000", instance.bigLookup("A135529X", 4000));

        assertEquals("A150000X2000", instance.bigLookup("A150000X", 2000));

        BigData[] bg = instance.getData();
        assertEquals("sd1000x0", bg[1].sd0.x0);
        assertEquals("sd3025x3", bg[3].sd25.x3);
        assertEquals("sd3099x9", bg[3].sd99.x9);

        assertEquals("sd77x0", bg[0].sd77.x0);

        assertEquals("sd1076x3", bg[1].sd76.x3);

    }

}
