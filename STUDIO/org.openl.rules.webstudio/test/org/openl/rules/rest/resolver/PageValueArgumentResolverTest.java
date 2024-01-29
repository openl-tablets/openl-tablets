package org.openl.rules.rest.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import org.openl.rules.repository.api.Offset;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.resolver.test.PaginationTestController;

@SpringJUnitConfig(classes = {MockConfiguration.class, PageValueArgumentResolverTest.TestConfig.class})
@WebAppConfiguration
public class PageValueArgumentResolverTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Consumer<Pageable> pageableConsumer;

    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    }

    @AfterEach
    public void reset() {
        Mockito.reset(pageableConsumer);
    }

    @Test
    public void testPageQuery() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset"));
        verify(pageableConsumer).accept(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertTrue(pageable.isUnpaged());
    }

    @Test
    public void testBadRequest() throws Exception {
        var mockMvcResult = mockMvc
            .perform(get("/pagination-test/pageOrOffset").queryParam("page", "100").queryParam("offset", "11"))
            .andReturn();
        BadRequestException error = (BadRequestException) mockMvcResult.getResolvedException();
        assertNotNull(error);
        assertEquals("openl.error.400.invalid.pageable.query.message", error.getErrorCode());
    }

    @Test
    public void testPageQuery1() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("size", "100"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testPageQuery2() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("page", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getPageNumber());
        assertEquals(AbstractPaginationValueArgumentResolver.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    public void testPageQuery3() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("size", "100").queryParam("page", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testPageQueryWithDefault() throws Exception {
        performRequest(get("/pagination-test/pageWithDefault"));
        verify(pageableConsumer).accept(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(1, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
    }

    @Test
    public void testPageQuery1WithDefault() throws Exception {
        performRequest(get("/pagination-test/pageWithDefault").queryParam("size", "100"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(1, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testPageQuery2WithDefault() throws Exception {
        performRequest(get("/pagination-test/pageWithDefault").queryParam("page", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
    }

    @Test
    public void testPageQuery3WithDefault() throws Exception {
        performRequest(get("/pagination-test/pageWithDefault").queryParam("size", "100").queryParam("page", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    // ------

    @Test
    public void testOffsetQuery() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset"));
        verify(pageableConsumer).accept(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertTrue(pageable.isUnpaged());
    }

    @Test
    public void testOffsetQuery1() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("size", "100"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(0, pageable.getOffset());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testOffsetQuery2() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("offset", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getOffset());
        assertEquals(AbstractPaginationValueArgumentResolver.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    public void testOffsetQuery3() throws Exception {
        performRequest(get("/pagination-test/pageOrOffset").queryParam("size", "100").queryParam("offset", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getOffset());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testOffsetQueryWithDefault() throws Exception {
        performRequest(get("/pagination-test/offsetWithDefault"));
        verify(pageableConsumer).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(4, pageable.getOffset());
        assertEquals(50, pageable.getPageSize());
    }

    @Test
    public void testOffsetQuery1WithDefault() throws Exception {
        performRequest(get("/pagination-test/offsetWithDefault").queryParam("size", "100"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(4, pageable.getOffset());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    public void testOffsetQuery2WithDefault() throws Exception {
        performRequest(get("/pagination-test/offsetWithDefault").queryParam("offset", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getOffset());
        assertEquals(50, pageable.getPageSize());
    }

    @Test
    public void testOffsetQuery3WithDefault() throws Exception {
        performRequest(get("/pagination-test/offsetWithDefault").queryParam("size", "100").queryParam("offset", "5"));
        verify(pageableConsumer, atLeastOnce()).accept(pageableCaptor.capture());
        var pageable = (Offset) pageableCaptor.getValue();
        assertFalse(pageable.isUnpaged());
        assertEquals(5, pageable.getOffset());
        assertEquals(100, pageable.getPageSize());
    }

    private void performRequest(RequestBuilder request) throws Exception {
        var mockMvcResult = mockMvc.perform(request).andReturn();
        if (mockMvcResult.getResolvedException() != null) {
            throw mockMvcResult.getResolvedException();
        }
    }

    @Configuration
    @ComponentScan(basePackageClasses = PaginationTestController.class)
    public static class TestConfig {

        @Bean
        @SuppressWarnings("unchecked")
        public Consumer<Pageable> pageableConsumer() {
            return (Consumer<Pageable>) mock(Consumer.class);
        }

        @Bean
        public PageValueArgumentResolver pageValueArgumentResolver() {
            return new PageValueArgumentResolver();
        }

        @Bean
        public OffsetValueArgumentResolver offsetValueArgumentResolver() {
            return new OffsetValueArgumentResolver();
        }

        @Bean
        public PaginationValueArgumentResolver paginationValueArgumentResolver(
                PageValueArgumentResolver pageValueArgumentResolver,
                OffsetValueArgumentResolver offsetValueArgumentResolver) {
            return new PaginationValueArgumentResolver(offsetValueArgumentResolver, pageValueArgumentResolver);
        }

    }

}
