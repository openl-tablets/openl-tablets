/**
 * Created Oct 25, 2005
 */
package org.openl.conf.ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openl.main.OpenLWrapper;
import org.openl.rules.context.IRulesRuntimeContextConsumer;
import org.openl.rules.context.IRulesRuntimeContextProvider;

/**
 * @author
 * 
 */
public class JavaWrapperAntTask extends JavaAntTask {

    private String[] implementsInterfaces = new String[] { OpenLWrapper.class.getName(),
            IRulesRuntimeContextProvider.class.getName(), IRulesRuntimeContextConsumer.class.getName() };

    public String[] getImplementsInterfaces() {
        return implementsInterfaces;
    }

    public void setImplementsInterfaces(String[] implementsInterfaces) {
        List<String> interfaces = new ArrayList<String>(Arrays.asList(this.implementsInterfaces));
        interfaces.addAll(Arrays.asList(implementsInterfaces));
        this.implementsInterfaces = interfaces.toArray(new String[interfaces.size()]);
    }

    @Override
    protected void writeSpecific() {
        // Do nothing
    }

    @Override
    protected OpenLToJavaGenerator getJavaGenerator() {
        return new JavaWrapperGenerator(getOpenClass(), getTargetClass(), getExtendsClass(), getImplementsInterfaces(),
                getOpenlName(), getDeplSrcFile(), getSrcFile(), getSrcModuleClass(), getUserHome(), getDeplUserHome(),
                getRulesFolder(), getFields(), getMethods(), isIgnoreNonJavaTypes());
    }
}
