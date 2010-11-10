package org.openl.mapper;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.openl.mapper.model.A;
import org.openl.mapper.model.C;
import org.openl.mapper.model.E;
import org.openl.mapper.model.F;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class RulesBeanMapperTest {

    @Test
    public void testMapper() {

        File source = new File("src/test/resources/org/openl/mapper/RulesBeanMapperTest.xlsx");
        ProjectDescriptor project = new ProjectDescriptor();
        project.setProjectFolder(source.getParentFile());
        project.setName("RulesBeanMapperTest");
        project.setId("RulesBeanMapperTest");

        Module module = new Module();
        module.setProject(project);
        module.setName("RulesBeanMapperTest");
        module.setRulesRootPath(new PathEntry(source.getAbsolutePath()));
        module.setType(ModuleType.API);

        RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module);

        RulesBeanMapper mapper = new RulesBeanMapper(instantiationStrategy);

        A a = new A();
        a.setA("string");
        a.setB(10);
        a.setX(new String[] { "x", null, "y" });

        C c = mapper.map(a, C.class);

        A a_ = new A();
        mapper.map(c, a_);

        F f = new F();
        f.setA(a);

        E e = mapper.map(f, E.class);

        assertEquals(10, e.getD().getI());
    }

}
