package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.ChoicePointLabel;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.impl.GoalStack;
import org.openl.ie.constrainer.impl.SubjectImpl;
import org.openl.ie.constrainer.impl.UndoStack;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class TestGoalStack extends TestCase {
    private class EmptyGoal extends GoalImpl {
        private int _num;

        public EmptyGoal(int num) {
            super(C, "EG_" + num);
            _num = num;
        }

        public Goal execute() throws Failure {
            return null;
        }

        public int num() {
            return _num;
        }
    }
    private class GoalN extends GoalImpl {
        private int _N;

        public GoalN(int i) {
            super(C, "G_" + i);
            _N = i;
        }

        public Goal execute() throws Failure {
            IntExp exp = _array.get(_N);
            // pushing undos to UndoStack
            Undo undo = exp.createUndo();
            undo.undoable(exp);
            _undoStack.pushUndo(undo);
            undo = SubjectImpl.UndoAttachObserver.getUndo(exp, _observers[_N]);
            _undoStack.pushUndo(undo);
            // making changes
            try {
                exp.setMin(_N);
            } catch (Failure f) {
                fail("test failed");
            }
            exp.attachObserver(_observers[_N]);
            return null;
        }
    }
    static private int nbGoals = 100;
    private Constrainer C = new Constrainer("TestGoalStack");
    private IntExpArray _array = new IntExpArray(C, nbGoals, 0, nbGoals - 1, "array");

    private UndoStack _undoStack = new UndoStack();

    private TestUtils.TestObserver[] _observers = new TestUtils.TestObserver[_array.size()];

    public static void main(String[] args) {
        TestRunner.run(new TestSuite(TestGoalStack.class));
    }

    public TestGoalStack(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        for (int i = 0; i < _observers.length; i++) {
            _observers[i] = TestUtils.createTestObserver();
        }
    }

    public void testBackTrack() {
        Goal g = new GoalN(0);
        GoalStack gs = new GoalStack(g, _undoStack);
        Goal[] goals = new Goal[nbGoals];
        Goal[] emptyGoals = new Goal[nbGoals];
        ChoicePointLabel[] labels = new ChoicePointLabel[nbGoals];

        int nbChoicePoint = 0;

        // initialization of GoalStack
        for (int i = 0; i < nbGoals; i++) {
            if ((i % 10) == 0) {
                emptyGoals[nbChoicePoint] = new EmptyGoal(nbChoicePoint);
                goals[i] = new GoalN(i);
                labels[nbChoicePoint] = C.createChoicePointLabel();
                gs.setChoicePoint(goals[i], emptyGoals[nbChoicePoint], labels[nbChoicePoint]);
                try {
                    goals[i].execute();
                } catch (Failure f) {
                    fail("test failed");
                }
                nbChoicePoint++;
            } else {
                goals[i] = new GoalN(i);
                gs.pushGoal(goals[i]);
                // attach observer to the i'th variable
                // and set it's minimum to i
                try {
                    goals[i].execute();
                } catch (Failure f) {
                    fail("test failed");
                }
            }
        }

        // check whether changes have taken effect
        for (int i = 0; i < nbGoals; i++) {
            assertEquals(i, _array.get(i).min());
            assertTrue(TestUtils.contains(_array.get(i).observers(), _observers[i]));
        }

        for (int j = nbChoicePoint - 1; j > 1; j--) {
            gs.backtrack(labels[j]);
            Goal gl = gs.popGoal();
            assertEquals(j, ((EmptyGoal) gl).num());
            // check the state of the goalstack
            gl = gs.popGoal();
            assertTrue(gl.equals(goals[j * 10 - 1]));
            // check backtracking of the appropriate changes
            // restoring undoStack to the state befor setting j'th choice point
            // undone all changes being executed after setting j'th choice point
            for (int i = nbGoals - 1; i > (j * 10 - 1); i--) {
                assertEquals(0, _array.get(i).min());
                assertTrue(!TestUtils.contains(_array.get(i).observers(), _observers[i]));
            }
            // other variables remain changed
            for (int i = 0; i <= (j * 10 - 1); i++) {
                assertEquals(i, _array.get(i).min());
                assertTrue(TestUtils.contains(_array.get(i).observers(), _observers[i]));
            }
        }

        gs.backtrack(labels[nbChoicePoint - 1]);
        assertTrue(_undoStack.empty());
        for (int i = 0; i < nbGoals; i++) {
            assertEquals(0, _array.get(i).min());
            assertTrue(!TestUtils.contains(_array.get(i).observers(), _observers[i]));
        }
        assertTrue(gs.empty());
    }
}