package org.openl.rules.rest.validation;

import javax.validation.constraints.Size;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
public class BasicValidatorTest  extends AbstractConstraintValidatorTest {

    @Test
    public void testSmoke() {
        MyDTO dto = new MyDTO();
        dto.field1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

        BindingResult result = validateAndGetResult(dto);

    }

    private static class MyDTO {

        @Size(max = 10, message = "{openl.constraints.size.max.message}")
        public String field1;

    }

}
