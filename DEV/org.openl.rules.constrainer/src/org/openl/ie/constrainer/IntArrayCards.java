package org.openl.ie.constrainer;

import java.util.HashMap;

import org.openl.ie.constrainer.impl.IntEvent;
import org.openl.ie.constrainer.impl.IntExpAddArray;
import org.openl.ie.constrainer.impl.IntExpCard;

/**
 * An implementation of the array of cardinalities for the integer expressions. One can use <code>IntArrayCards</code>
 * to control the number of occurences of several values among the constrained variables in an array of constrained
 * variables. There is a simle example explaining the idea of using <code>IntExpArray</code>:
 *
 * <pre>
 *
 * Constariner C = new Constrainer('IntArrayCards_Test');
 * IntExpArray array =  new IntExpArray(C,10,0,9,'array');
 * IntArrayCards cards = new IntArrayCards(C,array);
 *
 * for (int i=0; i &amp;lt (array.max()-array.min()+1); i++){
 *    IntExp acard = cards.cardAt(i);
 *    C.postConstraint(acard.eq(1));
 * }
 * </pre>
 *
 * Here states that each integer value from the interval [0,9] must occure in the "array" exactly once. So one can
 * readily guess that the only possible solution is array[i] == i, correct to permutations of the array's elements.
 * <code>IntArrayCards</code> has the only constructor {@link #IntArrayCards(Constrainer, IntExpArray)}. After being
 * created <code>IntArrayCards</code> becomes one to perform a policy of checking the number of occurences of every
 * possible value among the constrained variables of IntExpArray using the "cards" array. "cards" is an IntExpArray
 * consisting of (array.max() - array.min() + 1) elements (array is an IntExpArray to be controlled) so that it's i-th
 * element define the cardinality of i+array.min() value. In other words, the number of occurences must be within the
 * interval [cards.get(i).min(), cards.get(i).max()]. One can gain the access to the particular card by using
 * {@link #cardAt(int)} method.
 *
 */
public final class IntArrayCards extends Observer {
    int _min, _max;

    IntExpArray _vars;
    IntExpArray _cards;

    HashMap _index_map = new HashMap();

    /**
     * Creates an array of cardinalities for an <b>IntExpArray </b> that associates itself with each elements of
     * controlled array in order to spy on changes of their domains.
     *
     * @param constrainer A particular constrainer the constructed array belongs to
     * @param vars An array to take control of
     * @throws Failure
     */
    public IntArrayCards(Constrainer constrainer, IntExpArray vars) throws Failure {
        _vars = vars;

        // constrainer.propagate();

        _min = vars.min();
        _max = vars.max();

        IntExp[] var_data = vars.data();

        for (int i = 0; i < var_data.length; ++i) {
            var_data[i].attachObserver(this);
            _index_map.put(var_data[i], new Integer(i));
        }

        int size = Math.max(1, _max - _min + 1);

        _cards = new IntExpArray(constrainer, size);

        for (int i = 0; i < size; ++i) {
            _cards.set(new IntExpCard(constrainer, _vars, _min + i), i);
        }

        // System.out.println("After build " + this);

        new IntExpAddArray(constrainer, _cards).equals(vars.size()).execute();

        // System.out.println("After equals " + this);

    }

    /**
     * Returns cards[i-array.min()]. "array" is an array to be controlled.
     *
     * @param i The number of value in the "array's" domain.
     * @return card
     */
    public IntExpCard cardAt(int i) {
        return (IntExpCard) _cards.get(i - _min);
    }

    /**
     * @return cards Array of cardinalities.
     */
    public IntExpArray cards() {
        return _cards;
    }

    /**
     *
     * @return size Size of array of cardinalities.
     */
    public int cardSize() {
        return _cards.size();
    }

    int getIndex(IntExp exp) {
        return ((Integer) _index_map.get(exp));
    }

    @Override
    public Object master() {
        return null;
    }

    @Override
    public int subscriberMask() {
        return MIN | MAX | VALUE | REMOVE;
    }

    @Override
    public String toString() {
        return "" + _cards + " card for " + _vars;
    }

    /**
     * Overrides the appropriate method of {@link Observer} class
     */
    @Override
    public void update(Subject subject, EventOfInterest event) throws Failure {
        IntEvent e = (IntEvent) event;

        // System.out.println("Update event: " + e);
        // System.out.println("++++ " + this);

        IntExp[] cards = _cards.data();

        int var_index = getIndex(e.exp());

        int type = e.type();

        int max = e.max();
        int min = e.min();

        if ((type & EventOfInterestConstants.MIN) != 0) {
            int oldmin = e.oldmin();

            for (int i = oldmin; i < min; ++i) {
                ((IntExpCard) cards[i - _min]).removeIndex(var_index);
            }
        }

        if ((type & EventOfInterestConstants.MAX) != 0) {
            int oldmax = e.oldmax();

            for (int i = oldmax; i > max; --i) {
                ((IntExpCard) cards[i - _min]).removeIndex(var_index);
            }
        }

        if ((type & EventOfInterestConstants.REMOVE) != 0) {
            int nRemoves = e.numberOfRemoves();

            for (int i = 0; i < nRemoves; ++i) {
                int removed = e.removed(i);
                if (min < removed && removed < max) {
                    ((IntExpCard) cards[removed - _min]).removeIndex(var_index);
                }
            }
        }

        if ((type & EventOfInterestConstants.VALUE) != 0) {
            ((IntExpCard) cards[min - _min]).addValueIndex(var_index);
        }

        // System.out.println("---- " + this);

    }

}
