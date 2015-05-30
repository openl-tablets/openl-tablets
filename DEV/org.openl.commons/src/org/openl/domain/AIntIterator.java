/**
 * Created Jul 14, 2007
 */
package org.openl.domain;

import java.util.NoSuchElementException;

import org.openl.util.AOpenIterator;

/**
 * @author snshor
 * 
 */
public abstract class AIntIterator extends AOpenIterator<Integer> implements IIntIterator {

    private static final IIntIterator INT_EMPTY = new IntEmptyIterator();


	public Integer next() {
        return nextInt();
    }

    public IIntIterator select(IIntSelector selector) {
        return new IIntSelector.IntSelectIterator(this, selector);
    }
    
    
    static public IIntIterator fromValue(int... values)
    {
    	int len = values.length;
		if (len  == 0)
    		return INT_EMPTY;
    	if (len == 1)
    		return new SingleIntIterator(values[0]);
    	return new IntArrayIterator(values);
    }

    
    
    static final class SingleIntIterator extends AIntIterator {
    	
        int value;
        boolean hasNext = true;

        SingleIntIterator(int value) {
            this.value = value;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public int nextInt() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasNext = false;
            return value;
        }


        @Override
        public final int size() {
            return hasNext ? 1 : 0;
        }

		@Override
		public boolean isResetable() {
			return true;
		}

		@Override
		public void reset() {
			hasNext = true;
		}

    }
    
    
    static final class IntEmptyIterator extends AIntIterator
    {

		@Override
		public int nextInt() {
            throw new NoSuchElementException();
		}

		@Override
		public boolean isResetable() {
			return true;
		}

		@Override
		public void reset() {
		}

		@Override
		public boolean hasNext() {
			return false;
		}
		
	    public int size() {
	        return 0;
	    }

    	
    }
    
    
}
