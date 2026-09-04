package org.openl.rules.webstudio.web.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.studio.projects.service.trace.TableInputParserService;
import org.openl.types.IOpenClass;

class InputArgsBeanTest {

    @Test
    void parsesSingleJsonParameterWhenRuntimeContextPropertyIsMissing() throws Exception {
        var environment = mock(Environment.class);
        var inputParserService = mock(TableInputParserService.class);
        var inputArgsBean = new InputArgsBean(environment, inputParserService);
        inputArgsBean.setInputTestCaseType(InputArgsBean.InputTestCaseType.TEXT);
        inputArgsBean.setInputTextBean("{}");

        var parameterType = mock(IOpenClass.class);
        var parameter = mock(ParameterDeclarationTreeNode.class);
        when(parameter.getType()).thenReturn(parameterType);
        ReflectionTestUtils.setField(inputArgsBean, "argumentTreeNodes", new ParameterDeclarationTreeNode[]{parameter});

        var expectedParameter = new Object();
        var objectMapper = new ObjectMapper();
        when(inputParserService.parseParameter("{}", parameterType, objectMapper)).thenReturn(expectedParameter);

        var rulesDeploy = new RulesDeploy();
        var objectMapperFactory = mock(ProjectJacksonObjectMapperFactoryBean.class);
        when(objectMapperFactory.createJacksonObjectMapper()).thenReturn(objectMapper);
        var webStudio = mock(WebStudio.class);
        when(webStudio.getCurrentProjectRulesDeploy()).thenReturn(rulesDeploy);
        when(webStudio.getCurrentProjectJacksonObjectMapperFactoryBean()).thenReturn(objectMapperFactory);

        try (var webStudioUtils = mockStatic(WebStudioUtils.class)) {
            webStudioUtils.when(WebStudioUtils::getWebStudio).thenReturn(webStudio);

            assertArrayEquals(new Object[]{expectedParameter}, inputArgsBean.getParams());
        }
    }
}
