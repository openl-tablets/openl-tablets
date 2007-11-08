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

import com.act365.sudoku.masks.*;

import java.io.* ;
import java.text.* ;
import java.util.* ;

/**
 * A Composer object attempts to compose valid Su Doku puzzles with the
 * MostCandidates strategy. It runs as a thread because composition is
 * a lengthy process that the caller might well choose to terminate.
 */

public class Composer extends Thread {

    Vector puzzles ;
    
    GridContainer gridContainer ;

    int maxSolns ,
        maxMasks ,
        maxUnwinds ,
        maxComplexity ,
        nSolvers ,
        composeSolverThreshold ,
        lockedSectorCandidatesFilter ,
        disjointSubsetsFilter ,
        twoSectorDisjointSubsetsFilter ,
        singleValuedChainsFilter ,
        manyValuedChainsFilter ,
        nishioFilter ,
        guessFilter ;
    
    boolean logicalFilter ,
            shuffleGrid ,
            shuffleMask ,
            xmlFormat ;
    
    MaskFactory maskFactory ;

    Solver[] solvers ;
        
    IStrategy[] composeSolvers ;

    boolean[][][] solverMasks ;
    
    Grid[] solverGrids ;
    
    PrintWriter output ;
      
    final static String[] featuredGrades = { "Easy" , 
                                             "Medium" ,
                                             "Hard" ,
                                             "Two-Sector Disjoint Subsets" ,
                                             "Single-Valued Strings" ,
                                             "Many-Valued Strings" ,
                                             "Nishio" ,
                                             "Guess" };
    
    final static int EASY                        = 0 ,
                     MEDIUM                      = 1 ,
                     HARD                        = 2 ,
                     TWO_SECTOR_DISJOINT_SUBSETS = 3 ,
                     SINGLE_VALUED_CHAINS        = 4 ,
                     MANY_VALUED_CHAINS          = 5 ,
                     NISHIO                      = 6 ,
                     GUESS                       = 7 ;
                             
    transient int cellsInRow ,
                  maskSize ,
                  nSolns ,
                  nMasks ,
                  nThreads ,
                  nUnstartable ,
                  maxPuzzleComplexity ,
                  mostComplex ,
                  tempSolutions ;
    
    transient boolean allSolutionsFound ;
    
    transient boolean[] isAlive , 
                        isStartable;
    
    transient LeastCandidatesHybrid lch ;
    
    transient long startTime ;
    
    
    /**
     * Generates a new thread that will attempt to construct a Su Doku
     * puzzle with 'maskSize' cells on initial display. The puzzle will
     * be copied into 'grid'. When 'randomize' is selected, the mask 
     * will be selected randomly and the RadomMostCandidates strategy 
     * will be used to complete it.
     * @param gridContainer object to be notified when a puzzle has been composed
     * @param boxesAcross 
     * @param maxSolns maximum number of solutions to find (0 for no limit)
     * @param maxMasks maximum number of masks to use (0 for no limit)
     * @param maxUnwinds maximum permitted number of unwinds (0 for no limit)
     * @param maxComplexity maximum permitted complexity (0 for no limit)
     * @param maskFactory factory to generate the masks
     * @param nSolvers number of solver threads to run
     * @param composeSolverThreshold tree depth beyond which the compose solver will be invoked
     * @param output optional output stream (set to null for no output)
     */    
    
