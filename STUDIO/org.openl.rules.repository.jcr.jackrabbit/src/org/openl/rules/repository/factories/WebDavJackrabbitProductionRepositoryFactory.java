package org.openl.rules.repository.factories;

import org.openl.config.ConfigPropertyString;

/**
 * @author PUdalau
 */
public class WebDavJackrabbitProductionRepositoryFactory extends WebDavJacrabbitRepositoryFactory {

    private ConfigPropertyString confWebdavUrl = new ConfigPropertyString("production-repository.remote.webdav.url",
        "http://localhost:8080/jcr/server/");
    private final ConfigPropertyString login = new ConfigPropertyString("production-repository.login", "user");
    private final ConfigPropertyString password = new ConfigPropertyString("production-repository.password", "pass");

    public WebDavJackrabbitProductionRepositoryFactory() {
        setUri(confWebdavUrl);
        setLogin(login);
        setPassword(password);
        setProductionRepositoryMode(true);
    }
}
