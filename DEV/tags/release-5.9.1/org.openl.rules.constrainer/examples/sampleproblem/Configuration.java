package sampleproblem;

import java.util.*;
import org.openl.ie.constrainer.*;

public class Configuration
{
  static Constrainer C = new Constrainer("Configuration problem");
  static public class Data {
    static final int PRICE = 2;
    static final int CONNECTORS = 1;
    static final int POWER = 0;
    static final int DEMANDS = 1;

    public int nbRackTypes = 2;
    public int nbCardTypes = 4;
    public int nbRacks = 5;
    public int nbCards[] = {10, 4, 2, 1}; //amount if cards of each type
    public int maxConnectors = 16;
    public int racks[][] = {{0,150,200},{0,8,16},{0,150,200}};
    public int cards[][] = {{20,40,50,75},{10,4,2,1}};

    public IntArray prices = new IntArray(C, racks[PRICE]);
    public IntArray rackPower = new IntArray(C, racks[POWER]);
    public IntArray rackConnectors = new IntArray (C, racks[CONNECTORS]);

  } //end of Data definition

  static Data data = new Data();


  static public class Rack {
     IntVar type = C.addIntVar(0,data.nbRackTypes);
     IntExpArray cards = new IntExpArray(C,data.nbCardTypes,0,data.maxConnectors,"Storage");
     IntExp price = data.prices.elementAt(type);
     IntExp power = data.rackPower.elementAt(type);
     IntExp connectors = data.rackConnectors.elementAt(type);
     Rack(Constrainer c){
       try{
          c.postConstraint(C.scalarProduct(cards,data.cards[Data.POWER]).le(power));
          c.postConstraint(cards.sum().le(connectors));
       }
       catch (Failure f){
          System.out.println("There is no solutions");
       }
    }
  }
  public Configuration()
  {
  }

  static public void main (String[] argv){
    try{
//    Configuration conf = new Configuration();
    ArrayList racks = new ArrayList();
    IntExpArray rackTypes = new IntExpArray(C,data.nbRacks);
    IntExpArray storages = new IntExpArray(C,data.nbRacks*data.nbCardTypes);
    int counter = 0;
    for (int i=0;i<data.nbRacks;i++){
      Rack rack = new Rack(C);
      rackTypes.set(rack.type,i);
      for (int j=0;j<data.nbCardTypes;j++){
        storages.set(rack.cards.elementAt(j),counter++);
      }
      racks.add(rack);
    }

    // demand constraint
    for (int j=0;j<data.nbCardTypes;j++){
      IntExp tmp = ((Rack)racks.get(0)).cards.elementAt(j);
      for (int i=1;i<racks.size();i++){
        tmp = tmp.add(((Rack)racks.get(i)).cards.elementAt(j));
      }
      C.postConstraint(tmp.eq(data.nbCards[j]));
    }


    // symmetry constraint
    for (int i=1;i<data.nbRacks;i++){
      Rack rack = (Rack)racks.get(i-1);
      Rack next_rack = (Rack)racks.get(i);
      C.postConstraint(rack.type.le(next_rack.type));
      C.postConstraint((rack.type.eq(next_rack.type)).implies(
                        rack.cards.elementAt(0).le(next_rack.cards.elementAt(0))));
    }

    // cost function
    IntExpArray prices = new IntExpArray(C,data.nbRacks);
    for (int i=0;i<racks.size();i++){
      prices.set(((Rack)racks.get(i)).price,i);
    }
    IntExp cost = prices.sum();

    /*Goal mainGoal = new GoalAnd(new GoalInstantiate((IntVar)rackTypes.elementAt(0)),
                                new GoalGenerate(((Rack)racks.get(0)).cards));
    for (int i=1;i<data.nbRacks;i++){
      mainGoal = new GoalAnd(new GoalAnd(
                                          new GoalInstantiate((IntVar)rackTypes.elementAt(i)),
                                          new GoalGenerate(((Rack)racks.get(i)).cards)
                                         ),
                              mainGoal);
    }

    Goal total = new GoalFastMinimize(mainGoal,cost);*/

    //Goal total = new GoalAnd(new GoalGenerate(rackTypes), new GoalGenerate(storages));

    Goal total = new GoalFastMinimize(new GoalAnd(
                                               new GoalGenerate(rackTypes),
                                               new GoalGenerate(storages)
                                             ),
                                    cost
                                  );

    C.printInformation();
    boolean flag = C.execute(total);
    if (flag){
      for (int i=0;i<racks.size();i++){
        if (((Rack)racks.get(i)).type.value() > 0)
        System.out.println(((Rack)racks.get(i)).cards + " of type" + ((Rack)racks.get(i)).type);
      }
      System.out.println("The total cost is " + cost);
    }
    else{
      throw new Failure();
    }
   }
   catch (Failure f){
    System.out.println("Can't find any solution");
   }
   catch(Exception ex){
      ex.printStackTrace();
   }

  }
}