    public Composer( GridContainer gridContainer ,
                     int boxesAcross ,
                     int maxSolns ,
                     int maxMasks ,
                     int maxUnwinds ,
                     int maxComplexity ,
                     MaskFactory maskFactory ,
                     int nSolvers ,
                     int composeSolverThreshold ,
                     PrintStream output ,
                     boolean leastCandidatesHybridFilter ,
                     int lockedSectorCandidatesFilter ,
                     int disjointSubsetsFilter ,
                     int twoSectorDisjointSubsetsFilter ,
                     int singleValuedStringsFilter ,
                     int manyValuedStringsFilter ,
                     int nishioFilter ,
                     int guessFilter ,
                     boolean explain ,
                     boolean shuffleGrid ,
                     boolean shuffleMask ,
                     boolean xmlFormat ) throws Exception {
        this.gridContainer = gridContainer ;                       
        this.maxSolns = maxSolns ;
        this.maxMasks = maxMasks ;
        this.maxUnwinds = maxUnwinds ;
        this.maxComplexity = maxComplexity ;
        this.maskFactory = maskFactory ;
        this.nSolvers = nSolvers ;
        this.composeSolverThreshold = composeSolverThreshold ;
        this.output = output != null ? new PrintWriter( output ) : null ;
        this.lockedSectorCandidatesFilter = lockedSectorCandidatesFilter ;
        this.disjointSubsetsFilter = disjointSubsetsFilter ;
        this.twoSectorDisjointSubsetsFilter = twoSectorDisjointSubsetsFilter ;
        this.singleValuedChainsFilter = singleValuedStringsFilter ;
        this.manyValuedChainsFilter = manyValuedStringsFilter ;
        this.nishioFilter = nishioFilter ;
        this.guessFilter = guessFilter ;
        this.shuffleGrid = shuffleGrid ;
        this.shuffleMask = shuffleMask ;
        this.xmlFormat = xmlFormat ;
        
        maskSize = maskFactory.getFilledCells();
        cellsInRow = maskFactory.getCellsInRow();
        solvers = new Solver[nSolvers];
        composeSolvers = new LeastCandidatesHybrid[nSolvers];
        isAlive = new boolean[nSolvers];
        isStartable = new boolean[nSolvers];
        solverMasks = new boolean[nSolvers][cellsInRow][cellsInRow];
        solverGrids = new Grid[nSolvers];
        puzzles = new Vector();
        lch = new LeastCandidatesHybrid( false , true , false , true , explain );
        logicalFilter = guessFilter == -1 || 
                        guessFilter == 0 && 
                        ( lockedSectorCandidatesFilter != 0 ||
                          disjointSubsetsFilter != 0 ||
                          singleValuedStringsFilter != 0 ||
                          twoSectorDisjointSubsetsFilter != 0 ||
                          manyValuedStringsFilter != 0 ||
                          nishioFilter != 0 );
            
        int i = 0 ;
        while( i < nSolvers ){
            composeSolvers[i] = new LeastCandidatesHybrid( false , leastCandidatesHybridFilter , false , false , false );
            solverGrids[i] = new Grid( boxesAcross , cellsInRow / boxesAcross );
            ++ i ;
        }
        
        startTime = new Date().getTime();
        
        if( xmlFormat && this.output != null ){
            this.output.print( SuDokuUtils.libraryBookHeader( getClass().getName() , cellsInRow , boxesAcross , featuredGrades ) );
        }
    }
    
    /**
     * Called by a Solver object in order to indicate that a
     * solution has been found.
     * @see Solver
     * @param solverIndex index of the reporting solver
     */
    
