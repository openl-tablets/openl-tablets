package org.openl.tablets.tutorial6.manners;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.GoalAnd;
import com.exigen.ie.constrainer.GoalGenerate;
import com.exigen.ie.constrainer.GoalSaveArrayResult;
import com.exigen.ie.constrainer.IntArray;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.impl.ConstraintAllDiff;

public class MannersSolver
{

    /**
     * 
     * @param sexes - array of guest sexes [0..1]
     * @param hobbies - array of hobbies (bitmasks)
     * @return arranged indexes
     * @throws Failure 
     */
    public int[] solve(int[] sexes, int[] hobbies) throws Failure
    {
	
	Constrainer c = new Constrainer("Manners");
	int size = sexes.length;
	IntExpArray seats = new IntExpArray(c, size,0, size-1, "seat", IntVar.DOMAIN_BIT_SMALL);
	
	c.postConstraint(new ConstraintAllDiff(seats));
	
	IntArray sexAry =  new IntArray(c, sexes);
	IntArray hobbyAry =  new IntArray(c, hobbies);
	
	for (int i = 0; i < size; i++)
	{
	    int prevSeatIndex = i == 0 ? size - 1 : i - 1;
	    
	    IntExp curGuest = seats.elementAt(i);
	    IntExp prevGuest = seats.elementAt(prevSeatIndex);
	    
	    //different sexes
	    c.postConstraint(sexAry.elementAt(curGuest).ne(sexAry.elementAt(prevGuest)));
	    //at least one common hobby constraint
	    c.postConstraint(hobbyAry.elementAt(curGuest).bitAnd(hobbyAry.elementAt(prevGuest)).ne(0));
	}
	
	GoalGenerate generate = new GoalGenerate(seats);
	GoalSaveArrayResult save = new GoalSaveArrayResult(c, seats);
	c.execute(new GoalAnd(generate, save));
	return save.getFirstResult();
    }
    
    
}
