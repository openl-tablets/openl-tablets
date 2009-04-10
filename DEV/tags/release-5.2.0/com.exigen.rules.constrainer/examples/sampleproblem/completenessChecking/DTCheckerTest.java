package sampleproblem.completenessChecking;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import sampleproblem.completenessChecking.DemoData.SampleData;

public class DTCheckerTest
{

  public static void main(String[] args)
  {
    DecisionTable tbl = new DTCheckerImpl.DecisionTableImpl(SampleData.cells,
        SampleData.vars());

    DTChecker monitor = new DTCheckerImpl(tbl);
    System.out.println(monitor.checkCompleteness());
    System.out.println(monitor.checkOverlappings());
  }
}