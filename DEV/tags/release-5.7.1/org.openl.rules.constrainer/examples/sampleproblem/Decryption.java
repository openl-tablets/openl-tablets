package sampleproblem;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

/**
 * Title: Decryption<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * Solve the cryptarithm:<br>
 * <br><samp>
 * &nbsp;&nbsp;&nbsp; D O N A L D<br>
 * &nbsp;           + G E R A L D<br>
 * &nbsp;           -------------<br>
 * &nbsp;&nbsp;&nbsp; R O B E R T<br>
 * </samp><br>
 * where each letter represents a different digit.<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class Decryption
{
  static Constrainer C = new Constrainer("Decryption");

  static IntExpArray letters = new IntExpArray(C,18);
  static IntBoolExp ge10 (int i){
    if (i<0)
      return new IntBoolExpConst(C,false);
    return letters.get(i).add(letters.get(i+LENGTH)).ge(10);
  }
  static IntBoolExp lt10 (int i){
    if (i<0)
      return new IntBoolExpConst(C,false);
    return letters.get(i).add(letters.get(i+LENGTH)).lt(10);
  }
  static int LENGTH = 6;
  public Decryption()
  {
  }
  public static void main(String[] args)
  {
    try{

    /*   D O N A L D
    *  + G E R A L D
    *  ----------------
    *  = R O B E R T */
    IntVar D = C.addIntVar(1,9,"D",IntVar.DOMAIN_BIT_FAST);
    IntVar L = C.addIntVar(0,9,"L",IntVar.DOMAIN_BIT_FAST);
    IntVar A = C.addIntVar(0,9,"A",IntVar.DOMAIN_BIT_FAST);
    IntVar N = C.addIntVar(0,9,"N",IntVar.DOMAIN_BIT_FAST);
    IntVar O = C.addIntVar(0,9,"O",IntVar.DOMAIN_BIT_FAST);
    IntVar R = C.addIntVar(1,9,"R",IntVar.DOMAIN_BIT_FAST);
    IntVar E = C.addIntVar(0,9,"E",IntVar.DOMAIN_BIT_FAST);
    IntVar G = C.addIntVar(1,9,"G",IntVar.DOMAIN_BIT_FAST);
    IntVar T = C.addIntVar(0,9,"T",IntVar.DOMAIN_BIT_FAST);
    IntVar B = C.addIntVar(0,9,"B",IntVar.DOMAIN_BIT_FAST);
    int next = 0;
    IntExpArray vocabulary = new IntExpArray(C,10);
    vocabulary.set(D,next++); vocabulary.set(L,next++); vocabulary.set(A,next++); vocabulary.set(N,next++);
    vocabulary.set(O,next++); vocabulary.set(R,next++); vocabulary.set(E,next++); vocabulary.set(G,next++);
    vocabulary.set(T,next++); vocabulary.set(B,next++);
    C.postConstraint(C.allDiff(vocabulary));
    next = 0;
    letters.set(D,next++); letters.set(L,next++); letters.set(A,next++);
    letters.set(N,next++); letters.set(O,next++); letters.set(D,next++);
    letters.set(D,next++); letters.set(L,next++); letters.set(A,next++);
    letters.set(R,next++); letters.set(E,next++); letters.set(G,next++);
    letters.set(T,next++); letters.set(R,next++); letters.set(E,next++);
    letters.set(B,next++); letters.set(O,next++); letters.set(R,next++);


    System.out.println("Start!");
    for (int i = 0; i<6;i++){
      IntBoolExp first =
          lt10(i).and(letters.get(i).add(letters.get(i+LENGTH)).add(ge10(i-1)).eq(letters.get(i+2*LENGTH)));
      IntBoolExp second =
          ge10(i).and(letters.get(i+2*LENGTH).eq(
                                  letters.get(i).add(letters.get(i+LENGTH).add(ge10(i-1)).sub(10))));
      C.postConstraint(first.or(second));
    }
    C.postConstraint(letters.get(5).add(letters.get(11)).add(ge10(4)).le(9));

    C.printInformation();
    Goal gen = new GoalGenerate(letters);
    C.execute(gen);
    for (int i=5;i>=0;i--){
      System.out.print(letters.get(i).value());
    }
    System.out.println();
    for (int i=11;i>=6;i--){
      System.out.print(letters.get(i).value());
    }
    System.out.println();
    for (int i=17;i>=12;i--){
      System.out.print(letters.get(i).value());
    }

    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}