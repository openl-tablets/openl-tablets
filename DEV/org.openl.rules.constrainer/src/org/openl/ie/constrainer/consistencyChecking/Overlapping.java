package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: Auxiliary class using to store information about overlapping rules</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.consistencyChecking.DTChecker.Utils;


public class Overlapping {
    
    /**
     * 
     * @author snshor
     * 
     * For rules A and B, A fires first and B - second.
     * 
     * BLOCK (0x01),  // this is a very bad status, it means that rule A completely blocks rule B, so B can never be true. This is definitely an erroneous behavior.
     * PARTIAL(0x02), // A and B partially overlap, this could either be an error or an intended behavior. 
     * OVERRIDE(0x04); // B overrides A, this is always the case for default rules, such as "all the rest". In most cases this is an expected behavior
     *
     */
    
    public enum OverlappingStatus
    {
        BLOCK (0x01),  
        PARTIAL(0x02),  
        OVERRIDE(0x04); 
        
        
        OverlappingStatus(int bit)
        {
            this.bit = bit;
        }
        int bit;
        
        
        
        public int getBit() {
            return bit;
        }

    }
    
    
    private List<Integer> _overlapped = null;

    protected String[] _solutionNames = null;
    protected int[] _solutionValues = null;
    
    private OverlappingStatus status;

    public OverlappingStatus getStatus() {
        return status;
    }

    Overlapping(IntExpArray solution) {

        _solutionNames = Utils.IntExpArray2Names(solution);
        _solutionValues = Utils.IntExpArray2Values(solution);
    }

    Overlapping(Overlapping ovl, int i, int j, OverlappingStatus status) {
        this.status = status;
        _solutionNames = ovl.getSolutionNames();
        _solutionValues = ovl.getSolutionValues();
        addRule(i);
        addRule(j);
    }

    void addRule(int num) {
        if (_overlapped == null) {
            _overlapped = new ArrayList<Integer>();
        }
        _overlapped.add(num);
    }

    /**
     * @return an amount of rules being satisfied with the solution returned by
     *         <code>getSolution()</code>
     * @see #getSolution()
     */
    public int amount() {
        return _overlapped.size();
    }

    /**
     * @return an array of numbers of rules overlapping in the decision table
     */
    public int[] getOverlapped() {
        int[] arr = new int[_overlapped.size()];
        Iterator<Integer> iter = _overlapped.iterator();
        int i = 0;
        while (iter.hasNext()) {
            arr[i++] = iter.next();
        }
        return arr;
    }

    /**
     *
     * @return the solution satisfying two or more rules in the form of a
     *         HashMap of names of the variables associated with the according
     *         values
     */
    public Map<String, Integer> getSolution() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < _solutionNames.length; i++) {
            map.put(_solutionNames[i], _solutionValues[i]);
        }
        return map;
    }

    public String[] getSolutionNames() {
        return _solutionNames;
    }

    public int[] getSolutionValues() {
        return _solutionValues;
    }

    @Override
    public String toString() {
        String rep = "{ solution: " + getSolution() + " obeys the following rules: " + _overlapped + "}";
        return rep;
    }
}