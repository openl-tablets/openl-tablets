package org.openl.tests;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.openl.generated.model.epbds7808.Car;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.impl.URLSourceCodeModule;

public class EPBDS7808Test {

    private static final String SRC = "test-resources/tests/EPBDS-7808_sort.xlsx";

    private Comparator<Car> carValueComporator;
    private Service service;
    private Random rnd = new Random();

    @Before
    public void setUp() {
        RulesEngineFactory<Service> engineFactory = new RulesEngineFactory<>(URLSourceCodeModule.toUrl(new File(SRC)),
            Service.class);
        service = engineFactory.newEngineInstance();

        carValueComporator = new Comparator<Car>() {
            public int compare(Car a, Car b) {
                return a.getValue().compareTo(b.getValue());
            }
        };
    }

    private void evaluate(List<Car> actualList, boolean isDesc) {
        final List<Car> expectedList = new ArrayList<>(actualList);
        Collections.sort(expectedList, isDesc ? Collections.reverseOrder(carValueComporator) : carValueComporator);

        Object[] actualResult = isDesc ? service.carSortByDesc(actualList.toArray(new Car[0]))
                                       : service.carSortByAsc(actualList.toArray(new Car[0]));
        assertArrayEquals(expectedList.toArray(), actualResult);
    }

    @Test
    public void test_descAndAscSortByValue_whenAllValuesAreEq_thenOrderIsNotChanged() {
        final List<Car> actualList = new ArrayList<>();
        for (int i = 1; i < 9; i++) {
            actualList.add(new Car("car" + i, 8));
        }

        evaluate(actualList, true);
        evaluate(actualList, false);
    }

    @Test
    public void test_descAndAscSortByValue_whenSomeValuesAreEq() {
        final List<Car> actualList = new ArrayList<>();
        actualList.add(new Car("car1", 1));
        actualList.add(new Car("car2", 3));
        actualList.add(new Car("car3", 2));
        actualList.add(new Car("car4", 4));
        actualList.add(new Car("car5", 4));
        actualList.add(new Car("car6", 7));
        actualList.add(new Car("car7", 6));

        evaluate(actualList, true);
        evaluate(actualList, false);
    }

    @Test
    public void test_descAndAscSortByValue_whenAllValuesAreRnd() {
        final List<Car> actualList = new ArrayList<>();
        for (int i = 1; i < 1000; i++) {
            actualList.add(new Car("car" + i, rnd.nextInt(100)));
        }

        evaluate(actualList, true);
        evaluate(actualList, false);
    }

    @Test
    public void test_descSortByValue_whenValuesAreSortedByAsc() {
        final List<Car> actualList = new ArrayList<>();
        actualList.add(new Car("car1", 1));
        actualList.add(new Car("car2", 2));
        actualList.add(new Car("car3", 3));
        actualList.add(new Car("car4", 4));
        actualList.add(new Car("car5", 5));
        actualList.add(new Car("car6", 6));
        actualList.add(new Car("car7", 7));

        evaluate(actualList, true);
    }

    @Test
    public void test_ascSortByValue_whenValuesAreSortedByAsc() {
        final List<Car> actualList = new ArrayList<>();
        actualList.add(new Car("car1", 7));
        actualList.add(new Car("car2", 6));
        actualList.add(new Car("car3", 5));
        actualList.add(new Car("car4", 4));
        actualList.add(new Car("car5", 3));
        actualList.add(new Car("car6", 2));
        actualList.add(new Car("car7", 1));

        evaluate(actualList, false);
    }

    public interface Service {

        Car[] carSortByDesc(Car[] source);

        Car[] carSortByAsc(Car[] source);

    }

}
