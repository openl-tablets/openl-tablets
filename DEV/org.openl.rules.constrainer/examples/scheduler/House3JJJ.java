package scheduler;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

import org.openl.ie.constrainer.*;
import org.openl.ie.scheduler.*;
import org.openl.ie.tools.FastVector;
import java.util.*;

public class House3JJJ {

    class _Job {
        public String Name;
        public int Duration;
        public int Assignments; // bitfield
        public Job job;

        public _Job(String n, int d, int a)
        {
            Name=n;
            Duration=d;
            Assignments=a;
        }
    }

    final int JOE  = 1;
    final int JACK = 2;
    final int JIM  = 4;

    FastVector Workers;
    FastVector Jobs;

    class Worker {
        private ResourceUnary _resource;
        private IntExp _attendance;
        private Schedule _schedule;
        private String _name;
        private int _mask;

        public Worker(Schedule s, String name, int mask) throws Failure
        {
            _schedule = s;
            _name = name;
            _mask = mask;
            _resource = new ResourceUnary( _schedule );
            _resource.setName(_name);
            _schedule.addResource(_resource);
            _attendance = _resource.getUsed();
        }

        public Resource getResource() { return _resource; }
        public IntExp getAttendance() { return _attendance; }
        public int mask() { return _mask; }
        public Resource resource() { return _resource; }
        public String toString() { return _name; }
    }

    public void init(Schedule s) throws Failure
    {
        Workers = new FastVector();
        Workers.add(new Worker(s,"joe",JOE));
        Workers.add(new Worker(s,"jim",JIM));
        Workers.add(new Worker(s,"jack",JACK));

        Jobs = new FastVector();
        Jobs.add( new _Job("masonry",7,JOE|JACK) );
        Jobs.add( new _Job("carpentry",3,JOE|JIM) );
        Jobs.add( new _Job("plumbing",8,JACK) );
        Jobs.add( new _Job("ceiling",3,JOE|JIM) );
        Jobs.add( new _Job("roofing",1,JOE|JIM) );
        Jobs.add( new _Job("painting",2,JACK|JIM) );
        Jobs.add( new _Job("windows",1,JOE|JIM) );
        Jobs.add( new _Job("facade",2,JOE|JACK) );
        Jobs.add( new _Job("garden",1,JOE|JACK|JIM) );
        Jobs.add( new _Job("moving",1,JOE|JIM) );
    }

    private Job _job_(HashMap m, String name)
    {
        return (Job)m.get(name);
    }

    Constrainer C;

