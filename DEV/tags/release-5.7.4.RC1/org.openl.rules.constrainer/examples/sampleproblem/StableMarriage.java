package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: StableMarriage<br>
 * Description: (was taken from ILOG Solver User Manual)<br>
 * This widely studied problem is usually formulated like this: a
 * group of men and women must marry.  Each of those people knows
 * how to quantify his or her preferences in a mate.  They also
 * know how to rank the candidates for spouse in a list ordered by
 * preference.  The first name on such a list represents the
 * candidate that the owner of the list most prefers as a spouse.
 * The last such name indicates his or her least desirable spouse
 * among the possible candidates.<br>
 * The point of the problem is to create couples with the least
 * risk of separation after marriage.  What contributes to risk
 * of separation? Or, to put the question another way, what makes
 * stable marriages?<br>
 * One factor is whether each person is matched with his or her
 * first choice.  Clearly, if both partners in a couple prefer
 * each other to any other candidate, then their marriage should
 * be stable.<br>
 * A second factor is whether a person matched with anyone other
 * than his or her first choice prefers some third person who also
 * prefers him or her.  Perhaps a few names will clarify this point.
 * Let's say that Romeo is matched with Isolde, and Tristan with
 * Juliette, but as we all know, Romeo prefers Juliette to Isolde,
 * and Juliette prefers Romeo to Tristan.  These two couples are
 * consequently unstable and risk separating.<br>
 * In more or less formal terms, we say that a couple, A and B, is
 * stable if one of these conditions holds:
 * <UL>
 *   <LI>
 *     A is the first choice of B, and B is also the first choice of A.
 *   </LI>
 *   <LI>
 *     If B is not the first choice of A, then for every person C that
 *     A prefers to B, C still prefers someone else more than A. (In
 *     colloquial terms, A may be unhappy but stuck with the match!)
 *   </LI>
 * </UL>
 * Those conditions will be represented in the problem as constraints
 * on the assignment of spouses.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class StableMarriage
{
  static final Constrainer C = new Constrainer("StableMarriage");
  // women
  static final int Helen = 0;
  static final int Tracy = 1;
  static final int Linda = 2;
  static final int Sally = 3;
  static final int Wanda = 4;
  // men
  static final int Richard = 0;
  static final int James = 1;
  static final int John = 2;
  static final int Hugh = 3;
  static final int Greg =4;

  static final String womansNames[] = {"Helen","Tracy","Linda","Sally","Wanda"};
  static final String mansNames[] = {"Richard","James","John","Hugh","Greg"};

  static int nbMen = 5;

  static int[][] mansPreferences = {{Tracy,Linda,Wanda,Sally,Helen},
                             {Tracy,Sally,Linda,Helen,Wanda},
                             {Wanda,Linda,Tracy,Sally,Helen},
                             {Helen,Wanda,Sally,Linda,Tracy},
                             {Sally,Linda,Tracy,Helen,Wanda}};

  static int[][] womansPreferences = {{Richard, James, Hugh, John, Greg},
                               {John, Hugh, Richard, Greg, James},
                               {Hugh, John, Greg, James, Richard},
                               {Richard, Greg, James, Hugh, John},
                               {Greg, James, John, Richard, Hugh}};

  static IntExpArray men = new IntExpArray(C,nbMen,0,nbMen-1,"Men");
  static IntExpArray women = new IntExpArray(C,nbMen,0,nbMen-1,"Women");

  static IntBoolExp manIsGladOrHumble(int man, int woman){
    int i = 0;
    IntBoolExp result = new IntBoolExpConst(C, true);
    while (mansPreferences[man][i]!= woman){
      int anotherWoman = mansPreferences[man][i];
      int j = nbMen - 1;
      while (womansPreferences[anotherWoman][j]!=man){
        int anotherMan = womansPreferences[anotherWoman][j];
        result = result.and(women.elementAt(anotherWoman).ne(anotherMan));
        j--;
      }
      i++;
    }
    return result;
  }

  static IntBoolExp womanIsGladOrHumble(int woman, int man){
    int i = 0;
    IntBoolExp result = new IntBoolExpConst(C, true);
    while (womansPreferences[woman][i]!= man){
      int anotherMan = womansPreferences[woman][i];
      int j = nbMen - 1;
      while (mansPreferences[anotherMan][j]!=woman){
        int anotherWoman = mansPreferences[anotherMan][j];
        result = result.and(men.elementAt(anotherMan).ne(anotherWoman));
        j--;
      }
      i++;
    }
    return result;
  }

  public StableMarriage()
  {
  }

  public static void main(String[] argv){
    try{
      for (int i=0;i<men.size();i++){
        for (int j=0;j<women.size();j++){
          C.postConstraint(men.elementAt(i).eq(j).implies(
                    manIsGladOrHumble(i,j).and(women.elementAt(j).eq(i))));
        }
      }

      for (int i=0;i<men.size();i++){
        for (int j=0;j<women.size();j++){
          C.postConstraint(women.elementAt(i).eq(j).implies(
                    womanIsGladOrHumble(i,j).and(
                    men.elementAt(j).eq(i))));
        }
      }

      C.postConstraint(C.allDiff(men));
      C.postConstraint(C.allDiff(women));
      Goal gen = new GoalAnd(new GoalGenerate(men),
                             new GoalGenerate(women));
      boolean flag = C.execute(gen);
      if (!flag){
        throw new Failure();
      }
      for (int i=0;i<men.size();i++){
        System.out.println(mansNames[i] + " - " + womansNames[men.elementAt(i).value()]);
      }

    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}