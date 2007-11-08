/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com January 2004
 * 
 * Web site: http://act365.com/sudoku
 * E-mail: developers@act365.com
 * 
 * The Su Doku Solver solves Su Doku problems - see http://www.sudoku.com.
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *  
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package com.act365.sudoku;

import java.io.* ;
import java.text.DecimalFormat ;
import java.util.Date ;

/**
 * A Solver instance solves a grid on a thread that exits
 * gracefully if interrupted.
 */

public class Solver extends Thread {

    Grid grid ;
    
    IStrategy strategy ,
              composeSolver ;
    
    int maxSolns ,
        maxUnwinds ,
        maxComplexity ,
        composeSolverThreshold ,
        index ;
    
    Composer composer ;
    
    PrintWriter debug ;
    
    transient int nUnwinds ,
                  nSolns ,
                  complexity ,
                  firstDisputableMove ;
    
    /**
     * Creates a Solver instance.
     * @param threadName thread name
     * @param composer composer object to which to report
     * @param index composer-defined index number for solver
     * @param grid grid to be solved
     * @param strategy strategy to be used to complete the grid
     * @param composeSolver solver to be used at each step by the composer in order to check solution uniqueness
     * @param maxSolns the maximum number of solutions to find before exit (0 for unlimited solutions)
     * @param maxUnwinds the maximum permitted number of unwinds (0 for no limit)
     * @param maxComplexity the maximum permitted complexity (0 for no limit)
     * @param debug (optional) destination for debug info
     */
    
    public Solver( String threadName ,
                   Composer composer ,
                   int index ,
                   Grid grid ,
                   IStrategy strategy ,
                   IStrategy composeSolver ,
                   int composeSolverThreshold ,
                   int maxSolns ,
                   int maxUnwinds ,
                   int maxComplexity ,
                   PrintStream debug ){
        super( threadName );
        this.composer = composer ;
        this.index = index ;
        this.grid = grid ;
        this.strategy = strategy ;
        this.composeSolver = composeSolver ;
        this.composeSolverThreshold = composeSolverThreshold ;
        this.maxSolns = maxSolns ;
        this.maxUnwinds = maxUnwinds ;
        this.maxComplexity = maxComplexity ;
        this.debug = debug instanceof PrintStream ? new PrintWriter( debug ) : null ;
    }
    
    /**
     * Creates a Solver instance.
     * @param grid grid to be solved
     * @param strategy strategy to be used to complete the grid
     * @param composeSolver solver to be used at each step by the composer in order to check solution uniqueness
     * @param maxSolns the maximum number of solution to find before exit (0 for no limit)
     * @param debug (optional) destination for debug info
     */
    
    public Solver( Grid grid ,
                   IStrategy strategy ,
                   IStrategy composeSolver ,
                   int composeSolverThreshold ,
                   int maxSolns ,
                   PrintStream debug ){
        this.composer = null ;
        this.index = 0 ;                     
        this.grid = grid ;
        this.strategy = strategy ;
        this.composeSolver = composeSolver ;
        this.composeSolverThreshold = composeSolverThreshold ;
        this.maxSolns = maxSolns ;
        this.maxUnwinds = 0 ;
        this.maxComplexity = Integer.MAX_VALUE ;
        this.debug = debug instanceof PrintStream ? new PrintWriter( debug ) : null ;
    }

    /**
     * Creates a Solver instance.
     * @param grid grid to be solved
     * @param strategy strategy to be used to complete the grid
     */

    public Solver( Grid grid ,
                   IStrategy strategy ){
        this( grid , strategy , null , 0 , 1 , null );                   
    }
    
    /**
     * Runs the solver on a thread.
     */    
    
    public void run(){
        try {
            nSolns = solve( strategy , composeSolver , composeSolverThreshold , maxSolns , true , maxUnwinds , maxComplexity );
        } catch ( Exception e ) {
            e.printStackTrace();
            System.err.println( grid );
            System.err.println( strategy );
            nSolns = 0 ;
        }
        if( composer instanceof Composer ){
            composer.solverFinished( index );
        }
    }
    
    /**
     * Returns the number of solutions found.
     */
    
    public int getNumberOfSolutions(){
        return nSolns ;
    }
    
    /**
     * Returns the number of times the tree had to be unwound 
     * in order to solve the grid.
     */
    
    public int getNumberOfUnwinds(){
        return nUnwinds ;
    }
    
    /**
     * Determines the complexity figure for a puzzle.
     */
    
    public int getComplexity(){
        return complexity ;
    }
    
