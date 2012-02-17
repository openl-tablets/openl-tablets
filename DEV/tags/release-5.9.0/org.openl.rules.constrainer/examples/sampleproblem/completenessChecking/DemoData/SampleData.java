package sampleproblem.completenessChecking.DemoData;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

public class SampleData
{
  static private Constrainer C = new Constrainer("");
  static public final int SINGLE  =0,
                   MARRIED =1,
                   MALE    =0,
                   FEMALE  =1;
  static public final IntBoolExp ANY = new IntBoolExpConst(C, true);
  static public IntVar mst = C.addIntVar(0,1, "MaritalStatus"),
         gender        = C.addIntVar(0,1, "Gender"),
         age           = C.addIntVar(20, 100, "Age"),
         amu           = C.addIntVar(0, 100, "Annual Mileage Usage");
  static public IntExpArray vars(){
    return new IntExpArray(C, mst, gender, age, amu);
  }
  static final public  IntBoolExp cells[][] =
  {
     { mst.eq(SINGLE),    gender.eq(MALE),    age.le(20),                 amu.ge(25) },
     { mst.eq(SINGLE),    gender.eq(MALE),    age.ge(21).and(age.le(24)), amu.ge(25) },
     { mst.eq(SINGLE),    gender.eq(MALE),    age.le(20),                 amu.lt(25) },
     { mst.eq(MARRIED),    gender.eq(MALE),    age.ge(21).and(age.le(24)), amu.lt(25) },
     { mst.eq(SINGLE),    gender.eq(MALE),    age.ge(25).and(age.le(29)), amu.gt(50) },
     { mst.eq(SINGLE),    gender.eq(MALE),    age.ge(30).and(age.le(49)), amu.gt(50) },

     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.le(20),                 amu.ge(25) },
     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.ge(21).and(age.le(24)), amu.ge(25) },
     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.le(20),                 amu.lt(25) },
     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.ge(21).and(age.le(24)), amu.lt(25) },
     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.ge(25).and(age.le(29)), amu.gt(50) },
     { mst.eq(SINGLE),    gender.eq(FEMALE),  age.ge(30).and(age.le(49)), amu.gt(50) },

     { mst.eq(MARRIED),   gender.eq(MALE),    age.le(20),                 ANY        },
     { mst.eq(MARRIED),   gender.eq(MALE),    age.ge(21).and(age.le(24)), ANY        },

     { mst.eq(MARRIED),   gender.eq(MALE),    age.ge(25).and(age.le(49)), ANY        },
     { mst.eq(MARRIED),   gender.eq(FEMALE),  age.le(49),                 ANY        },
     { mst.eq(SINGLE) ,   ANY,                age.ge(25).and(age.le(49)), amu.le(50) },

     { ANY,   ANY,   age.ge(50).and(age.le(64))    ,ANY },
     { ANY,   ANY,   age.ge(65).and(age.le(70))    ,ANY },
     { ANY,   ANY,   age.ge(71).and(age.le(75))    ,ANY },
     { ANY,   ANY,   age.ge(76).and(age.le(80))    ,ANY },
     { ANY,   ANY,   age.ge(81)                    ,ANY },
  };
  static public String[] names = {"B1-Male Basic 1","B2-Male Basic 2","R1-Male Restricted 1",
  "R2-Male Restricted 2","S1-Single Male 1",
  "S2-Single Male 2","GB1-Female Basic 1","GB2-Female Basic 2","GR1-Female Restricted 1",
  "GR2-Female Restricted 2","GS1-Single Female 1","GS2-Single Female 2",
    "M1-Married 1","M2-Married 2","A-Adult","A-Adult" ,"A-Adult","MA1-Mature Adult 1",
  "MA2-Mature Adult 2","MA3-Mature Adult 3","MA4-Mature Adult 4","MA5-Mature Adult 5"};

 private SampleData()
 {
 }
}