package org.openl.studio.projects.service.tables;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.security.AuthenticationSettings;
import org.openl.rules.webstudio.web.admin.security.UserMode;

/**
 * The properties OpenL Studio records about a table itself, rather than the author typing them.
 *
 * @author Vladyslav Pikus
 */
@Service
@RequiredArgsConstructor
public class SystemPropertiesService {

    private final Environment environment;

    /**
     * What a table written for the first time is stamped with: who created it and when.
     *
     * <p>Only the properties recorded once are stamped — the ones recorded on every edit belong to a save, not to a
     * creation. Nothing is stamped while the administrator has turned the recording off, and the author is left out
     * of a single-user installation, where there is only one.
     *
     * @return the properties to write, in the order they are declared; empty when nothing is recorded
     */
    public Map<String, Object> onCreate() {
        return stamped(SystemValuePolicy.IF_BLANK_ONLY);
    }

    /**
     * What a table is stamped with whenever it is edited: who edited it and when.
     *
     * <p>Only the properties recorded on every edit are stamped — the ones recorded once belong to a creation.
     * Nothing is stamped while the administrator has turned the recording off, and the author is left out of a
     * single-user installation, where there is only one.
     *
     * @return the properties to write, in the order they are declared; empty when nothing is recorded
     */
    public Map<String, Object> onEdit() {
        return stamped(SystemValuePolicy.ON_EACH_EDIT);
    }

    /** The system properties recorded under the given policy, valued as of now. */
    private Map<String, Object> stamped(SystemValuePolicy policy) {
        if (!Boolean.TRUE.equals(
                environment.getProperty(AdministrationSettings.UPDATE_SYSTEM_PROPERTIES, Boolean.class))) {
            return Map.of();
        }
        // One author, who is therefore not worth recording.
        boolean singleUser = UserMode.SINGLE.getValue().equals(
                environment.getProperty(AuthenticationSettings.USER_MODE));
        var stamped = new LinkedHashMap<String, Object>();
        for (TablePropertyDefinition definition : TablePropertyDefinitionUtils.getSystemProperties()) {
            var value = valueOf(definition, singleUser, policy);
            if (value != null) {
                stamped.put(definition.getName(), value);
            }
        }
        return stamped;
    }

    /** The value the property is stamped with, or {@code null} when it is not one that policy records. */
    private static Object valueOf(TablePropertyDefinition definition, boolean singleUser, SystemValuePolicy policy) {
        var descriptor = definition.getSystemValueDescriptor();
        if (definition.getSystemValuePolicy() != policy
                || (singleUser && SystemValuesManager.CURRENT_USER_DESCRIPTOR.equals(descriptor))) {
            return null;
        }
        return SystemValuesManager.getInstance().getSystemValue(descriptor);
    }
}