    /**
     * Solves the grid.
     * @param strategy main strategy to use
     * @param composeSolver (optional) strategy used at each step in order to check for uniqueness
     * @param the number of cells filled before the composeSolver is invoked
     * @param maxSolns (optional) maximum number of solutions to be found before exit
     * @param maskSize the number of initially-filled cells 
     * @param countUnwinds whether the number of unwinds should be counted
     * @param maxUnwinds the maximum permitted number of unwinds (0 for no limit)
     * @param maxComplexity the maximum permitted complexity (0 for no limit)     
     * @return the number of solutions found
     */
    
    int solve( IStrategy strategy , 
               IStrategy composeSolver , 
               int composeSolverThreshold ,
               int maxSolns ,
               boolean countUnwinds ,
               int maxUnwinds ,
               int maxComplexity ) throws Exception {
        int nSolns = 0 , nComposeSolns = 2 , count , lastWrittenMove ;
        boolean stillIndisputable = true ;
        if( countUnwinds ){
            nUnwinds = complexity = 0 ;
        } else {
            firstDisputableMove = 0 ;
        }
        try {
            strategy.setup( grid );
        } catch ( Exception e ) {
            return 0 ;
        }
        // Solve the grid.
        solveGrid:
        while( ! isInterrupted() ){
            // Try to find a valid move.
            if( strategy.findCandidates() > 0 ){
                strategy.selectCandidate();
                strategy.setCandidate();
                if( ! strategy.updateState( strategy.getBestX() , strategy.getBestY() , strategy.getBestValue() , strategy.getBestReason() , strategy.getScore() > 1 ) ){
                    return nSolns ;
                }
                if( stillIndisputable && ! countUnwinds ){
                    if( strategy.getScore() == 1 ){
                        ++ firstDisputableMove ;   
                    } else {
                        stillIndisputable = false ;
                    }
                }                
                count = grid.countFilledCells();
                if( composeSolver instanceof IStrategy && count >= composeSolverThreshold ){
                    nComposeSolns = solve( composeSolver , null , 0 , 2 , false , 0 , 0 );
                    composeSolver.reset();
                    if( nComposeSolns == 0 ){
                        nComposeSolns = 2 ;
                        // No solutions exist - that's no good.
                        lastWrittenMove = strategy.getLastWrittenMove();
                        complexity += strategy.getThreadLength() - lastWrittenMove ;
                        if( countUnwinds && ( ++ nUnwinds == maxUnwinds || complexity >= maxComplexity ) || ! strategy.unwind( lastWrittenMove , true , true ) ){
                            return nSolns ;
                        }
                        continue ;
                    }
                }
                if( count == grid.cellsInRow * grid.cellsInRow || nComposeSolns == 1 ){
                    // Grid has been solved.
                    if( nComposeSolns == 1 ){
                        composer.addSolution( index );
                        nComposeSolns = 2 ;
                    }
                    if( debug instanceof PrintWriter ){
                        debug.println( ( 1 + nSolns ) + ".");
                        debug.println( grid.toString() );
                        int i = 0 ;
                        while( i < strategy.getThreadLength() ){
                            debug.print( ( 1 + i ) + ". " + strategy.getReason(i) );
                            ++ i ;
                        }      
                        debug.println();  
                        debug.flush();                
                    }
                    if( ++ nSolns == maxSolns ){ 
                        return nSolns ;
                    }
                    lastWrittenMove = strategy.getLastWrittenMove();
                    complexity += strategy.getThreadLength() - lastWrittenMove ;
                    if( countUnwinds && ( ++ nUnwinds == maxUnwinds || complexity >= maxComplexity ) || ! strategy.unwind( lastWrittenMove , true , true ) ){
                        return nSolns ;
                    }
                } else if( composeSolver instanceof IStrategy  && count >= composeSolverThreshold ){
                    try {
                        int i = 0 ;
                        while( i < firstDisputableMove ){
                            strategy.updateState( composeSolver.getThreadX( i ) , 
                                                  composeSolver.getThreadY( i ) , 
                                                  grid.data[composeSolver.getThreadX( i )][composeSolver.getThreadY( i )] , 
                                                  null , 
                                                  false );
                            ++ i ;
                        }
                        composeSolver.reset( firstDisputableMove );
                    } catch ( Exception e ) {
                        composeSolver.reset();
                        lastWrittenMove = strategy.getLastWrittenMove();
                        complexity += strategy.getThreadLength() - lastWrittenMove ;
                        if( countUnwinds && ( ++ nUnwinds == maxUnwinds || complexity >= maxComplexity ) || ! strategy.unwind( lastWrittenMove , true , true ) ){
                            return nSolns ;
                        }
                    }
                }
            } else {
                // Stuck
                lastWrittenMove = strategy.getLastWrittenMove();
                complexity += strategy.getThreadLength() - lastWrittenMove ;
                if( countUnwinds && ( ++ nUnwinds == maxUnwinds || complexity >= maxComplexity ) || ! strategy.unwind( lastWrittenMove , true , true ) ){
                    return nSolns ;
                }
            }
        }
        
        return nSolns ;
    }
    
