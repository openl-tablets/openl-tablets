package org.openl.tablets.tutorial6.sudoku;

import java.util.Random;

import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.IntVarSelector;
import com.exigen.ie.constrainer.IntVarSelectorMinSize;
import com.exigen.ie.constrainer.IntVarSelectorMinSizeMin;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

/**
 * An implementation of the IntVarSelector interface
 * that selects the first unbound variable.
 *
 * @see IntVarSelector
 * @see IntVarSelectorMinSize
 * @see IntVarSelectorMinSizeMin
 * @see GoalGenerate
 */
public class IntVarSelectorFirstUnboundRandom implements IntVarSelector
{
  private IntExpArray 	  _intvars;

  /**
   * Constructor from the IntExpArray.
   */
  public IntVarSelectorFirstUnboundRandom(IntExpArray intvars)
  {
    _intvars = intvars;
  }
  
  Random r = new Random();

  int max = -1;
  long nc = 0;
  int start = 0; 

  /**
   * Selects the first unbound variables in the vector _intvars.
   */
  public int select()
  {
    int size = _intvars.size();
    
    
    for(int i=0; i < size; i++)
    {
      int n = (start + i)%size;	
      IntVar vari = (IntVar)_intvars.elementAt(n);
      if (!vari.bound())
      {
	if (i > max)
	    max = i;
	
	++nc;
	if (nc % 1000 == 0)
	{    
	    start = (int)(r.nextDouble() * size);
	    System.out.println(max + " : " + nc + " : " + n + " : " + i + " : " + start);
	}    
        return n;
      }  
    }
    return -1;
  }

} // ~IntVarSelectorFirstUnbound
