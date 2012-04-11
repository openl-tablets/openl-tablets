package sampleproblem;

import org.openl.ie.constrainer.*;

/**
 * Title: FamilyRiddle <br>
 * Description: (was taken from ILOG Solver User Guide)<br>
 * Let's assume that Rene and Leo are both heads of household,
 * and--what a coincidence---both families include three girls
 * and three boys.  The youngest child in Leo's family is a girl,
 * and in Rene's family, a little girl has just arrived.  In
 * other words, there is a girl in Rene's family whose age is
 * less than one year.  Neither family includes any twins, nor
 * any children closer in age than a year.<br>
 * <br>
 * All the children are under age ten.  In each family, the sum
 * of the ages of the girls is equal to the sum of the ages of
 * the boys; in fact, the sum of the squares of the ages of the
 * girls is equal to the sum of the squares of the ages of the
 * boys.  The sum of the ages of all these children is 60.<br>
 * <br>
 * Here's our riddle: what are the ages of the children in these
 * two families?<br>
 * <br>
 * Copyright: Copyright (c) 2002<br>
 * Company: Exigen Group<br>
 * @author Tseitlin Eugeny
 * @version 1.0
 */

public class FamilyRiddle
{
  static Constrainer C = new Constrainer("FamilyRiddle");

  static private class ScalarProduct{
    IntExpArray array1 = null;
    IntExpArray array2 = null;
    ScalarProduct(IntExpArray array1, IntExpArray array2){
      this.array1 = array1;
      this.array2 = array2;
    }

    IntExp getProduct(){
      int size1 = array1.size();
      int size2 = array2.size();
      if (size1 != size2)
        throw new RuntimeException("scalarProduct parameters have different size");

      IntExpArray products = new IntExpArray(C, size1);
      for(int i=0; i < size1; i++)
      {
        products.set(array1.elementAt(i).mul(array2.elementAt(i)), i);
      }
      return products.sum();
    }
  } // end of ScalarProduct definition

  static public class Family{
    public String father = null;
    public IntExpArray daughters = new IntExpArray(C,3,0,9,"girls");
    public IntExpArray sons = new IntExpArray(C,3,0,9,"boys");
    public IntExpArray children = new IntExpArray(C,6);

    public Family(String father){
      this.father = father;
      for (int i=0;i<sons.size();i++){
        children.set(daughters.elementAt(i),i);
        children.set(sons.elementAt(i),i+sons.size());
      }
      addConstraints();
    }


    private void addConstraints(){
      // there are no twins
      C.addConstraint(C.allDiff(children));
      // the sum of the ages of the girls is equal to the sum of the ages of the boys
      C.addConstraint(C.sum(sons).eq(C.sum(daughters)));
      // the sum of the squares of the ages of the girls
      // is equal to
      // the sum of the squares of the ages of the boys
      C.addConstraint((new ScalarProduct(sons,sons).getProduct()).eq(
                        new ScalarProduct(daughters,daughters).getProduct()));
    }

    public Goal instantiate(){
      return (new GoalGenerate(children));
    }

    public String toString(){
      return ("" + father + "'s family: " + "\nsons: " + sons + "\ndaughters: " + daughters + "\n");
    }

  }

  public FamilyRiddle()
  {
  }
  public static void main(String[] args)
  {
    try{
      Family Leos_family = new Family("Leo");
      Family Renes_family = new Family("Rene");

      // in Rene's family, a little girl has just arrived
      C.addConstraint(Renes_family.daughters.elementAt(0).eq(0));
      //The youngest child in Leo's family is a girl

      for (int i=1;i<Leos_family.children.size();i++){
        C.addConstraint(Leos_family.daughters.elementAt(0).lt(
                         Leos_family.children.elementAt(i)));
      }
      // The sum of the ages of all these children is 60
      C.addConstraint(((Renes_family.children.sum()).add(Leos_family.children.sum())).eq(60));

      C.postConstraints();

      C.execute(new GoalAnd(Renes_family.instantiate(),
                            Leos_family.instantiate()));

      System.out.println("\n" + Leos_family +"\n"+ Renes_family);

    }
    catch(Failure ex){
      System.out.println("there is no solution");
      System.out.println(ex);
    }
    catch(Exception ex){
      ex.printStackTrace();
    }

  }
}