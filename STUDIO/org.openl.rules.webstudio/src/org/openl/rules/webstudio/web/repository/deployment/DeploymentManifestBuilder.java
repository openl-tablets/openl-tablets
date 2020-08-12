package org.openl.rules.webstudio.web.repository.deployment;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.openl.info.OpenLVersion;
import org.openl.util.StringUtils;

/**
 * Builds {@link Manifest} entry for deployment. Default parameters:</br>
 * <p>
 *     Manifest-Version: 1.0</br>
 *     Build-Date: current date in ISO 8601 format</br>
 *     Created-By: OpenL WebStudio v.{@code project.version}</br>
 * </p>
 *
 * Optional parameters: Build-Number, Built-By, Branch-Name, Implementation-Title
 *
 */
public class DeploymentManifestBuilder {

    private Map<String, String> entries;

    public DeploymentManifestBuilder() {
        this.entries = new HashMap<>();
    }

    /**
     * Set {@code Build-Number} property
     * @param number project revision number
     * @return {@code this}
     */
    public DeploymentManifestBuilder setBuildNumber(String number) {
        putValue("Build-Number", number);
        return this;
    }

    /**
     * Set {@code Built-By} property
     * @param builtBy current user
     * @return {@code this}
     */
    public DeploymentManifestBuilder setBuiltBy(String builtBy) {
        putValue("Built-By", builtBy);
        return this;
    }

    /**
     * Set {@code Implementation-Title} property
     * @param implementationTitle project title
     * @return {@code this}
     */
    public DeploymentManifestBuilder setImplementationTitle(String implementationTitle) {
        putValue(Attributes.Name.IMPLEMENTATION_TITLE.toString(), implementationTitle);
        return this;
    }

    /**
     * Set {@code Branch-Name} property
     * @param branchName working branch
     * @return {@code this}
     */
    public DeploymentManifestBuilder setBranchName(String branchName) {
        putValue("Branch-Name", branchName);
        return this;
    }

    private void putValue(String name, String value) {
        entries.put(name, StringUtils.trimToEmpty(value));
    }

    public Manifest build() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Build-Date", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        attributes.putValue("Created-By", "OpenL WebStudio v." + OpenLVersion.getVersion());
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            attributes.putValue(entry.getKey(), entry.getValue());
        }
        return manifest;
    }
}
