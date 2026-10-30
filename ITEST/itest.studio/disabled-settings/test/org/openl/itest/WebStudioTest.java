package org.openl.itest;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

class WebStudioTest {

    @Test
    void disabled_settings() throws Exception {
        var smtpServer = new GreenMail(new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP));
        smtpServer.setUser("username@email", "password");
        try {
            smtpServer.start();
            var smtp = smtpServer.getSmtp();
            JettyServer.get()
                    // auth
                    .withInitParam("user.mode", "multi")
                    .withInitParam("security.administrators", "admin")
                    .withInitParam("security.default-group", "Viewers")
                    .withInitParam("security.allow-project-create-delete", "false")
                    // system settings
                    .withInitParam("project.history.count", "99")
                    .withInitParam("project.detect-by-excel-files", "true")
                    .withInitParam("data.format.date", "MM/dd/yyyy")
                    .withInitParam("data.format.time", "hh:mm:ss a")
                    .withInitParam("update.system.properties", "true")
                    .withInitParam("test.run.thread.count", "3")
                    .withInitParam("compile.auto", "false")
                    .withInitParam("dispatching.validation", "false")
                    .withInitParam("audit.retention", "1y")
                    .withInitParam("audit.outputs.console", "true")
                    // DB
                    .withInitParam("db.url", "jdbc:h2:mem:studio;DB_CLOSE_DELAY=-1")
                    // email validation settings
                    .withInitParam("mail.url", smtp.getProtocol() + "://" + smtp.getBindTo() + ":" + smtp.getPort())
                    .withInitParam("mail.username", "username@email")
                    .withInitParam("mail.password", "password")
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
                    .test();
        } finally {
            smtpServer.stop();
        }
    }

    /**
     * Repositories configured outside OpenL Studio, through a chain of {@code $ref} keys, as a deployment sets them
     * in its environment variables: every setting they inherit is read-only, and the list of the repositories is fixed.
     */
    @Test
    void repositories_set_through_references() throws Exception {
        JettyServer.get()
                .withProfile("repository-refs")
                .withInitParam("user.mode", "multi")
                .withInitParam("security.administrators", "admin")
                .withInitParam("db.url", "jdbc:h2:mem:studio-refs;DB_CLOSE_DELAY=-1")
                // shared defaults of the comments and the new branches
                .withInitParam("repo-default.design.comment-template.use-custom-comments", "true")
                .withInitParam("repo-default.design.comment-template.comment-validation-pattern",
                        "\\p{Upper}{2,}(?:-\\d+)+:?\\s+[^\\s](?s).{4,}")
                .withInitParam("repo-default.design.comment-template.invalid-comment-message",
                        "Invalid comment: Comment must be started with ticket number, like: EPBDS-1234 Description")
                .withInitParam("repo-default.design.comment-template.user-message.default.save", "EPBDS-")
                .withInitParam("repo-default.design.new-branch.pattern", "EPBDS-")
                // a block of Git settings the design repositories refer to
                .withInitParam("design.repo-git.$ref", "repo-git")
                .withInitParam("design.repo-git.branch", "development")
                .withInitParam("design.repo-git.protected-branches", "master, release-*")
                .withInitParam("design.repo-git.comment-template.$ref", "repo-default.design.comment-template")
                .withInitParam("design.repo-git.new-branch.$ref", "repo-default.design.new-branch")
                // the design repositories
                .withInitParam("design-repository-configs", "openl-rating, openl-claim")
                .withInitParam("repository.openl-rating.name", "openl-rating")
                .withInitParam("repository.openl-rating.$ref", "design.repo-git")
                .withInitParam("repository.openl-rating.uri", "${openl.home}/repositories/openl-rating")
                .withInitParam("repository.openl-claim.name", "openl-claim")
                .withInitParam("repository.openl-claim.$ref", "design.repo-git")
                .withInitParam("repository.openl-claim.uri", "${openl.home}/repositories/openl-claim")
                .test();
    }
}