    public void go() throws Failure
    {
        C = new Constrainer("House 3: JoeJimJack");
        Schedule S = new Schedule(C, 0, 30);
        S.setName("JoeJimJack");
        System.out.println("\n[-----------------JoeJimJack-------------------]");
        init(S);
        int i;

        HashMap jobs = new HashMap();
        System.out.println("*** Jobs:");
        for (i=0; i<Jobs.size(); i++) {
            _Job _jb = (_Job)Jobs.elementAt(i);
            Job jb = S.addJob(_jb.Duration,_jb.Name);
            _jb.job = jb;
            System.out.print("  "+_jb.Name);
            jobs.put(_jb.Name,jb);
        }

        System.out.println("\nPosting sequence constraints... ");
        _job_(jobs,"carpentry").startsAfterEnd(_job_(jobs,"masonry")).asConstraint().post();
        _job_(jobs,"roofing").startsAfterEnd(_job_(jobs,"carpentry")).asConstraint().post();
        _job_(jobs,"plumbing").startsAfterEnd(_job_(jobs,"masonry")).asConstraint().post();
        _job_(jobs,"ceiling").startsAfterEnd(_job_(jobs,"masonry")).asConstraint().post();
        _job_(jobs,"windows").startsAfterEnd(_job_(jobs,"roofing")).asConstraint().post();
        _job_(jobs,"facade").startsAfterEnd(_job_(jobs,"roofing")).asConstraint().post();
        _job_(jobs,"facade").startsAfterEnd(_job_(jobs,"plumbing")).asConstraint().post();
        _job_(jobs,"garden").startsAfterEnd(_job_(jobs,"roofing")).asConstraint().post();
        _job_(jobs,"garden").startsAfterEnd(_job_(jobs,"plumbing")).asConstraint().post();
        _job_(jobs,"painting").startsAfterEnd(_job_(jobs,"ceiling")).asConstraint().post();
        _job_(jobs,"moving").startsAfterEnd(_job_(jobs,"windows")).asConstraint().post();
        _job_(jobs,"moving").startsAfterEnd(_job_(jobs,"facade")).asConstraint().post();
        _job_(jobs,"moving").startsAfterEnd(_job_(jobs,"garden")).asConstraint().post();
        _job_(jobs,"moving").startsAfterEnd(_job_(jobs,"painting")).asConstraint().post();
        System.out.println("Ok");


        AlternativeResourceSet wrkres = new AlternativeResourceSet();
        for (i=0; i<Workers.size(); i++) {
            wrkres.add(((Worker)Workers.elementAt(i)).resource());
        }

        System.out.println("Posting requirement constraints...");
        for (i=0; i<Jobs.size(); i++) {
            _Job _jb = (_Job)Jobs.elementAt(i);
            Job jb = _jb.job;

            AlternativeResourceConstraint c = jb.requires(wrkres, 1);
            for (int w=0; w<Workers.size(); w++) {
                Worker wrk = (Worker)Workers.elementAt(w);
                if ((_jb.Assignments & wrk.mask()) == 0) {
                  c.setNotPossible(wrk.resource());
                }
            }
            System.out.println(c);
            c.post();
        }

        ///////////////////////////////////////////////////////
        System.out.println("Solving...");

        Goal solution = new GoalSetTimes(S.jobs());

        IntExpArray attn = new IntExpArray(C, Workers.size());
        for(i=0; i < Workers.size(); ++i)
        {
            attn.set(((Worker)Workers.elementAt(i)).getAttendance(),i);
        }

        C.printInformation();
        //C.traceExecution();


        IntExp objective = _job_(jobs,"moving").getStartVariable();

        IntExp objective1 = attn.sum();
        IntVar objective2 = C.addIntVar(0,30);

        Goal solutionAll = new GoalAnd(
                                    solution,
//                                    new GoalMyPrint(S,objective1),
//                                    new GoalBoundToMin(objective2)
                                    new GoalBoundToMaxValue(objective2,attn)
                                    );

        if (!C.execute(new GoalMinimize(solutionAll,objective)))
            System.out.println("Can not minimize cost "+objective);
        else
        {
            System.out.println("Optimal solution with objective="+objective+":");
            for(i=0; i < S.jobs().size(); ++i)
            {
                Job job = (Job)S.jobs().elementAt(i);
                System.out.println(job);
            }
        }

        System.out.println("SUM ATTENDANCE1 ="+objective1);

        Worker w = (Worker)Workers.elementAt(0);
        System.out.println("availability["+w+"]="+w.getAttendance());
        for(i=0; i < S.resources().size(); ++i)
        {
            Resource r = (Resource)S.resources().elementAt(i);
            System.out.println(r.mapString()+" "+r);
        }

    }

    class GoalBoundToMaxValue extends GoalImpl {
        IntExp _exp;
        IntExpArray _a;
        int _v;
        public GoalBoundToMaxValue( IntExp e , IntExpArray a)
        {
          super(e.constrainer());
          _exp = e;
          _a = a;
          _v = a.max();
        }
        public Goal execute() throws Failure
        {
          //System.out.print("FIX: "+_exp+" -> ");
          int v = _a.min();
          for (int i=0; i<_a.size(); i++)
          {
            if (v < _a.get(i).min())
              v = _a.get(i).min();
          }
          _exp.setMax(v);
          if (_v > v) {
            _v = v;
            System.out.println("-> "+_v);
          }
          //System.out.println(v);
          return null;
        }
    }

    class GoalBoundToMin extends GoalImpl {
        IntExp _exp;
        int v;
        public GoalBoundToMin( IntExp e )
        {
          super(e.constrainer());
          _exp = e;
          v = _exp.max();
        }
        public Goal execute() throws Failure
        {
         // System.out.print("FIX: "+_exp+" -> ");
          _exp.setValue(_exp.min());
          if (v>_exp.min()) {
            v = _exp.min();
            System.out.println("-> "+v);
          }
          //System.out.println(_exp);
          return null;
        }
    }

    class GoalMyPrint extends GoalImpl {
        Schedule _s;
        int n;
        IntExp o;

        public GoalMyPrint(Schedule s, IntExp _o)
        {
            super(s.constrainer(),"my print");
            _s = s;
            o = _o;
            n =1;
        }
        public Goal execute() {

            System.out.println("############## SOLUTION "+(n++)+", obj = "+o);
            for(int i=0; i < _s.jobs().size(); ++i)
            {
                Job job = (Job)_s.jobs().elementAt(i);
                System.out.println(job);
            }
            return null;
        }
    }


    public static void main(String args[]) throws Exception
    {
        House3JJJ house = new House3JJJ();
        house.go();
    }
}
