package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitDesignRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString("design-repository.remote.webdav.url",
        "http://localhost:8080/jcr/server/");
    private final ConfigPropertyString login = new ConfigPropertyString("design-repository.login", null);
    private final ConfigPropertyString password = new ConfigPropertyString("design-repository.pass", null);

    public WebDavJackrabbitDesignRepositoryFactory() {
        setUri(confWebdavUrl);
        setLogin(login);
        setPassword(password);
    }
}
