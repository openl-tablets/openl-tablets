package org.openl.tablets.tutorial6.manners;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

import com.exigen.ie.constrainer.Failure;

public class MannersParser
{
    public static void main(String[] args) throws IOException
    {
	for (int i = 0; i < args.length; i++)
	{
	    MannersParser p = new MannersParser();
	    p.parse(args[i]);
	    int extIndex = args[i].lastIndexOf('.');
	    String outFile = args[i].substring(0, extIndex) + ".csv";
	    System.out.println(outFile);
	    p.dump(outFile);
	    p.solveAndPrint();
	}
    }

    private void solveAndPrint()
    {
	ArrayList<Guest> ordered = new ArrayList<Guest>(guests.values());
	Collections.sort(ordered);

	int size = ordered.size();

	int[] sexes = new int[size];

	for (int i = 0; i < sexes.length; i++)
	{
	    sexes[i] = ordered.get(i).getSex().equals("m") ? 0 : 1;
	}

	int[] hobbies = new int[size];

	for (int i = 0; i < hobbies.length; i++)
	{
	    int h = 0;
	    for (String hobby : ordered.get(i).getHobbies())
	    {
		int hi = getHobbyIndex(hobby);
		h |= 1 << hi;
	    }

	    hobbies[i] = h;
	}

	int[] res=null;
	long start, end;
	int N = 5;
	try
	{
	    start = System.currentTimeMillis();
	    for (int i = 0; i < N; i++)
	    {
		    res = new MannersSolver().solve(sexes, hobbies);
		
	    }
	    end = System.currentTimeMillis();

	} catch (Failure e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    throw RuntimeExceptionWrapper.wrap(e);
	}

	for (int i = 0; i < res.length; i++)
	{
	    Guest g = ordered.get(res[i]);
	    System.out.println(serializeGuestAsCSV(g));
	}
	System.out.println("Total Time: " + (end - start)/N + "ms");

    }

    private void parse(String fname) throws IOException
    {
	BufferedReader r = new BufferedReader(new FileReader(fname));

	while (true)
	{
	    String line = r.readLine();
	    if (line == null)
		break;
	    parseLine(line);
	}
    }

    private void parseLine(String line)
    {
	String[] tokens = StringTool.tokenize(line, " ()^");

	int adj = 0;
	if (tokens.length == 8) // (make guest ^name 1 ^sex m ^hobby 2)
	    adj = 1;
	else if (tokens.length == 7) // (guest (name n1) (sex m) (hobby h1))
	    adj = 0;
	else
	{
	    if (tokens.length > 0)
		System.out.println("Strange line: " + line);
	    return;
	}

	String name = tokens[2 + adj];
	String sex = tokens[4 + adj];

	String hobby = tokens[6 + adj];

	Guest g = getGuest(name, sex);

	g.getHobbies().add(hobby);

	addHobbyToTheList(hobby);

    }

    ArrayList<String> allHobbies = new ArrayList<String>();

    void dump(String fname) throws IOException
    {
	PrintWriter w = new PrintWriter(new FileWriter(fname));

	w.print("Name,Sex,");
	for (Iterator<String> iterator = allHobbies.iterator(); iterator
		.hasNext();)
	{
	    String h = iterator.next();
	    w.print(h);
	    w.print(',');
	}
	w.println();

	ArrayList<Guest> ordered = new ArrayList<Guest>(guests.values());

	Collections.sort(ordered);

	for (Iterator<Guest> iterator = ordered.iterator(); iterator.hasNext();)
	{
	    Guest g = iterator.next();

	    w.println(serializeGuestAsCSV(g));
	}

	w.close();

    }

    private void addHobbyToTheList(String hobby)
    {
	if (allHobbies.contains(hobby))
	    return;
	allHobbies.add(hobby);
    }

    int getHobbyIndex(String hobby)
    {
	return allHobbies.indexOf(hobby);
    }

    int guestIndex = 0;

    private Guest getGuest(String name, String sex)
    {
	Guest g = guests.get(name);
	if (g == null)
	{
	    g = new Guest();
	    g.setName(name);
	    g.setSex(sex);
	    g.setOriginalIndex(++guestIndex);
	    guests.put(name, g);
	}

	if (!sex.equals(g.getSex()))
	    throw new RuntimeException(
		    "Guest's sex have been changed!!! Guest:" + g.getName());
	return g;
    }

    HashMap<String, Guest> guests = new HashMap<String, Guest>();

    String serializeGuestAsCSV(Guest g)
    {
	StringBuilder b = new StringBuilder();
	b.append(g.getSex()).append(',');

	for (int i = 0; i < allHobbies.size(); ++i)
	{
	    String hobby = allHobbies.get(i);

	    if (g.getHobbies().contains(hobby))
		b.append("X,");
	    else
		b.append(" ,");

	}

	b.append(g.getName()).append(',');

	return b.toString();

    }

}
