/*
 * Created on Sep 4, 2003
 *
 *  Copyright Intelligent ChoicePoint Inc. 2003
 */

package zebra;

import java.util.Vector;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalFail;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalPrint;
import org.openl.ie.constrainer.GoalPrintSolutionNumber;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.impl.ConstraintAllDiff;

/**
 * @author snshor
 *  "There are five houses, each of a different color, inhabited by men of"  crlf
    "different nationalities, with different pets, drinks, and cigarettes."  crlf
    crlf
    "The Englishman lives in the red house.  The Spaniard owns the dog."     crlf
    "The ivory house is immediately to the left of the green house, where"   crlf
    "the coffee drinker lives.  The milk drinker lives in the middle house." crlf
    "The man who smokes Old Golds also keeps snails.  The Ukrainian drinks"  crlf
    "tea.  The Norwegian resides in the first house on the left.  The"       crlf)
   (printout t
    "Chesterfields smoker lives next door to the fox owner.  The Lucky"      crlf
    "Strike smoker drinks orange juice.  The Japanese smokes Parliaments."   crlf
    "The horse owner lives next to the Kools smoker, whose house is yellow." crlf
    "The Norwegian lives next to the blue house."			     crlf
    crlf
    "Now, who drinks water?  And who owns the zebra?" crlf crlf)

 *
 */
public class Zebra
{
	
	final String[] cigNames =
	{
		"Old Golds",
		"Chesterfields",
		"Lucky Strike",
		"Parliaments",
		"Kools"
	};

	final String[] drinkNames =
	{
		"Coffee",
		"Milk",
		"Tea",
		"OrangeJuice",
		"Water",
	};

	final String[] petNames =
	{
		"Dog",
		"Snails",
		"Fox",
		"Horse",
		"Zebra",
	};

	final String[] peopleNames =
	{
		"Englishman",
		"Spaniard",
		"Ukrainian",
		"Norwegian",
		"Japanease",
	};

	final String[] houseNames =
	{
		"h0",
		"h1",
		"h2",
		"h3",
		"h4",
	};


	final String[] colorNames =
	{
		"Red",
		"Ivory",
		"Green",
		"Yellow",
		"Blue",
	};


	
  static final int 
    
    OldGolds = 0,
    Chesterfields = 1,
    LuckyStrike = 2,
    Parliaments = 3,
    Kools = 4,
    
    Coffee = 0,
    Milk = 1,
    Tea = 2,
    OrangeJuice = 3,
    Water = 4,

    Dog = 0,
    Snails = 1,
    Fox = 2,
    Horse = 3,
    Zebra = 4,

    Englishman = 0,
    Spaniard = 1,
    Ukrainian = 2,
    Norwegian = 3,
    Japanease = 4,

    Red = 0,
    Ivory = 1,
    Green = 2,
    Yellow = 3,
    Blue = 4,
    
    nPeople = 5,
    nPets = 5,
    nHouses = 5,
    nDrinks = 5,
    nColors = 5,
    nCigs = 5;

  Constrainer c;
  
  Vector allvars = new Vector();

  IntExpArray livesIn, owns, smokes, hasColor, drinks;

	void printRel(String[] subjects, IntExpArray rel, String[] objects) throws Failure
	{
		for (int i = 0; i < rel.size(); i++)
    {
      System.out.println(subjects[i] +  "\t" + rel.name() + "\t" + objects[rel.elementAt(i).value()]);
    }
	}
	
	
	void printAll() throws Failure
	{
		printRel(peopleNames, livesIn, houseNames);
		printRel(houseNames, hasColor, colorNames);
		printRel(peopleNames, smokes, cigNames);
		printRel(peopleNames, owns, petNames);
		printRel(peopleNames, drinks, drinkNames);
	}
	
	



  void say(IntExp subject, IntExpArray rel, IntExp obj) throws Failure
  {
    c.addConstraint(rel.elementAt(subject).eq(obj));
  }

  void say(IntExp subject, IntExpArray rel, int obj) throws Failure
  {
    c.addConstraint(rel.elementAt(subject).eq(obj));
  }

  void say(int subject, IntExpArray rel, IntExp obj) throws Failure
  {
    c.addConstraint(rel.elementAt(subject).eq(obj));
  }

  void say(int subject, IntExpArray rel, int obj) throws Failure
  {
    c.addConstraint(rel.elementAt(subject).eq(obj));
  }

  IntVar who(IntExpArray rel, int value) throws Failure
  {
    return what(rel, value);
  }

  IntVar what(IntExpArray rel, int value) throws Failure
  {
    IntVar x = c.addIntVar(0, rel.size() - 1);
    c.addConstraint(rel.elementAt(x).eq(value));

    addVar(x);

    return x;

  }

  IntExp what(IntExp subj, IntExpArray rel) throws Failure
  {
    return rel.elementAt(subj);
  }