    public synchronized void addSolution( int solverIndex ){

        boolean lockedSectorCandidates = false ,
                disjointSubsets = false ,
                twoSectorDisjointSubsets = false ,
                singleValuedChains = false ,
                manyValuedChains = false ,
                nishio = false ,
                logical ;

        int puzzleComplexity ,
            puzzleUnwinds ,
            category = GUESS ;

        // We might already have enough puzzles from the other threads.
        if( maxSolns > 0 && nSolns >= maxSolns ){
            return ;
        }
        // The grid might have been completed by the composeSolver,
        // in which case only certain cells should be read from the
        // solver grid.
        int r , c ;
        Grid solution = new Grid( solverGrids[solverIndex].boxesAcross , solverGrids[solverIndex].boxesDown );
        solverGrids[solverIndex].solve( lch , 1 );
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                if( solverMasks[solverIndex][r][c] ){
                    solution.data[r][c] = solverGrids[solverIndex].data[r][c] ;
                } else {
                    solution.data[r][c] = 0 ;
                }
                ++ c ;
            }
            ++ r ;
        }            
        lch.reset();
        // Ensure that the puzzle appears in the correct form.
        Grid puzzle = (Grid) solution.clone(); 
        if( shuffleGrid ){
            puzzle.shuffle( true , false , false );
        } else {
            puzzle.rectify( solverMasks[solverIndex] );
        }
        // Store (and report) the puzzle if it hasn't been seen before.
        if( ! puzzles.contains( puzzle ) ){
            // Categorize the puzzle and filter it out if necessary.
            puzzle.solve( lch , 2 );
            puzzleComplexity = puzzle.complexity ;
            puzzleUnwinds = puzzle.nUnwinds ;
            lch.reset();
            if( puzzleComplexity > maxComplexity ){
                mostComplex = nSolns ;
                maxPuzzleComplexity = puzzleComplexity ;                
            }
            logical = puzzleUnwinds == 1 ;
            if( logicalFilter && ! logical || guessFilter == 1 && logical ){
                return ;
            }          
            puzzle.solve( lch , 1 );  
            if( logical ){
                lockedSectorCandidates = lch.lockedSectorCandidatesEliminations > 0 ; 
                if( lockedSectorCandidatesFilter == 1 && ! lockedSectorCandidates || 
                    lockedSectorCandidatesFilter == -1 && lockedSectorCandidates ){
                    return ;
                }
                disjointSubsets = lch.disjointSubsetsEliminations > 0 ; 
                if( disjointSubsetsFilter == 1 && ! disjointSubsets || 
                    disjointSubsetsFilter == -1 && disjointSubsets ){
                    return ;
                }
                twoSectorDisjointSubsets = lch.twoSectorDisjointSubsetsEliminations > 0 ; 
                if( twoSectorDisjointSubsetsFilter == 1 && ! twoSectorDisjointSubsets || 
                    twoSectorDisjointSubsetsFilter == -1 && twoSectorDisjointSubsets ){
                    return ;
                }
                singleValuedChains = lch.singleValuedChainsEliminations > 0 ; 
                if( singleValuedChainsFilter == 1 && ! singleValuedChains || 
                    singleValuedChainsFilter == -1 && singleValuedChains ){
                    return ;
                }
                manyValuedChains = lch.manyValuedChainsEliminations > 0 ; 
                if( manyValuedChainsFilter == 1 && ! manyValuedChains|| 
                    manyValuedChainsFilter == -1 && manyValuedChains ){
                    return ;
                }
                nishio = lch.nishioEliminations > 0 ; 
                if( nishioFilter == 1 && ! nishio || 
                    nishioFilter == -1 && nishio ){
                    return ;
                }
            }
            if( output != null ){
                if( ! xmlFormat ){
                    double t = ( new Date().getTime() - startTime )/ 1000. ;
                    output.println( "Puzzle " + ( 1 + nSolns ) +":\n");
                    output.println( "Puzzle Complexity = " + puzzleComplexity );
                    output.println( "Puzzle Unwinds = " + puzzleUnwinds );
                    output.println( "Cumulative Composer Complexity = " + solvers[solverIndex].complexity );
                    output.println( "Cumulative Composer Unwinds = " + solvers[solverIndex].nUnwinds );
                    output.println( "Time = " + new DecimalFormat("#0.000").format( t ) + "s" );
                }
                boolean multipleCategories = false ;
                StringBuilder sb = ! xmlFormat ? new StringBuilder() : null ;
                if( logical ){
                    category = EASY ;
                    if( lockedSectorCandidates ){
                        category = MEDIUM ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Locked Sector Candidates");
                            multipleCategories = true ;
                        }
                    }
                    if( disjointSubsets ){
                        category = HARD ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Disjoint Subsets");
                            multipleCategories = true ;
                        }
                    }
                    if( twoSectorDisjointSubsets ){
                        category = TWO_SECTOR_DISJOINT_SUBSETS ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Two-Sector Disjoint Subsets");
                            multipleCategories = true ;
                        }
                    }
                    if( singleValuedChains ){
                        category = SINGLE_VALUED_CHAINS ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Single-Valued Chains");
                            multipleCategories = true ;
                        }
                    }
                    if( manyValuedChains ){
                        category = MANY_VALUED_CHAINS ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Many-Valued Chains");
                            multipleCategories = true ;
                        }
                    }
                    if( nishio ){
                        category = NISHIO ;
                        if( ! xmlFormat ){
                            if( multipleCategories ){
                                sb.append(":");
                            }
                            sb.append("Nishio");
                            multipleCategories = true ;
                        }
                    }
                    sb.append("\n");
                }
                if( lch.explainsReasoning() ){
                    int i = 0 ;
                    while( i < lch.getThreadLength() ){
                        sb.append( ( 1 + i ) + ". " + lch.getReason( i ) );
                        ++ i ;
                    }
                }
                if( sb != null && sb.length() > 0 ){
                    output.println( sb.toString() );
                }
            }
            lch.reset();
            puzzles.addElement( puzzle );
            if( output != null ){
                if( xmlFormat ){
                    output.println( puzzle.toXML( 1 + nSolns , featuredGrades[category] ) );
                } else {
                    output.println( puzzle.toString() );
                }
                output.flush();
            }
            if( ++ nSolns == maxSolns ){
                allSolutionsFound = true ;
                notifyAll();
            }
        }
    }
    
    /**
     * Called by a Solver object in order to indicate that all
     * possible solutions to the puzzle have been considered.
     * @see Solver
     * @param solverIndex index of finished solver
     */
    
    public synchronized void solverFinished( int solverIndex ){
        isAlive[solverIndex] = false ;
        -- nThreads ;
        notifyAll();    
    }

    /**
     * Starts a new Solver thread to find puzzles for
     * a fixed initial mask.
     * @see Solver
     * @param solverIndex index of solver to restart with a new mask
     */
    
    synchronized boolean startThread( int solverIndex ){
        boolean[][] mask = null ;
        if( shuffleMask ){
            maskFactory.shuffle();
        }
        try {
            mask = (boolean[][]) maskFactory.nextElement();
        } catch ( NoSuchElementException e ) {
            return false ;
        }
        ++ nMasks ;
        int r , c ;
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                solverMasks[solverIndex][r][c] = mask[r][c];
                ++ c ;
            }
            ++ r ;
        }
        solverGrids[solverIndex].reset();
        solvers[solverIndex] = new Solver( "Solver-" + ( solverIndex + 1 ) , 
                                           this ,
                                           solverIndex ,
                                           solverGrids[solverIndex] , 
                                           new MostCandidates( solverMasks[solverIndex] , true ) , 
                                           composeSolvers[solverIndex] ,
                                           composeSolverThreshold , 
                                           maxSolns , 
                                           maxUnwinds ,
                                           maxComplexity ,
                                           null );
        solvers[solverIndex].start();
        while( ! solvers[solverIndex].isAlive() );
        isAlive[solverIndex] = true ;
        ++ nThreads ;
        return true ;
    }
    
    /**
     * Starts a Composer thread. The thread will start a number of Solvers
     * and collate the returned results.
     * @see Solver
     */
    
    @Override public void run(){
        nSolns = 0 ;
        nMasks = 0 ;
        allSolutionsFound = false ;
        nThreads = 0 ;
        nUnstartable = 0 ;
        // Set off the solver threads, one per mask.
        int i = 0 ;
        while( i < nSolvers && ( maxMasks == 0 || nMasks < maxMasks ) ){
            isAlive[i] = false ;
            if( !( isStartable[i] = startThread( i ) ) ){
                ++ nUnstartable ;
            }
            ++ i ;
        }
        // Wait for the reports back.
        synchronized( this ){
            while( ! allSolutionsFound && nThreads > 0 && ( maxMasks == 0 || nMasks <= maxMasks ) ){
                try {
                    while( ! allSolutionsFound && 
                           nThreads + nUnstartable == nSolvers && 
                           ( maxMasks == 0 || nMasks <= maxMasks ) ){
                        wait();
                    }                
                } catch ( InterruptedException e ) {
                    break ;
                }
                i = 0 ;
                while( i < nSolvers ){
                    if( ! isAlive[i] && isStartable[i] ){
                        if( !( isStartable[i] = startThread( i ) ) ){
                            ++ nUnstartable ;                        
                        }
                    }
                    ++ i ;
                }
            }
            // Interrupt the remaining threads.
            i = 0 ;
            while( i < nSolvers ){
                if( isAlive[i] ){
                    solvers[i].interrupt();
                    while( isAlive[i] ){
                        try {
                            wait();
                        } catch ( InterruptedException ie ) {
                            continue ;
                        }
                    }
                }
                ++ i ;
            }
            if( gridContainer != null ){
                if( puzzles.size() > 0 ){
                    gridContainer.setGrid( (Grid) puzzles.elementAt( 0 ) );
                }
            } else {
                if( xmlFormat && output != null ){
                    output.println( SuDokuUtils.libraryBookFooter() );
                    output.close();
                } else {
                    System.out.println( nSolns + " solutions found");
                    if( nSolns > 0 ){
                        System.out.println("Most complex: (" + maxPuzzleComplexity + ")");
                        System.out.println( ((Grid) puzzles.elementAt( mostComplex ) ).toString() );
                    }
                }
            }
        }
    }            
    
    /**
     * Command-line app to compose Su Doku puzzles.
     * <br><code>Composer [-a across] [-d down] [-ms max solns|-mm max masks] [-mu max unwinds] [-s solvers] [-c threshold] [-r] [-v] -i|#cells</code>
     * <br><code>[-a across] [-d down]</code> define the dimensions of the puzzles to be composed. The default values are 
     * three in each dimension.
     * <br><code>[-ms max solns|-mm max masks]</code> defines optional termination conditions. The app will exit if <code>max solns</code>
     * puzzles have been generated or <code>max masks</code> masks have been considered, whichever occurs sooner.
     * <br><code>[-mu max unwinds]</code> stipulates a limit on the number of unwinds permitted on a single mask.
     * <br><code>[-mu max unwinds]</code> stipulates a limit on the complexity permitted on a single mask.
     * <br><code>[-s solvers]</code> stipulates the number of solver (or, equivalently, threads) to execute simultaneously. The default is 3.
     * <br><code>[-c threshold]</code> stipulates the tree depth beyond which the compose solver will be invoked. The default value is 0.
     * <br><code>[-v]</code> stipualtes whether the Composer should run in verbose mode. The default is no.
     * <br><code>[-f]</code> stipulates that the full set of Least Candidates Hybrid algorithms should be used to solve.
     * <br><code>[-shuffle]</code> stipulates that the puzzles should be randomly shuffled.
     * <br><code>[-xml]</code> stipulates that the output should be in XML format.
     * <br><code>-i</code> stipulates that the initial mask should be read from standard input.
     * <br><code>#cells</code> stipulates the number of initially-filled cells to appear in the puzzles.   
     */

    public static void main( String[] args ){
        final String usage = "Usage: Composer [-a across] [-d down] [-ms max solns|-mm max masks] [-mu max unwinds] [-mc max complexity] [-s solvers] [-c threshold] [-v] [-shuffle] [-iter] [-f] [-xml] -i|#cells" ,
                     strategyTypes = "Valid strategy types are:\nSSC [Single Sector Candidates]\nDS [Disjoint Subsets]\nSVS [Single-Valued Strings]\nMVS [Many-Valued Strings]\nNishio";
        
        int boxesAcross = 3 ,
            boxesDown = 3 ,
            maxSolns = 0 ,
            maxMasks = 0 ,
            maxUnwinds = 0 ,
            maxComplexity = Integer.MAX_VALUE ,
            nSolvers = defaultThreads ,
            filledCells = 0 ,
            maskType = MaskUtils.ROTATE_2 ,
            composeSolverThreshold = 0 ,
            lockedSectorCandidatesFilter = 0 ,
            disjointSubsetsFilter = 0 ,
            singleValuedChainsFilter = 0 ,
            twoSectorDisjointSubsetsFilter = 0 ,
            manyValuedChainsFilter = 0 ,
            nishioFilter = 0 ,
            guessFilter = 0 ,
            sign ;
            
        boolean explain = false ,
                standardInput = false ,
                leastCandidatesHybridFilter = false ,
                shuffleGrid = false ,
                shuffleMask = true ,
                xmlFormat = false ;
        
        // Process command-line args.
        if( args.length == 0 ){
            System.err.println( usage );
            System.exit( 1 );
        }           
        int i = 0 ;
        while( i < args.length - 1 ){
            sign = 0 ;
            if( args[i].equals("-a") ){
                try {
                    boxesAcross = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-d") ){
                try {
                    boxesDown = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-ms") ){
                try {
                    maxSolns = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-mm") ){
                try {
                    maxMasks = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-mu") ){
                try {
                    maxUnwinds = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-mc") ){
                try {
                    maxComplexity = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-s") ){
                try {
                    nSolvers = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-c") ){
                try {
                    composeSolverThreshold = Integer.parseInt( args[++i] ); 
                } catch ( NumberFormatException e ) {
                    System.err.println( usage );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-mask") ) {
                try {
                    maskType = MaskUtils.getMaskType( args[++i] );
                } catch( Exception e ) {
                    System.err.println( e.getMessage() );
                    System.exit( 1 );
                }
            } else if( args[i].equals("-v") ){
                explain = true ;
            } else if( args[i].equals("-f") ){
                leastCandidatesHybridFilter = true ;
            } else if( args[i].equals("-shuffle") ){
                shuffleGrid = true ;
            } else if( args[i].equals("-iter") ){
                shuffleMask = false ;
            } else if( args[i].equals("-xml") ) {
                xmlFormat = true ;
            } else if( args[i].charAt( 0 ) == '+' ) {
                sign = 1 ;
            } else if( args[i].charAt( 0 ) == '-' ) {
                sign = -1 ;
            } else {
                System.err.println( usage );
                System.exit( 1 );                
            }
            if( sign != 0 ){
                String strategy = args[i].substring( 1 );
                if( strategy.equalsIgnoreCase("lsc") ){
                    lockedSectorCandidatesFilter = sign ;
                } else if( strategy.equalsIgnoreCase("ds") ) {
                    disjointSubsetsFilter = sign ;
                } else if( strategy.equalsIgnoreCase("2ds") ) {
                    twoSectorDisjointSubsetsFilter = sign ;
                } else if( strategy.equalsIgnoreCase("svc") ) {
                    singleValuedChainsFilter = sign ;
                } else if( strategy.equalsIgnoreCase("mvc") ){
                    manyValuedChainsFilter = sign ;
                } else if( strategy.equalsIgnoreCase("nishio") ){
                    nishioFilter = sign ;
                } else if( strategy.equalsIgnoreCase("guess") ){
                    guessFilter = sign ;
                } else {
                    System.err.println( strategyTypes );
                    System.exit( 1 );
                }
            }
            ++ i ;
        }
        if( i == args.length ){
            System.err.println( usage );
            System.exit( 1 );
        }
        if( maxMasks > 0 && maxSolns > 0 ){
            System.err.println("The -ms and -mm options are mutually exclusive");
            System.exit( 1 );
        }
        try {
            filledCells = Integer.parseInt( args[i] ); 
        } catch ( NumberFormatException e ) {
            if( args[i].equals("-i") ){
                standardInput = true ;
            } else {
                System.err.println( usage );
                System.exit( 1 );
            }
        }
        // Read a mask from standard input.
        MaskFactory maskFactory = null ;
        
        try {
            if( standardInput ){
                String text ;
                StringBuilder maskText = new StringBuilder();
                BufferedReader standardInputReader = new BufferedReader( new InputStreamReader( System.in ) );
                try {
                    while( ( text = standardInputReader.readLine() ) != null ){
                        if( text.length() == 0 ){
                            break ;
                        }
                        maskText.append( text );
                        maskText.append('\n');
                    }
                } catch ( IOException e ) {
                    System.err.println( e.getMessage() );
                    System.exit( 3 );               
                }
                boolean[][] mask = new boolean[boxesAcross*boxesDown][boxesAcross*boxesDown];
                MaskFactory.populate( mask , maskText.toString() );
                maskFactory = new UserDefined( mask );                                
            } else {
                maskFactory = MaskUtils.createMaskFactory( maskType , boxesAcross * boxesDown , boxesAcross , null );
            }
            filledCells = maskFactory.setFilledCells( filledCells );
            maskFactory.shuffle();
        } catch ( Exception e ) {
            System.err.println( e.getMessage() );
            System.exit( 2 );
        }
        // Create the puzzles.
        try {
            new Composer( null , 
                          boxesAcross , 
                          maxSolns ,
                          maxMasks , 
                          maxUnwinds ,
                          maxComplexity ,
                          maskFactory , 
                          nSolvers , 
                          composeSolverThreshold , 
                          System.out ,
                          leastCandidatesHybridFilter ,
                          lockedSectorCandidatesFilter ,
                          disjointSubsetsFilter ,
                          twoSectorDisjointSubsetsFilter ,
                          singleValuedChainsFilter ,
                          manyValuedChainsFilter ,
                          nishioFilter ,
                          guessFilter ,
                          explain ,
                          shuffleGrid ,
                          shuffleMask ,
                          xmlFormat ).start();  
        } catch ( Exception e ) {
            System.out.println( e.getMessage() );
            System.exit( 3 );
        }
    }
    
    public final static int defaultThreads = 3 ;
}
