package org.openl.rules.rest.validation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.openl.rules.rest.config.ValidationConfiguration;
import org.openl.rules.webstudio.mail.MailSender;
import org.openl.rules.webstudio.security.CurrentUserInfo;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.filter.PathFilter;

@Configuration
@ComponentScan(basePackages = "org.openl.rules.rest.validation")
@Import(ValidationConfiguration.class)
public class MockConfiguration {

    @Autowired
    public Environment environment;

    @Bean
    public DesignTimeRepository designTimeRepository() {
        return mock(DesignTimeRepository.class);
    }

    @Bean
    public MailSender mailSender() {
        return mock(MailSender.class);
    }

    @Bean
    public UserManagementService userManagementService() {
        return mock(UserManagementService.class);
    }

    @Bean
    public CurrentUserInfo currentUserInfo() {
        return mock(CurrentUserInfo.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return mock(PasswordEncoder.class);
    }

    @Bean
    public Boolean canCreateInternalUsers() {
        return true;
    }

    @Bean
    public PathFilter zipFilter() {
        PathFilter mocked = mock(PathFilter.class);
        when(mocked.accept(anyString())).thenReturn(Boolean.TRUE);
        return mocked;
    }

    @Bean
    public ZipCharsetDetector zipCharsetDetector() {
        return new ZipCharsetDetector(
                new String[]{"IBM866", "IBM437", "IBM850", "windows-1252", "windows-1251", "windows-1250", "ISO-8859-1"},
                zipFilter());
    }

    @PostConstruct
    public void postConstruct() {
        Props.setEnvironment(environment);
    }

    @Bean
    public GroupManagementService groupManagementService() {
        return mock(GroupManagementService.class);
    }
}
