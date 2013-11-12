package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: MagicSequence<br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * A magic sequence is a sequence of N+1 values (x0,x1,...,xn) such that
 * 0 will appear in the sequence x0 times, 1 will appear x1 times, 2 will
 * appear x2 times and so on.<br>
 * For example, the following sequence is a solution for N=3:<br>
 * <samp>(1,2,1,0)</samp><br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

 public class MagicSequence
{
  static Constrainer C = new Constrainer("MagicSequence");

  public static void main(String[] args)
  {
    try{
      String arg = (args.length==0)?"10":args[0];
      int N = Integer.parseInt(arg);
      IntExpArray sequence = new IntExpArray(C,N+1,0,N, "MS");
      C.distribute(sequence,sequence);
      int[] coeffs = new int[N+1];
      for (int i=0;i<coeffs.length;i++){
        coeffs[i] = i;
      }
      C.postConstraint(C.scalarProduct(sequence,coeffs).eq(N+1));

      C.printInformation();
      Goal gen = new GoalGenerate(sequence);
      boolean flag = C.execute(gen);
      if (!flag){
        throw new Failure();
      }
      for (int i = 0; i < N; i++) {
        System.out.print(sequence.get(i).value() + " ");
      }
      System.out.println();
    }
    catch (Failure f){
      System.out.println("There is no such a sequence");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}