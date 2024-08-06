package org.openl.rules.webstudio.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;

/**
 * Tags configuration beans.
 *
 * @author Vladyslav Pikus
 */
@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
public class TagsConfiguration {

    @Bean
    public TagService tagService(TagDao tagDao) {
        return new TagService(tagDao);
    }

    @Bean
    public TagTypeService tagTypeService(TagTypeDao tagTypeDao, TagDao tagDao) {
        return new TagTypeService(tagTypeDao, tagDao);
    }

}
