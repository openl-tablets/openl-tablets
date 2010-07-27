package org.openl.tablets.tutorial6.manners;

import java.util.ArrayList;

public class Guest implements Comparable<Guest>
{
    String name;
    String sex;
    ArrayList<String> hobbies = new ArrayList<String>();
    
    
    int originalIndex;
    
    public int getOriginalIndex()
    {
        return originalIndex;
    }
    public void setOriginalIndex(int originalIndex)
    {
        this.originalIndex = originalIndex;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public String getSex()
    {
        return sex;
    }
    public void setSex(String sex)
    {
        this.sex = sex;
    }
    public ArrayList<String> getHobbies()
    {
        return hobbies;
    }
    public void setHobbies(ArrayList<String> hobbies)
    {
        this.hobbies = hobbies;
    }
    public int compareTo(Guest o)
    {
	return originalIndex - o.originalIndex;
    }
    
    
}
