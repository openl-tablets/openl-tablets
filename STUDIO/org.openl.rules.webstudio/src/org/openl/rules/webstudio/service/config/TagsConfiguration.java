/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.webstudio.service.config;

import org.openl.rules.security.standalone.dao.OpenLProjectDao;
import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Tags configuration beans.
 *
 * @author Vladyslav Pikus
 */
@Configuration
@ImportResource("classpath:META-INF/standalone/spring/security-hibernate-beans.xml")
public class TagsConfiguration {

    @Bean
    public OpenLProjectService openLProjectService(OpenLProjectDao openLProjectDao) {
        return new OpenLProjectService(openLProjectDao);
    }

    @Bean
    public TagService tagService(TagDao tagDao) {
        return new TagService(tagDao);
    }

    @Bean
    public TagTypeService tagTypeService(TagTypeDao tagTypeDao, TagDao tagDao) {
        return new TagTypeService(tagTypeDao, tagDao);
    }

}