  IntExp what(int subj, IntExpArray rel) throws Failure
  {
    return rel.elementAt(subj);
  }

  IntExp where(int subj, IntExpArray rel) throws Failure
  {
    return rel.elementAt(subj);
  }

  IntExp where(IntExp subj, IntExpArray rel) throws Failure
  {
    return rel.elementAt(subj);
  }

  void addVar(IntVar v)
  {
  	allvars.add(v); 
  }

  IntExpArray definePropertyArray(int size, int domainSize, String name)
  {
    IntExpArray ia = new IntExpArray(c, size, 0, domainSize - 1, name);
    
    
    c.addConstraint(new ConstraintAllDiff(ia));
    
    for (int i = 0; i < size; i++)
    {
			allvars.add(ia.elementAt(i));      
    }
    
    return ia;
  }

  void initializePorblem()
  {
    c = new Constrainer("zebra");

    livesIn = definePropertyArray(nPeople, nHouses, "livesIn");
    owns = definePropertyArray(nPeople, nPets, "owns");
    smokes = definePropertyArray(nPeople, nCigs, "smokes");
    drinks = definePropertyArray(nPeople, nDrinks, "drinks");
    hasColor = definePropertyArray(nHouses, nColors, "hasColor");

  }

  void nextDoor(IntExp house1, IntExp house2)
  {
    c.addConstraint(house1.sub(house2).abs().equals(1));
  }

  public void defineProblem() throws Failure
  {

    //		"The Englishman lives in the red house.  

    say(Englishman, livesIn, what(hasColor, Red));

    //		The Spaniard owns the dog."     

    say(Spaniard, owns, Dog);

    //		"The ivory house is immediately to the left of the green house, where"   crlf
    //		"the coffee drinker lives.  " +

    IntVar ivoryHouse = what(hasColor, Ivory);
    IntVar greenHouse = what(hasColor, Green);

    IntVar coffeeDrinker = who(drinks, Coffee);

    say(coffeeDrinker, livesIn, greenHouse);

    c.addConstraint(ivoryHouse.add(1).eq(greenHouse));

    //		"The milk drinker lives in the middle house." crlf

    say(who(drinks, Milk), livesIn, 2);

    //"The man who smokes Old Golds also keeps snails. 

    say(who(smokes, OldGolds), owns, Snails);

    //		The Ukrainian drinks"  "tea.

    say(Ukrainian, drinks, Tea);
    //The Norwegian resides in the first house on the left.
    say(Norwegian, livesIn, 0);

    // Chesterfields smoker lives next door to the fox owner.

    nextDoor(
      where(who(smokes, Chesterfields), livesIn),
      where(who(owns, Fox), livesIn));

    //  The Lucky Strike smoker drinks orange juice.

    say(who(smokes, LuckyStrike), drinks, OrangeJuice);

    //		  The Japanese smokes Parliaments.

    say(Japanease, smokes, Parliaments);

    // "The horse owner lives next to the Kools smoker, whose house is yellow." 

    IntVar yellowHouse;

    say(who(smokes, Kools), livesIn, yellowHouse = what(hasColor, Yellow));

    nextDoor(where(who(owns, Horse), livesIn), yellowHouse);

    //		"The Norwegian lives next to the blue house."	

    nextDoor(where(Norwegian, livesIn), what(hasColor, Blue));

  }

  
  public class Print extends GoalImpl
  {
    public Print(Constrainer constrainer, String s)
    {
      super(constrainer, s);
    }

    /**
     * Prints the object to System.out.
     */
    public Goal execute() throws Failure
    {
	printAll(); return null;    
    }

  } // ~GoalDisplay  
	
  
  void solveProblem() throws Failure
	{
		c.postConstraints();
		
		IntExpArray vars = new IntExpArray(c, allvars);
		Goal search_goal = new GoalGenerate(vars);
		
		Goal print_goal_vars = new GoalPrint(vars,"\nVars",true);
		IntExpArray results = new IntExpArray(c, allvars);
		
//		GoalDoSomething.SomeThing nice_printer = new GoalDoSomething.SomeThing()
//		{
//			public Goal doSomething() throws Failure{ printAll(); return null;}
//		};
//		
//		
		Goal goal_print_all = new Print(c,"ZZZ");
		

		
		
		
		Goal print_goal_results = new GoalPrint(results,"Victories, Loses, Draws",true);
		Goal goal = new GoalAnd(search_goal,
														new GoalPrintSolutionNumber(c),
														print_goal_vars
														,goal_print_all,
														new GoalFail(c)
														);
		c.executeAll(goal);
		
		
		
		
	}
  
  void run() throws Failure
  {
  	initializePorblem();
  	defineProblem();
  	solveProblem();
  	
  	
  	
  }
  
  public static void main(String[] args) throws Failure
  {
    Zebra z = new Zebra();
    z.run();
  }
  

}
