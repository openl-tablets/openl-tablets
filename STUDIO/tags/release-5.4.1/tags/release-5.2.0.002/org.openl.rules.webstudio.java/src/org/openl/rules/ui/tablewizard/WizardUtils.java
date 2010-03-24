package org.openl.rules.ui.tablewizard;

import java.util.regex.Pattern;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.openl.meta.IMetaInfo;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;

/**
 * @author Aliaksandr Antonik.
 */
public class WizardUtils {
    private static final Pattern REGEXP_PARAMETER = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");

    /**
     * Checks a string to be a valid parameter name
     *
     * @param s String to check, must not be <code>null</code>
     * @return if <code>s</code> is a valid parameter name.
     */
    public static boolean isValidParameter(String s) {
        return REGEXP_PARAMETER.matcher(s).matches();
    }

    public static void autoRename(Collection<? extends TableArtifact> conditions, String prefix) {
        int i = 0;
        for (TableArtifact c : conditions)
            c.setName(prefix + ++i);
    }

    public static String checkParameterName(String name) {
        if (StringUtils.isEmpty(name))
            return "Parameter name can not be empty";

        if (!isValidParameter(name))
            return "Invalid name for parameter";

        return null;
    }

    public static XlsMetaInfo getMetaInfo() {
        return (XlsMetaInfo) WebStudioUtils.getWebStudio().getModel().getWrapper().getOpenClass().getMetaInfo();
    }
}
