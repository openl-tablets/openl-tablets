package org.openl.ie.constrainer;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Exigen Group, Inc.</p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Iterator;
import java.util.Set;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalFastMinimize;
import org.openl.ie.constrainer.GoalIntSetGenerate;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.GoalOr;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntSetVar;
import org.openl.ie.constrainer.IntSetVarArray;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.impl.IntSetEvent.IntSetEventConstants;


import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TestIntSetVar extends TestCase {
    static class TestCrews {
        static Constrainer C = new Constrainer("");

        static final String[] names = { "Tom", "David", "Jeremy", "Ron", "Joe", "Bill", "Fred", "Bob", "Mario", "Ed",
                "Carol", "Janet", "Tracy", "Marilyn", "Carolyn", "Cathy", "Inez", "Jean", "Heather", "Juliet" };

        static final int Tom = 0, David = 1, Jeremy = 2, Ron = 3, Joe = 4, Bill = 5, Fred = 6, Bob = 7, Mario = 8,
                Ed = 9, Carol = 10, Janet = 11, Tracy = 12, Marilyn = 13, Carolyn = 14, Cathy = 15, Inez = 16,
                Jean = 17, Heather = 18, Juliet = 19;

        static final int iNbMembers = 0, iStewards = 1, iHostesses = 2, iFrench = 3, iSpanish = 4, iGerman = 5;

        static int[] Staff = { Tom, David, Jeremy, Ron, Joe, Bill, Fred, Bob, Mario, Ed, Carol, Janet, Tracy, Marilyn,
                Carolyn, Cathy, Inez, Jean, Heather, Juliet };

        static IntSetVar Stewards = C.addIntSetVar(
                new int[] { Tom, David, Jeremy, Ron, Joe, Bill, Fred, Bob, Mario, Ed }, "Stewards");

        static IntSetVar Hostesses = C.addIntSetVar(new int[] { Carol, Janet, Tracy, Marilyn, Carolyn, Cathy, Inez,
                Jean, Heather, Juliet }, "Hostesses");

        static IntSetVar French = C.addIntSetVar(new int[] { Inez, Bill, Jean, Juliet }, "French");
        static IntSetVar German = C.addIntSetVar(new int[] { Tom, Jeremy, Mario, Cathy, Juliet }, "German");
        static IntSetVar Spanish = C.addIntSetVar(new int[] { Bill, Fred, Joe, Mario, Marilyn, Inez, Heather },
                "Spanish");

        static IntSetVarArray crews = new IntSetVarArray(C, 10, "crews");

        static private int[][] req = {/*
                                         * crew nbMembers stewards hostesses
                                         * french spanish german
                                         */
        /* #1 */{ 4, 1, 1, 1, 1, 1 },
        /* #2 */{ 5, 1, 1, 1, 1, 1 },
        /* #3 */{ 5, 1, 1, 1, 1, 1 },
        /* #4 */{ 6, 2, 2, 1, 1, 1 },
        /* #5 */{ 7, 3, 3, 1, 1, 1 },
        /* #6 */{ 4, 1, 1, 1, 1, 1 },
        /* #7 */{ 5, 1, 1, 1, 1, 1 },
        /* #8 */{ 6, 1, 1, 1, 1, 1 },
        /* #9 */{ 6, 2, 2, 1, 1, 1 },
        /* #10 */{ 7, 3, 3, 1, 1, 1 }, };

        static {
            C.showInternalNames(true);
            for (int i = 0; i < crews.size(); i++) {
                crews.set(C.addIntSetVar(Staff, "flight#" + i), i);
            }
        };

        static void checkResults(IntSetVarArray array) {
            int size = array.size();
            for (int i = 0; i < size; i++) {
                IntSetVar var = array.get(i);
                Set checkSet = var.requiredSet();
                assertTrue(checkSet.size() >= req[i][iNbMembers]);
                assertTrue(intersectionCardinality(checkSet, Stewards.possibleSet()) >= req[i][iStewards]);
                assertTrue(intersectionCardinality(checkSet, Hostesses.possibleSet()) >= req[i][iHostesses]);
                assertTrue(intersectionCardinality(checkSet, French.possibleSet()) >= req[i][iFrench]);
                assertTrue(intersectionCardinality(checkSet, German.possibleSet()) >= req[i][iGerman]);
                assertTrue(intersectionCardinality(checkSet, Spanish.possibleSet()) >= req[i][iSpanish]);
            }
            for (int i = 0; i < size - 2; i++) {
                assertTrue(intersectionCardinality(array.get(i).requiredSet(), array.get(i + 1).requiredSet()) == 0);

            }

            assertTrue(intersectionCardinality(array.get(size - 2).requiredSet(), array.get(size - 1).requiredSet()) == 0);
        }

        static void postTeamConstraints(IntSetVar E, int n, int[] nbMembers, IntSetVarArray memberSets) throws Failure {
            C.postConstraint(E.cardinality().eq(n));
            for (int i = 0; i < nbMembers.length; i++) {
                C.postConstraint(E.intersectionWith(memberSets.get(i)).cardinality().ge(nbMembers[i]));
            }
        }

        static void printResults() {
            printSetArray(crews, true);
        }

        static void printSet(IntSetVar var, boolean requiredOnly) {
            System.out.print(var.name());
            Iterator iter;
            if (requiredOnly) {
                System.out.print(": ");
                iter = var.requiredSet().iterator();
            } else {
                System.out.print("(possibleSet)" + ": ");
                iter = var.possibleSet().iterator();
            }
            while (iter.hasNext()) {
                Integer val = (Integer) iter.next();
                System.out.print(names[val.intValue()] + (iter.hasNext() ? ", " : ""));
            }
        }

        static void printSetArray(IntSetVarArray array, boolean requiredOnly) {
            for (int i = 0; i < array.size(); i++) {
                printSet(array.get(i), requiredOnly);
                System.out.println("");
            }
        }

        static void test() {
            IntSetVarArray memberSets = new IntSetVarArray(C, 5);

            memberSets.set(Stewards, 0);
            memberSets.set(Hostesses, 1);
            memberSets.set(French, 2);
            memberSets.set(German, 3);
            memberSets.set(Spanish, 4);

            try {
                for (int i = 0; i < crews.size() - 1; i++) {
                    crews.get(i).nullIntersectWith(crews.get(i + 1)).execute();
                    if ((i + 2) < 10) {
                        crews.get(i).nullIntersectWith(crews.get(i + 2)).execute();
                    }
                }

                for (int i = 0; i < crews.size(); i++) {
                    postTeamConstraints(crews.get(i), req[i][iNbMembers], new int[] { req[i][iStewards],
                            req[i][iHostesses], req[i][iFrench], req[i][iGerman], req[i][iSpanish] }, memberSets);
                }

                assertTrue("Solution for crews task wasn't found", C.execute(new GoalIntSetGenerate(crews)));
                checkResults(crews);

            } catch (Failure f) {
                fail("test failed due to Failure exception's being thrown");
            }

        }

    }// ~TestCrews
    static Constrainer C = new Constrainer("");

    static {
        C.showInternalNames(true);
    }

    static public int intersectionCardinality(Set set1, Set set2) {
        Iterator iter = set2.iterator();
        int counter = 0;
        while (iter.hasNext()) {
            if (set1.contains(iter.next())) {
                counter++;
            }
        }
        return counter;
    }

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestIntSetVar.class));
    }

    public TestIntSetVar(String name) {
        super(name);
    }

    public void testCrews() {
        TestCrews.test();
        TestCrews.printResults();
    }

    public void testEventPropagation() {
        class IntSetVarValueObserver extends Observer {
            int counter = 0;

            @Override
            public Object master() {
                return this;
            }

            @Override
            public int subscriberMask() {
                return IntSetEventConstants.VALUE;
            }

            @Override
            public void update(Subject var, EventOfInterest ev) throws Failure {
                // IntSetVar.IntSetEvent e = (IntSetVar.IntSetEvent)ev;

                System.out.println("value#" + counter + " obtained: " + ((IntSetVar) var).value());
                counter++;
            }
        }

        IntSetVar var1 = C.addIntSetVar(new int[] { 1, 2, 3, 4, 5, 6, 7 });
        var1.attachObserver(new IntSetVarValueObserver());
        IntSetVar var2 = C.addIntSetVar(new int[] { 5, 6, 7, 8, 9, 10, 11 });
        IntSetVar var3 = C.addIntSetVar(new int[] { 28, 78, 23, 1, 4, 7 });

        IntSetVarArray array = new IntSetVarArray(C, 1);
        array.set(var1, 0);

        Goal generate = new GoalIntSetGenerate(array);

        IntExp intersectionWithVar2 = var1.intersectionWith(var2).cardinality();
        IntExp intersectionWithVar3 = var1.intersectionWith(var3).cardinality();

        Goal alt1 = new GoalMinimize(generate, intersectionWithVar2.neg());
        Goal alt2 = new GoalMinimize(generate, intersectionWithVar3.neg());

        alt1 = new GoalAnd(alt1, new GoalFail(C));

        Goal main = new GoalOr(alt1, alt2);
        C.execute(main);
        System.out.println("Succeeded");
    }

    public void testIntersection() {
        int[] array1 = { 1, 2, 3, 4, 5, 6, 7 };
        int[] array2 = { 1, 2, 7, 9, 4 };

        Constrainer C = new Constrainer("test");
        IntSetVar var2 = C.addIntSetVar(array1);
        IntSetVar var1 = C.addIntSetVar(array2);

        IntSetVar var3 = var2.intersectionWith(var1);
        try {
            C.postConstraint(var3.cardinality().eq(2));
            IntSetVarArray array = new IntSetVarArray(C, 2);
            array.set(var1, 0);
            array.set(var2, 1);

            assertTrue(C.execute(new GoalIntSetGenerate(array)));
            assertTrue(intersectionCardinality(var2.requiredSet(), var1.requiredSet()) == 2);
        } catch (Failure f) {
            fail("test failed: " + f);
        }

        var1 = C.addIntSetVar(new int[] { 1, 2, 3, 4, 5 });
        var2 = C.addIntSetVar(new int[] { 4, 5, 6, 7, 8 });
        var3 = var2.intersectionWith(var1);

        try {
            IntSetVarArray array = new IntSetVarArray(C, 2);
            array.set(var1, 0);
            array.set(var2, 1);
            assertTrue(C.execute(new GoalFastMinimize(new GoalIntSetGenerate(array), var3.cardinality().neg())));
            assertTrue(var1.value().contains(new Integer(4)));
            assertTrue(var1.value().contains(new Integer(5)));
            assertTrue(var2.value().contains(new Integer(4)));
            assertTrue(var2.value().contains(new Integer(5)));
        } catch (Failure f) {
            fail("test failed: " + f);
        }
    }
}
