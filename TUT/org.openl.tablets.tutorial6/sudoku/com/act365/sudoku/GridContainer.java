/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com November 2004
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

import java.awt.*;
import java.awt.event.*;
import java.util.Date ;

/**
 * The GridContainer class displays a Su Doku grid.
 */

public class GridContainer extends com.act365.awt.Container 
                           implements AdjustmentListener {

    final static int insetSize = 1 ,
                     displayHeight = 600 ,
                     displayWidth = 600 ,
                     maxDisplayedRows = 15 ,
                     maxDisplayedColumns = 11 ;
    
    // Members
    
    int displayedRows ,
        displayedColumns ,
        firstDisplayedRow ,
        firstDisplayedColumn ,
        maskType ;
    
    double solveTime ;
    
    Grid grid ;
    
    LeastCandidatesHybrid strategy ;
    
    TextField[][] textFields ;
    
    Composer composer ;
    
    Scrollbar horizontalScroll ,
              verticalScroll ;
    
    /**
     * Creates a new GridContainer instance. 
     */
    
    public GridContainer( Grid grid ) {
        this.grid = grid ;
        strategy = (LeastCandidatesHybrid) Strategy.create( Strategy.LEAST_CANDIDATES_HYBRID_II , true );
        horizontalScroll = new Scrollbar( Scrollbar.HORIZONTAL );
        horizontalScroll.addAdjustmentListener( this );
        verticalScroll = new Scrollbar();
        verticalScroll.addAdjustmentListener( this );
        maskType = MaskUtils.ROTATE_2 ;
        setBoxes( grid.boxesAcross , grid.boxesDown );
        write();
    }
    
    /**
     * Lays out the grid of text fields.
     */
    
    void layoutComponents(){
		
        setVisible( false );
        
		int r , c , rDisplayed , cDisplayed ;
        
        rDisplayed = 0 ;
		while( rDisplayed < displayedRows ){
            r = firstDisplayedRow + rDisplayed ;
			cDisplayed =  0 ;
			while( cDisplayed < displayedColumns ){
                c = firstDisplayedColumn + cDisplayed ;
				if( r < grid.cellsInRow + grid.boxesDown - 1 && r % (grid.boxesAcross+1) < grid.boxesAcross && 
                    c < grid.cellsInRow + grid.boxesAcross - 1 && c % (grid.boxesDown+1) < grid.boxesDown ){
					addComponent( textFields[r/(grid.boxesAcross+1)*grid.boxesAcross+r%(grid.boxesAcross+1)][c/(grid.boxesDown+1)*grid.boxesDown+c%(grid.boxesDown+1)] , cDisplayed , rDisplayed , 1 , 1 , 0 , 0 , insetSize );
				} else {
					addComponent( new Label() , cDisplayed , rDisplayed , 1 , 1 , 0 , 0 , insetSize );
				}
				++ cDisplayed ;
			}
			++ rDisplayed ;
		}
        
        if( displayedRows < grid.cellsInRow + grid.boxesDown - 1 ){
            addComponent( verticalScroll , displayedColumns , 0 , 1 , displayedRows , 0 , 0 , insetSize );
        }
        
        if( displayedColumns < grid.cellsInRow + grid.boxesAcross - 1 ){
            addComponent( horizontalScroll , 0 , displayedRows , displayedColumns , 1 , 0 , 0 , insetSize );           
        }
        
        setVisible( true );
    }
    
    /**
     * Returns the best display size for a GridContainer.
     */
    
    public Dimension getBestSize() {
        return new Dimension( displayWidth , displayHeight );   
    }

    /**
     * Solves the grid.
     */
    
    public void solve(){  
    	read();
        long now = new Date().getTime();
        grid.solve( strategy , 1 );
        solveTime = ( new Date().getTime() - now )/ 1000. ;
    	write();  
    }

    /**
     * Returns the time taken to solve.
     */

    public double getSolveTime() {
        return solveTime ;
    }
    
    /**
     * Evaluates the complexity of the grid. Establishes that just a single
     * solution exists and, if so, calculates the number of thread unwinds
     * used to solve the problem.
     * @return number of solutions 
     */
        
    public int evaluate(){
    	read();
        final int strategyType = grid.cellsInRow >= 12 ? Strategy.LEAST_CANDIDATES_HYBRID_II : Strategy.LEAST_CANDIDATES_HYBRID ; 
        IStrategy strategy = Strategy.create( strategyType , false );
    	int nSolns = grid.solve( strategy , 2 );
    	strategy.reset();
    	
    	return nSolns ;
    }
    
    /**
     * Unsolves the grid (reverts its state to that prior to the 
     * most recent solve).
     */
    
    public void unsolve(){
        unsolve( 0 );
    }
    
    /**
     * Resets the grid to the partial solution prior to the given move.
     */
    
    public void unsolve( int move ){
    	strategy.reset( move );
    	write();
    }
    
    /**
     * Resets the grid.
     */
    
    public void reset(){
    	grid.reset();
        solveTime = 0 ;
        try {
            strategy.setup( grid );
        } catch ( Exception e ) {
        }
    	write();
    }
    
    /**
     * Shuffles the grid.
     */
    
    public void shuffle(){
        grid.shuffle();
        write();
    }

    /**
     * Sets the underlying grid to be a clone of the given grid.   
     * @param grid new grid
     */

    public synchronized void setGrid( Grid grid ){
        final boolean redraw = this.grid.boxesAcross != grid.boxesAcross || 
                               this.grid.boxesDown != grid.boxesDown ;
        this.grid = (Grid) grid.clone();
        if( redraw ){
            removeAll();
            layoutComponents();
            validate();
        }
        write();
    }

    /**
     * Sets the size of the underlying grid.
     */
    
    public void setSize( int boxesAcross , 
                         int boxesDown ){
        grid.resize( boxesAcross , boxesDown );
    }
    
    /**
     * Resizes the drawn grid.
     * @param boxesAcross - number of boxes across one row of the Su Doku grid
     * @param boxesDown - number of boxes down one column of the Su Doku grid
     */
    
    public void setBoxes( int boxesAcross ,
                          int boxesDown ){
        int r , c , cellsInRow ;
        cellsInRow = boxesAcross * boxesDown ; 
        textFields = new TextField[cellsInRow][cellsInRow];
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                textFields[r][c] = new TextField( 2 );   
                ++ c ;
            }
            ++ r ;
        }
        displayedRows = Math.min( maxDisplayedRows , cellsInRow + boxesDown - 1 );
        displayedColumns = Math.min( maxDisplayedColumns , cellsInRow + boxesAcross - 1 );
        verticalScroll.setMaximum( cellsInRow + boxesDown - 1 );
        verticalScroll.setVisibleAmount( displayedRows );
        verticalScroll.setBlockIncrement( 1 + boxesAcross );
        horizontalScroll.setMaximum( cellsInRow + boxesAcross - 1 );
        horizontalScroll.setVisibleAmount( displayedColumns );
        horizontalScroll.setBlockIncrement( 1 + boxesAcross );
        firstDisplayedRow = firstDisplayedColumn = 0 ;
        removeAll();
        layoutComponents();
        validate();
    }

    /**
     * Pastes data onto the grid.
     * @param s data to be pasted, which should be in the form created by Copy
     */
    
    public void paste( String s ){
        int oldBoxesAcross = grid.boxesAcross ,
            oldBoxesDown = grid.boxesDown ;            
        grid.populate( s );
        if( grid.boxesAcross != oldBoxesAcross || grid.boxesDown != oldBoxesDown ){
            setBoxes( grid.boxesAcross , grid.boxesDown );
        }
        write();
    }
    
    /**
     * Composes a puzzle, with rotational symmetry and a unique solution,
     * based upon the initial values in the grid.
     * @param filledCells - number of filled cells to appear in the puzzle
     */
    
    public synchronized int startComposer( int filledCells ){
        read();        
        try {
            boolean[][] userMask = null ;
            
            if( maskType == MaskUtils.USER_DEFINED ){
                userMask = new boolean[grid.cellsInRow][grid.cellsInRow];
                int i , j ;
                i = 0 ;
                while( i < grid.cellsInRow ){
                    j = 0 ;
                    while( j < grid.cellsInRow ){
                        userMask[i][j] = grid.data[i][j] > 0 ;
                        ++ j ;
                    }
                    ++ i ;
                }
            }
            
            MaskFactory factory = MaskUtils.createMaskFactory( maskType , grid.cellsInRow , grid.boxesAcross , userMask );
            filledCells = factory.setFilledCells( filledCells );
            factory.shuffle();
            
            composer = new Composer( this , 
                                     grid.boxesAcross , 
                                     1 , 
                                     0 ,
                                     50 ,
                                     Integer.MAX_VALUE ,
                                     factory ,
                                     Composer.defaultThreads , 
                                     0 , 
                                     null ,
                                     grid.cellsInRow >= 12 ,
                                     strategy.useLockedSectorCandidates ? 0 : -1 ,
                                     strategy.useDisjointSubsets ? 0 : -1 ,
                                     strategy.useTwoSectorDisjointSubsets ? 0 : -1 ,
                                     strategy.useSingleValuedChains ? 0 : -1 ,
                                     strategy.useManyValuedChains ? 0 : -1 ,
                                     strategy.useNishio ? 0 : -1 ,
                                     strategy.useGuesses ? 0 : -1 ,
                                     false ,
                                     true ,
                                     true ,
                                     false );
            composer.start();
        } catch ( Exception e ) {
            filledCells = 0 ;
        }
        return filledCells ;
    }

    /**
     * Interrupts any ComposerThread that might have been started 
     * by startComposer().
     */
    
    public void stopComposer(){
        if( composer instanceof Composer ){
            composer.interrupt();
        }
    }

    /** 
     * Returns number of boxes across one row of the Su Doku grid.
     */
    
    public int getBoxesAcross(){
    	return grid.boxesAcross ;
    }
    
    /**
     * Returns number of boxes down one column of the Su Doku grid.
     */
    
    public int getBoxesDown(){
    	return grid.boxesDown ;
    }
    
    /**
     * Returns the number of unwinds used to solve the grid
     * as a measure of its complexity.
     */
    
    public int getComplexity(){
    	return grid.complexity ;
    }
    
    /**
     * Returns the current strategy.
     */
    
    public LeastCandidatesHybrid getStrategy(){
        return strategy ;    
    }
    
    /**
     * Fills the text fields with values from the grid.
     */
    
    void write(){        
        
        int r , c ;
        
        c = 0 ;
        while( c < grid.cellsInRow ){
            r = 0 ;
            while( r < grid.cellsInRow ){ 
                textFields[r][c].setText( SuDokuUtils.valueToString( grid.data[r][c] - 1 ) );   
                ++ r ;
            }
            ++ c ;
        }
    }
    
    /**
     * Reads the values in the text fields and populates the underlying grid.
     */
    
    void read(){
        int r , c ;        
        r = 0 ;
        while( r < grid.cellsInRow ){
            c = 0 ;
            while( c < grid.cellsInRow ){ 
                grid.data[r][c] = SuDokuUtils.parse( textFields[r][c].getText().trim() );      
                ++ c ;
            }
            ++ r ;
        }
    }
    
    /**
     * A GridContainer returns a string representation of the 
     * underlying grid.
     */
    
    public String toString(){
        return grid.toString();
    }
    
    /**
     * Reacts to scrollbar presses.
     */

    public void adjustmentValueChanged( AdjustmentEvent evt ){
        if( evt.getSource() == horizontalScroll ){
            firstDisplayedColumn = evt.getValue();   
        } else if( evt.getSource() == verticalScroll ){   
            firstDisplayedRow = evt.getValue();   
        }
        removeAll();
        layoutComponents();
        validate();
   }
}
