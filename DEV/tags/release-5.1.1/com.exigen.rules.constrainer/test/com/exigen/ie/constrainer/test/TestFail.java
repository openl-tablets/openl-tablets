package com.exigen.ie.constrainer.test;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.GoalFail;

/**
 * Test that fail to find a solution is correct in execute().
 */
public class TestFail
{
  static public void main(String[] args) throws Exception
  {
    Constrainer c = new Constrainer("");

    if(c.execute(new GoalFail(c)))
    {
      System.out.println(" Incorrect");
    }
    else
    {
      System.out.println(" Correct");
    }
  }

}
