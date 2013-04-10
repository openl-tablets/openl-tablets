package org.openl.rules.ruleservice.multimodule.top;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.InitializingModuleListener;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;

public class ModuleInitializingListener implements InitializingModuleListener {

    private final Log log = LogFactory.getLog(ModuleInitializingListener.class);
    private static DateFormat dateFormatForModuleName = new SimpleDateFormat("MMddyyyy");
    private static Pattern moduleNamePattern = Pattern.compile("(.*)-(.*)-(.*)-(.*)");

    private CountriesEnum extractCountry(Module module) {
        Matcher matcher = moduleNamePattern.matcher(module.getName());
        matcher.find();
        CountriesEnum country = CountriesEnum.valueOf(matcher.group(2));
        return country;
    }

    private String extractLOB(Module module) {
        Matcher matcher = moduleNamePattern.matcher(module.getName());
        matcher.find();
        return matcher.group(1);
    }

    private Date extractEffectiveDate(Module module) throws ParseException {
        Matcher matcher = moduleNamePattern.matcher(module.getName());
        matcher.find();
        return dateFormatForModuleName.parse(matcher.group(3));
    }

    private Date extractStartRequestDate(Module module) throws ParseException {
        Matcher matcher = moduleNamePattern.matcher(module.getName());
        matcher.find();
        return dateFormatForModuleName.parse(matcher.group(4));
    }

    public void afterModuleLoad(Module module) {
        if (hasPropertiesInModuleName(module)) {
            ITableProperties props = new TableProperties();
            props.setCountry(new CountriesEnum[] { extractCountry(module) });
            props.setLob(extractLOB(module));
            try {
                props.setEffectiveDate(extractEffectiveDate(module));
            } catch (ParseException e) {
                log.warn(String.format("Failed to extract effective date from module \"%s\"", module.getName()), e);
            }
            try {
                props.setStartRequestDate(extractStartRequestDate(module));
            } catch (ParseException e) {
                log.warn("Failed to extract start request date from module", e);
            }
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(PropertiesLoader.EXTERNAL_MODULE_PROPERTIES_KEY, props);
            module.setProperties(params);
        }
    }

    private boolean hasPropertiesInModuleName(Module module) {
        return moduleNamePattern.matcher(module.getName()).matches();
    }

}
