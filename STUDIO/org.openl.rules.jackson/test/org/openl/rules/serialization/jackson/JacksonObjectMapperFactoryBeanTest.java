package org.openl.rules.serialization.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.serialization.DefaultTypingMode;
import org.openl.rules.serialization.JacksonObjectMapperFactoryBean;

public class JacksonObjectMapperFactoryBeanTest {

    @Test
    public void testSpreadsheetResult() throws ClassNotFoundException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        ObjectMapper objectMapper = bean.createJacksonObjectMapper();
        SpreadsheetResult value = new SpreadsheetResult();
        value.setResults(new Object[3][3]);
        value.setColumnNames(new String[3]);
        value.setRowNames(new String[3]);
        String text = objectMapper.writeValueAsString(value);
        SpreadsheetResult result = objectMapper.readValue(text, SpreadsheetResult.class);
        assertNotNull(result);
    }

    @Test
    public void testRange() throws ClassNotFoundException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        ObjectMapper objectMapper = bean.createJacksonObjectMapper();
        String text = objectMapper
                .writeValueAsString(new DoubleRange("(0; 1)"));
        DoubleRange result = objectMapper.readValue(text, DoubleRange.class);
        assertNotNull(result);

        text = objectMapper.writeValueAsString(new IntRange(199, 299));
        IntRange intRange = objectMapper.readValue(text, IntRange.class);
        assertNotNull(intRange);
        assertEquals(199, intRange.getMin());
        assertEquals(299, intRange.getMax());
    }

    @Test
    public void testIRulesRuntimeContext() throws ClassNotFoundException, IOException {
        DefaultRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        Date date = new Date();
        context.setCurrentDate(date);
        context.setLob("LOB");
        context.setLocale(Locale.FRANCE);
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        ObjectMapper objectMapper = bean.createJacksonObjectMapper();
        String text = objectMapper.writeValueAsString(context);

        IRulesRuntimeContext iRulesRuntimeContext = objectMapper.readValue(text, IRulesRuntimeContext.class);

        assertEquals(date, iRulesRuntimeContext.getCurrentDate());
        assertEquals("LOB", iRulesRuntimeContext.getLob());
        assertEquals(Locale.FRANCE, iRulesRuntimeContext.getLocale());

        assertEquals("fr_FR", objectMapper.readTree(text).get("locale").asText());
    }

    public static class Wrapper {
        public Animal animal;
        public Animal[] animals;
        public Object[] arrayOfAnimals;
    }

    public static class Animal {
        public String name;
    }

    public static class Dog extends Animal {
    }

    public static class Cat extends Animal {
    }

    @Test
    public void testOverrideTypesSmart() throws ClassNotFoundException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setDefaultTypingMode(DefaultTypingMode.OBJECT_AND_NON_CONCRETE);
        bean.setPolymorphicTypeValidation(true);
        Set<String> overrideTypes = new HashSet<>();
        overrideTypes.add(Animal.class.getName());
        overrideTypes.add(Dog.class.getName());
        overrideTypes.add(Cat.class.getName());
        bean.setOverrideTypes(overrideTypes);
        Wrapper wrapper = new Wrapper();
        wrapper.animal = new Dog();
        wrapper.animals = new Animal[]{new Dog()};
        wrapper.arrayOfAnimals = new Animal[]{new Dog()};
        ObjectMapper objectMapper = bean.createJacksonObjectMapper();
        String text = objectMapper.writeValueAsString(wrapper);
        Wrapper w = objectMapper.readValue(text, Wrapper.class);
        assertNotNull(w);
        assertTrue(w.animal instanceof Dog);
        assertNotNull(w.animals);
        assertEquals(1, w.animals.length);
        assertTrue(w.animals[0] instanceof Dog);
        assertNotNull(w.arrayOfAnimals);
        assertEquals(1, w.arrayOfAnimals.length);
        assertTrue(w.arrayOfAnimals[0] instanceof Dog);
    }

    @Test
    public void testOverrideTypesEnable() throws ClassNotFoundException, IOException {
        JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
        bean.setDefaultTypingMode(DefaultTypingMode.OBJECT_AND_NON_CONCRETE);
        bean.setPolymorphicTypeValidation(true);
        Set<String> overrideTypes = new HashSet<>();
        overrideTypes.add(Wrapper.class.getName());
        overrideTypes.add(Animal.class.getName());
        overrideTypes.add(Dog.class.getName());
        overrideTypes.add(Cat.class.getName());
        bean.setOverrideTypes(overrideTypes);
        Wrapper wrapper = new Wrapper();
        wrapper.animal = new Dog();
        wrapper.animals = new Animal[]{new Dog()};
        wrapper.arrayOfAnimals = new Animal[]{new Dog()};
        ObjectMapper objectMapper = bean.createJacksonObjectMapper();
        String text = objectMapper.writeValueAsString(wrapper);
        Wrapper w = objectMapper.readValue(text, Wrapper.class);
        assertNotNull(w);
        assertTrue(w.animal instanceof Dog);
        assertNotNull(w.animals);
        assertEquals(1, w.animals.length);
        assertTrue(w.animals[0] instanceof Dog);
        assertNotNull(w.arrayOfAnimals);
        assertEquals(1, w.arrayOfAnimals.length);
        assertTrue(w.arrayOfAnimals[0] instanceof Dog);
    }

    @Test
    public void testOverrideTypesEnableMissedClass() throws ClassNotFoundException, IOException {
        assertThrows(InvalidTypeIdException.class, () -> {
            JacksonObjectMapperFactoryBean bean = new JacksonObjectMapperFactoryBean();
            bean.setDefaultTypingMode(DefaultTypingMode.NON_FINAL);
            bean.setPolymorphicTypeValidation(true);
            Set<String> overrideTypes = new HashSet<>();
            overrideTypes.add(Animal.class.getName());
            overrideTypes.add(Dog.class.getName());
            overrideTypes.add(Cat.class.getName());
            bean.setOverrideTypes(overrideTypes);
            Wrapper wrapper = new Wrapper();
            wrapper.animal = new Dog();
            ObjectMapper objectMapper = bean.createJacksonObjectMapper();
            String text = objectMapper.writeValueAsString(wrapper).replace("$Wrapper", "$Wrapper1");
            objectMapper.readValue(text, Wrapper.class);
        });
    }
}
