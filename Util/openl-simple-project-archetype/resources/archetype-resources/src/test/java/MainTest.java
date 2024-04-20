#set($symbol_pound='#')
#set($symbol_dollar='$')
#set($symbol_escape='\' )
package ${package};

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public class MainTest {

    @Test
    public void test() throws Exception {
        SimpleProjectEngineFactoryBuilder<Service> factoryBuilder = new SimpleProjectEngineFactoryBuilder<Service>();
        SimpleProjectEngineFactory<Service> factory = factoryBuilder.setProject("src/main/openl")
                .setInterfaceClass(Service.class)
                .build();

        Service instance = factory.newInstance();
        String result = instance.hello(10);
        assertEquals("Good Morning", result);
    }
}
