package org.openl.internal.properties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.project.resolving.NoMatchFileNameException;
import org.openl.rules.project.resolving.PropertiesFileNameProcessor;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;

public class SampleFileNameProcessor implements PropertiesFileNameProcessor {
    private final Pattern PATTERN = Pattern.compile("hello-([A-Z]{2}).xls");

    @Override
    public ITableProperties process(String path) throws NoMatchFileNameException {
        Matcher matcher = PATTERN.matcher(path);
        if (matcher.matches()) {
            ITableProperties props = new TableProperties();
            props.setState(new UsStatesEnum[] { UsStatesEnum.valueOf(matcher.group(1)) });
            return props;
        }
        throw new NoMatchFileNameException(String.format("Cannot extract properties from module '%s'", path));
    }

}