    /**
     * Command-line app to solve Su Doku puzzles.
     * <br><code>Solver [-m max solutions] [-s strategy] [-v]</code>
     * <br><code>[-m max solutions]</code> stipulates the maximum number of solutions to be reported. 
     * The default is for all solutions to be reported.
     * <br><code>[-s strategy]</code> stipulates the strategy to be used. the default is Least Candidates Hybrid.
     * <br><code>[-v]</code> stipulates whether the app should execute in verbose mode. The default is no.
     * <br><code>[-p]</code> enables profiling information.
     * <br> The puzzle will be read from standard input.  
     */
    
    public static void main( String[] args ){
        
        final String usage = "Usage: Solver [-m max solutions] [-s strategy] [-v] [-p profile]";
        
        boolean debug = false ,
                profile = false ;
        
        int i , maxSolns = 0 ;
        
        String strategyLabel = "Least Candidates Hybrid";
        
        i = 0 ;
        while( i < args.length ){
            if( args[i].equals("-m") ){
                try {
                    maxSolns = Integer.parseInt( args[++i] );
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-v") ) {
                debug = true ;
            } else if( args[i].equals("-s") ){
                strategyLabel = args[++i];
            } else if( args[i].equals("-p") ) {
                profile = true ;
            } else {
                System.err.println( usage );
                System.exit( 1 );
            }
            ++ i ;
        }
        // Create the strategy.
        IStrategy strategy ;       
        if( ( strategy = Strategy.create( strategyLabel , debug ) ) == null ){
            System.err.println("Unsupported strategy");
            System.exit( 2 );
        }
        // Read the grid from standard input. A blank line will terminate
        // the read.
        Grid grid = new Grid();
        String text ;
        StringBuilder gridText = new StringBuilder();
        BufferedReader standardInputReader = new BufferedReader( new InputStreamReader( System.in ) );
        try {
            while( ( text = standardInputReader.readLine() ) != null ){
                if( text.length() == 0 ){
                    break ;
                }
                gridText.append( text );
                gridText.append('\n');
            }
            grid.populate( gridText.toString() );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 3 );
        }
        // Solve.
        long startTime ;
        double solveTime ;
        Solver solver = new Solver( grid , strategy , null , 0 , maxSolns , debug ? System.out : null );
        startTime = new Date().getTime();
        solver.start();
        try {
            solver.join();
        } catch ( InterruptedException e ){
            System.out.println("Solver interrupted");
        }
        solveTime = ( new Date().getTime() - startTime )/ 1000. ;
        System.out.print( solver.getNumberOfSolutions() + " solution");
        if( solver.getNumberOfSolutions() != 1 ){
            System.out.print('s');
        }
        System.out.print(" found in ");
        System.out.println( new DecimalFormat("#0.000").format( solveTime )+ "s");
        if( profile ){
            System.out.println("Unwinds: " + solver.nUnwinds );
            System.out.println("Complexity: " + solver.complexity );
            if( strategy instanceof LeastCandidatesHybrid ){
                LeastCandidatesHybrid lch = (LeastCandidatesHybrid) strategy ;
                if( lch.state instanceof IState ){
                    System.out.println("Single Candidature: " + lch.singleCandidatureCalls + " calls");
                    System.out.println("Locked Sector Candidates: " + lch.lockedSectorCandidatesCalls + " calls " + lch.lockedSectorCandidatesEliminations + " eliminations");
                    System.out.println("Disjoint Subsets: " + lch.disjointSubsetsCalls + " calls " + lch.disjointSubsetsEliminations + " eliminations");
                    System.out.println("Two-Sector Disjoint Subsets: " + lch.twoSectorDisjointSubsetsCalls + " calls " + lch.twoSectorDisjointSubsetsEliminations + " eliminations");
                    System.out.println("Single-Valued Chains: " + lch.singleValuedChainsCalls + " calls " + lch.singleValuedChainsEliminations + " eliminations");
                    System.out.println("Many-Valued Chains: " + lch.manyValuedChainsCalls + " calls " + lch.manyValuedChainsEliminations + " eliminations");
                    System.out.println("Nishio: " + lch.nishioCalls + " calls " + lch.nishioEliminations + " eliminations");
                }
            }
        }
    }
}
