import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.openl.generated.beans.Driver;
import org.openl.rules.project.instantiation.variation.JXPathVariation;
import org.openl.rules.project.instantiation.variation.NoVariation;
import org.openl.rules.project.instantiation.variation.VariationsPack;
import org.openl.rules.project.instantiation.variation.VariationsResult;
import org.openl.rules.ruleservice.databinding.AegisDatabindingConfigurableFactoryBean;
import org.openl.rules.tutorial4.Tutorial4WithVariations;

public class Client {
    public static final String STANDART = "Standard Driver";
    public static final String YOUNG = "Young Driver";
    public static final String SENOIR = "Senior Driver";

    public static void main(String[] args) throws Exception {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(Tutorial4WithVariations.class);
        AegisDatabindingConfigurableFactoryBean aegisDatabindingConfigurableFactoryBean = new AegisDatabindingConfigurableFactoryBean();
        Set<String> types = new HashSet<String>();
        types.add(JXPathVariation.class.getName());
        aegisDatabindingConfigurableFactoryBean.setOverrideTypes(types);
        aegisDatabindingConfigurableFactoryBean.setWriteXsiTypes(true);
        AegisDatabinding dataBinding = aegisDatabindingConfigurableFactoryBean.getObject();
        // Set<String> types = new HashSet<String>();
        // types.add(JXPathVariation.class.getName());
        // dataBinding.setOverrideTypes(types);
        // dataBinding.getAegisContext().setWriteXsiTypes(true);
        // Set<Type> typess = new HashSet<Type>();
        // typess.add(JXPathVariation.class);
        // dataBinding.getAegisContext().setRootClasses(typess);
        factory.setDataBinding(dataBinding);
        factory.setAddress("http://localhost:8080/openl-ws-5.9.3/tut4");
        Tutorial4WithVariations client = (Tutorial4WithVariations) factory.create();
        Driver driver = new Driver();
        driver.setAge(43);
        driver.setGender("Male");
        VariationsResult<String> resultsDrivers = client.driverAgeType(driver,
            new VariationsPack(new JXPathVariation("young", 0, "age", 18), new JXPathVariation("senior", 0, "age", 71)));
        assertTrue(resultsDrivers.getVariationFailures().isEmpty());
        assertEquals(resultsDrivers.getResultForVariation("young"), YOUNG);
        assertEquals(resultsDrivers.getResultForVariation("senior"), SENOIR);
        assertEquals(resultsDrivers.getResultForVariation(NoVariation.ORIGIANAL_CALCULATION), STANDART);

    }
}
