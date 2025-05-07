package org.openl.itest;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

public class WebStudioTest {

    @Test
    public void wizard() throws Exception {
        JettyServer.test("wizard");
    }

    @Test
    public void simple() throws Exception {
        JettyServer.test("simple");
    }

    @Test
    public void multi() throws Exception {
        JettyServer.test("multi");
    }

    @Test
    public void repos() throws Exception {
        JettyServer.test("repos");
    }

    @Test
    public void dtr() throws Exception {
        JettyServer.test("dtr");
    }

    @Test
    public void acl() throws Exception {
        JettyServer.test("acl");
    }

    @Test
    public void disabled_settings() throws Exception {
        var smtpServer = new GreenMail(new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP));
        smtpServer.setUser("username@email", "password");
        try {
            smtpServer.start();
            var smtp = smtpServer.getSmtp();
            JettyServer.get()
                    .withInitParam("webstudio.configured", "true")
                    .withProfile("disabled-settings")
                    // system settings
                    .withInitParam("user.workspace.home", "openl-repository/workspace")
                    .withInitParam("project.history.count", "99")
                    .withInitParam("data.format.date", "MM/dd/yyyy")
                    .withInitParam("data.format.time", "hh:mm:ss a")
                    .withInitParam("update.system.properties", "true")
                    .withInitParam("test.run.thread.count", "3")
                    .withInitParam("compile.auto", "false")
                    .withInitParam("dispatching.validation", "false")
                    // email validation settings
                    .withInitParam("mail.url", smtp.getProtocol() + "://" + smtp.getBindTo() + ":" + smtp.getPort())
                    .withInitParam("mail.username", "username@email")
                    .withInitParam("mail.password", "password")
                    // group settings
                    .withInitParam("security.default-group", "Viewers")
                    // design repository settings
                    .withInitParam("design-repository-configs", "design")
                    .withInitParam("repository.design.factory", "repo-jdbc")
                    .withInitParam("repository.design.name", "H2 Design")
                    .withInitParam("repository.design.uri", "jdbc:h2:mem:design-repo;DB_CLOSE_DELAY=-1")
                    // production repository settings
                    .withInitParam("production-repository-configs", "production")
                    .withInitParam("repository.production.$ref", "repo-jdbc")
                    .withInitParam("repository.production.name", "H2 Production")
                    .withInitParam("repository.production.uri", "jdbc:h2:mem:production-repo;DB_CLOSE_DELAY=-1")
                    .withInitParam("repository.production.base.path.$ref", "repo-default.production.base.path")
                    // deploy configuration repository settings
                    .withInitParam("repository.deploy-config.use-repository", "design")
                    .test();
        } finally {
            smtpServer.stop();
        }
    }
}
