/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com January 2005
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

import java.util.* ;

/**
 * The MaskFactory class iterates through the complete set of possible
 * masks (a mask is a boolean[][] array that indicates whether the 
 * corresponding cells in Su Doku grids should have their initial values 
 * exposed) of a given size. The masks are guaranteed to have rotational
 * symmetry of order two, as demanded by the Su Doku puzzle. No mask 
 * will be a reflection of a mask that appeared earlier in the sequence.
 */

public abstract class MaskFactory implements Enumeration {

    int sector ;
    
    int[] sectorBalls0 ;
    
    protected int[][] g ;
    
    int[][] g0 ;
    
    boolean[] haveIteratedBalls ;
    
    boolean haveIteratedSectors ;
    
    protected boolean[][] mask ;
    
    boolean[][] previousMask ;
    
    protected int nSectors ,
                  cellsInRow ,
                  filledCells ;
    
    protected int[] sectorBalls ,
                    sectorSlots ,
                    sectorMin ,
                    sectorMax ;
    
    static Random random ;
    
    /**
     * Iterates sequentially through all possible settings of the vector x,
     * which has elements xMin[i] <= x[i] <= xMax[i].
     */
    
    static void iterate( int[] x , int[] xMin , int[] xMax ){
        int s = x.length ;
        while( -- s >= 0 && x[s] == xMax[s] );
        if( s >= 0 ){
            ++ x[s];
        }
        while( ++ s < x.length ){
            x[s] = xMin[s];
        }
    }
    
    /**
     * Iterates sequentially through all possible settings of the vector x,
     * which has elements that satisfy Sum(x[i]) = xSum.
     */
    
    static void iterate( int[] x , int xSum ){
        int i , s = 0 ;
        while( ++ s < x.length && x[s] == 0 );
        if( s < x.length ){
            -- x[s];
            i = x.length ;
            while( i > s ){
                -- i ;
                xSum -= x[i]; 
            }
        } else {
            i = x.length ;
        }  
        x[--i] = xSum ;          
        while( i > 0 ){
            x[--i] = 0 ;
        }
    }
    
    /**
     * Randomly allocates to the vector x, which has elements 
     * xMin[i] <= x[i] <= xMax[i].
     */
    
    static void randomlyAllocate( int[] x , int[] xMin , int[] xMax ){
        if( random == null ){
            random = new Random();            
        }
        int i = 0 ;
        while( i < x.length ){
            if( xMin[i] < xMax[i] ){
                x[i] = xMin[i] + Math.abs( random.nextInt() % ( xMax[i] - xMin[i] ) );
            } else {
                x[i] = xMin[i];
            }
            ++ i ; 
        }
    }

    /**
     * Randomly allocates to the vector x, which has elements with Sum(x[i]) = xSum.
     */
    
    static void randomlyAllocate( int[] x , int xSum ){
        if( random == null ){
            random = new Random();            
        }
        while( xSum > 0 ){
            ++ x[Math.abs( random.nextInt() % x.length )];
            -- xSum ;
        }
    }

    /**
     * Creates a new MaskFactory. The sequence starts in its natural start
     * position, which is usually some distance from the masks that are 
     * likely to lead to successful puzzles.
     * @param cellsInRow size of grid
     * @param filledCells number of true elements to appear in mask
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
    
    public MaskFactory( int cellsInRow ) {
        resize( cellsInRow );
//        initiate( false );
    }

    /**
     * Creates a new MaskFactory. The sequence starts at an optionally
     * random position.
     * @param cellsInRow size of grid
     * @param filledCells number of true elements to appear in mask
     * @param randomize whether the start position should be randomized
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
/*    
    public MaskFactory( int cellsInRow , 
                        int filledCells ,
                        boolean randomize ) throws Exception {
        this( cellsInRow , filledCells );
        initiate( randomize );
    }
*/
    /**
     * Creates a new MaskFactory. The sequence starts at the given
     * mask.
     * @param cellsInRow size of grid
     * @param filledCells number of true elements to appear in mask
     * @param mask start mask
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
/*    
    public MaskFactory( int cellsInRow , 
                        int filledCells ,
                        boolean[][] mask ) throws Exception {
        this( cellsInRow , filledCells );
        initiate( mask );
    }
*/
    /**
     * Creates a new MaskFactory. The sequence starts at the mask 
     * specified by a given gap sequence.
     * @param cellsInRow size of grid
     * @param filledCells number of true elements to appear in mask
     * @param gaps gap sequence that defines the start mask
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
/*    
    public MaskFactory( int cellsInRow , 
                        int filledCells ,
                        int[] gaps ) throws Exception {
        this( cellsInRow , filledCells );
        initiate( gaps );
    }
*/
    /**
     * Creates a new MaskFactory. The sequence starts at the mask specified
     * by the string, which should be in the format used by
     * <code>toString()</code>.
     * @param s string representation of start mask
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
/*    
    public MaskFactory( String s ) throws Exception {
        initiate( s );
    }
*/
    /**
     * Creates a new MaskFactory. The sequence will start from a mask with
     * uniformly-distributed filled cells.
     * @param cellsInRow size of grid
     * @param filledCells number of true elements to appear in mask
     * @param boxesAcross grid dimension
     * @throws Exception thrown if cellsInRow and filledCells are incompatible
     */
/*    
    public MaskFactory( int cellsInRow , 
                        int filledCells ,
                        int boxesAcross ) throws Exception {
        this( cellsInRow , filledCells );
        initiate( boxesAcross );
    }
*/
    public abstract int countSectors();

    public abstract boolean areSectorsValid();
    
    public abstract int allocateSectors( int filledCells );
    
    public abstract void populateMask();
    
    /**
     * Resizes the masks produced by the factory.
     * @param cellsInRow number of cell per row on the grid 
     */
    
    void resize( int cellsInRow ) {
        if( this.cellsInRow == cellsInRow ){
            return ;
        }
        this.cellsInRow = cellsInRow ;
        mask = new boolean[cellsInRow][cellsInRow];
        previousMask = new boolean[cellsInRow][cellsInRow];
        nSectors = countSectors();
        sectorBalls0 = new int[nSectors];
        sectorMin = new int[nSectors];
        sectorMax = new int[nSectors];
        sectorBalls = new int[nSectors];
        sectorSlots = new int[nSectors];
        g = new int[nSectors][];
        g0 = new int[nSectors][];       
        haveIteratedBalls = new boolean[nSectors]; 
    }
    
    /**
     * Sets the number of cells to be filled on the mask.
     * The returned number is the actual number of cells filled, 
     * which might exceed the requested number if the current 
     * symmetry type so demands.
     */
    
    public int setFilledCells( int filledCells ){
        if( this.filledCells != filledCells ){
            this.filledCells = allocateSectors( filledCells );        
        }
        return this.filledCells ;
    }
    
    // Various private functions to iterate through the set of possible masks.
    
    void resetBalls( int sector ){
        g0[sector][sectorBalls[sector]] = g[sector][sectorBalls[sector]] = sectorSlots[sector] - sectorBalls[sector] ;
        int i = 0 ;
        while( i < sectorBalls[sector] ){
            g0[sector][i] = g[sector][i] = 0 ;
            ++ i ;
        }
        haveIteratedBalls[sector] = false ;
    }
    
    void iterateBalls( int sector ){
        iterate( g[sector] , sectorSlots[sector] - sectorBalls[sector] );
        haveIteratedBalls[sector] = true ;
    }
    
    boolean hasMoreBalls( int sector ){
        if( ! haveIteratedBalls[sector] ){
            return true ;
        }
        int i = 0 ;
        while( i < 1 + sectorBalls[sector] ){
            if( g[sector][i] != g0[sector][i] ){
                return true ;   
            }
            ++ i ;
        }
        return false ;
    }

    void resetSectors(){
        int i = 0 ;
        while( i < nSectors ){
            sectorBalls[i] = sectorMin[i] ;
            ++ i ;    
        }
        while( ! areSectorsValid() ){
            iterate( sectorBalls , sectorMin , sectorMax );
        }
        i = 0 ;
        while( i < nSectors ){
            sectorBalls0[i] = sectorBalls[i];
            resetBalls( i ++ );
        }
        sector = 0 ;
        haveIteratedSectors = false ;
    }
    
    void iterateSectors(){
        iterate( sectorBalls , sectorMin , sectorMax );
        while( ! areSectorsValid() ){
            iterate( sectorBalls , sectorMin , sectorMax );
        }
        haveIteratedSectors = true ;
    }
    
    boolean hasMoreSectors(){
        if( ! haveIteratedSectors ){
            return true ;
        }
        int i = 0 ;
        while( i < nSectors ){
            if( sectorBalls[i] != sectorBalls0[i] ){
                return true ;
            }
            ++ i ;
        }
        return false ;
    }

    /**
     * Generates the next mask. It's (temporarily not) guaranteed that the new mask
     * will not simply be a reflection of an earlier mask. 
     */
    
    public void iterate(){
        if( hasMoreBalls( sector ) ){
            iterateBalls( sector );
        } else if( hasMoreSectors() ) {
            iterateSectors();
        } else {
            resetSectors();
        }
        populateMask();
    }
    
/*    
    void iterate(){
        int i ;
        int[] reflection = new int[nBalls+1];
        while(true){
            iterateGaps();
            generateMask();
            // Check top/bottom
            i = 0 ;
            while( i < reflection.length ){
                reflection[i++] = 0 ;
            }
            reflectTopBottom( reflection );
            if( ! precedes( reflection ) ){
                continue ;
            }
            // Check left/right
            i = 0 ;
            while( i < reflection.length ){
                reflection[i++] = 0 ;
            }
            reflectLeftRight( reflection );
            if( ! precedes( reflection ) ){
                continue ;
            }
            // Check top-left/bottom-right
            i = 0 ;
            while( i < reflection.length ){
                reflection[i++] = 0 ;
            }
            reflectTopLeftBottomRight( reflection );
            if( ! precedes( reflection ) ){
                continue ;
            }
            // Check top-right/bottom-left
            i = 0 ;
            while( i < reflection.length ){
                reflection[i++] = 0 ;
            }
            reflectTopRightBottomLeft( reflection );
            if( ! precedes( reflection ) ){
                continue ;
            }
            // All tests passed.
            return ;
        }
    }
*/    
    /**
     * Restarts the iterative sequence at a random position. 
     */
    
    void shuffle(){
        randomlyAllocate( sectorBalls , sectorMin , sectorMax );   
        while( ! areSectorsValid() ){
            iterate( sectorBalls , sectorMin , sectorMax );   
        }
        haveIteratedSectors = false ;
        int i , s = 0 ;
        while( s < nSectors ){
            sectorBalls0[s] = sectorBalls[s];
            g[s] = new int[1+sectorBalls[s]];
            g0[s] = new int[1+sectorBalls[s]];
            randomlyAllocate( g[s] , sectorSlots[s] - sectorBalls[s] );
            haveIteratedBalls[s] = false ;
            i = 0 ;
            while( i < sectorBalls[s] ){
                g0[s][i] = g[s][i];
                ++ i ;
            }
            ++ s ;   
        }
        populateMask();
    }
    
    /**
     * Writes the mask as a string.
     */
    
    public String toString(){
        StringBuilder sb = new StringBuilder();        
        int i , j ;        
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                if( mask[i][j] ){
                    sb.append("*");
                } else {
                    sb.append(".");
                }
                ++ j ;
            }
            sb.append("\n");
            ++ i ;
        }
        return sb.toString();
    }
    
    /**
     * Indicates whether the MaskFactory has cycled through the complete
     * set of all possible masks.
     */
    
    public boolean hasMoreElements(){
        return hasMoreSectors() || hasMoreBalls( sector );
    }
    
    /**
     * Returns the next mask. The returned mask will simply be
     * a reference to a member of the MaskFactory object, so the
     * caller might well have to clone. 
     */
    
    public Object nextElement(){
        if( hasMoreElements() ){
            int r , c ;
            r = 0 ;
            while( r < cellsInRow ){
                c = 0 ;
                while( c < cellsInRow ){
                    previousMask[r][c] = mask[r][c];
                    ++ c ;
                }
                ++ r ;
            }
            iterate();
            return previousMask ;
        } else {
            throw new NoSuchElementException();
        }
    }
    
    /**
     * Calculates whether the current mask precedes the mask 
     * represented by the gaps h[] in the iterative sequence.
     * Note that true will be returned if the two masks are equal. 
     */
/*
    boolean precedes( int[] h ){
        int i = g.length ;
        while( --i >= 0 ){
            if( h[i] > g[i] ){
                return false ;
            } else if( h[i] < g[i] ){
                return true ;
            }
        }
        return true ;
    }
*/    
    /**
     * Calculates the gaps that characterize the reflection 
     * of the current mask in the line that runs from 
     * grid top to bottom. The array h[] should match g in length
     * and contain zeros.
     */
/*    
    void reflectTopBottom( int[] h ) {
        int i = 0 , j = 0 , k = 0 , b = 0 ;
        while( b <= nBalls ){
            if( mask[i][cellsInRow-1-j] ){
                ++ b ;
            } else {
                ++ h[b];
            }
            if( ++ k == nSlots ){
                return ;
            }
            if( ++ j == cellsInRow ){
                ++ i ;
                j = 0 ;
            }
        }
    }
*/    
    /**
     * Calculates the gaps that characterize the reflection 
     * of the current mask in the line that runs from 
     * grid left to right. The array h[] should match g in length
     * and contain zeros.
     */
/*    
    void reflectLeftRight( int[] h ) {
        int i = 0 , j = 0 , k = 0 , b = 0 ;
        while( b <= nBalls ){
            if( mask[cellsInRow-1-i][j] ){
                ++ b ;
            } else {
                ++ h[b];
            }
            if( ++ k == nSlots ){
                return ;
            }
            if( ++ j == cellsInRow ){
                ++ i ;
                j = 0 ;
            }
        }
    }
*/    
    /**
     * Calculates the gaps that characterize the reflection 
     * of the current mask in the line that runs from 
     * grid top-left to bottom-right. The array h[] should match g in length
     * and contain zeros.
     */
/*    
    void reflectTopLeftBottomRight( int[] h ) {
        int i = 0 , j = 0 , k = 0 , b = 0 ;
        while( b <= nBalls ){
            if( mask[j][i] ){
                ++ b ;
            } else {
                ++ h[b];
            }
            if( ++ k == nSlots ){
                return ;
            }
            if( ++ j == cellsInRow ){
                ++ i ;
                j = 0 ;
            }
        }
    }
*/    
    /**
     * Calculates the gaps that characterize the reflection 
     * of the current mask in the line that runs from 
     * grid top-right to bottom-left. The array h[] should match g in length
     * and contain zeros.
     */
/*    
    void reflectTopRightBottomLeft( int[] h ) {
        int i = 0 , j = 0 , k = 0 , b = 0 ;
        while( b <= nBalls ){
            if( mask[cellsInRow-1-j][cellsInRow-1-i] ){
                ++ b ;
            } else {
                ++ h[b];
            }
            if( ++ k == nSlots ){
                return ;
            }
            if( ++ j == cellsInRow ){
                ++ i ;
                j = 0 ;
            }
        }
    }
*/    
    /**
     * Where necessary, rectify() replaces the current mask with
     * a reflection that appears in the iterative sequence. 
     * (The random generator usually creates masks that don't belong
     * on the iterative sequence and it sometimes takes quite a while
     * for the random mask to iterate through to a valid mask). 
     */
/*    
    void rectify(){
        int i ;
        int[] h = new int[ nBalls + 1 ];    
        generateMask();    
        reflectTopBottom( h );
        if( ! precedes( h ) ){
            i = 0 ;
            while( i < nBalls + 1 ){
                g[i] = h[i];
                ++ i ;
            }
        }
        i = 0 ;
        while( i < nBalls + 1 ){
            h[i++] = 0 ;
        }
        generateMask();
        reflectLeftRight( h );
        if( ! precedes( h ) ){
            i = 0 ;
            while( i < nBalls + 1 ){
                g[i] = h[i];
                ++ i ;
            }
        }
        i = 0 ;
        while( i < nBalls + 1 ){
            h[i++] = 0 ;
        }
        generateMask();
        reflectTopLeftBottomRight( h );
        if( ! precedes( h ) ){
            i = 0 ;
            while( i < nBalls + 1 ){
                g[i] = h[i];
                ++ i ;
            }
        }
        i = 0 ;
        while( i < nBalls + 1 ){
            h[i++] = 0 ;
        }
        generateMask();
        reflectTopRightBottomLeft( h );
        if( ! precedes( h ) ){
            i = 0 ;
            while( i < nBalls + 1 ){
                g[i] = h[i];
                ++ i ;
            }
        }
        generateMask();
    }
*/    
    /**
     * Starts the iterative sequence at the mask specified by an array of gaps.
     */    
/*    
    void initiate( int[] h ){
        int i = 0 ;
        while( i < nBalls + 1 ){
            g[i] = h[i];
            ++ i ;
        }
        rectify();
        i = 0 ;
        while( i < nBalls + 1 ){
            g0[i] = g[i];
            ++ i ;
        }
        generateMask();
        haveIterated = false ;
    }
*/    
    /**
     * Starts the iterative sequence at the given mask.
     */    
/*    
    void initiate( boolean[][] mask ){
        int i , j = 0 , k = 0 , b = 0 ;
        i = 0 ;
        while( i < nBalls + 1 ){
            g[i++] = 0 ;
        }
        i = 0 ;
        while( b <= nBalls ){
            if( mask[i][j] ){
                ++ b ;
            } else {
                ++ g[b];
            }
            if( ++ k == nSlots ){
                break ;
            }
            if( ++ j == cellsInRow ){
                ++ i ;
                j = 0 ;
            }
        }
        rectify();
        i = 0 ;
        while( i < nBalls + 1 ){
            g0[i] = g[i];
            ++ i ;
        }
        generateMask();
        haveIterated = false ;
    }
*/
    /**
     * Popualtes the pre-allocated mask array according to the contents of the string.
     */    
    
    public static void populate( boolean[][] mask , String s ) throws Exception {       
        int i , j , p , cellsInRow = s.indexOf('\n') , filledCells = 0 ;
        char c ;
        if( mask.length != cellsInRow ){
            throw new Exception("Mask array should have " + cellsInRow + " rows" );
        }
        i = p = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                c = s.charAt( p ++ );
                if( c == '*' ){
                    mask[i][j] = true ;
                    ++ filledCells ;
                } else if( c == '.' ){
                    mask[i][j] = false ;
                } else {
                    continue ;
                }
                ++ j ;
            }
            ++ i ;
        }
    }

    /**
     * Starts the iterative sequence at an optionally random position. 
     */
/*    
    void initiate( boolean randomize ){
        Random random = null ;    
        if( randomize ){
            random = new Random(); 
        }           
        int i = 0 ;
        while( i < 1 + nBalls ){
            g[i++] = 0 ;
        }
        i = 0 ;
        while( i < nSlots - nBalls ){
            if( randomize ){
                ++ g[Math.abs(random.nextInt()%(nBalls+1))];
            } else {
                ++ g[ i %( nBalls + 1 ) ];
            }
            ++ i ;
        }
        rectify(); // Ensure it's not a reflection.
        i = 0 ;
        while( i < nBalls + 1 ){
            g0[i] = g[i];
            ++ i ;
        }
        generateMask();
        haveIterated = false ;
    }
*/    
    /**
     * Starts the iterative sequence at a position such that the
     * filled cells will be uniformly distributed with respect to 
     * the grid size.
     * @param boxesAcross
     */
/*    
    void initiate( int boxesAcross ) throws Exception {
        
        Random generator = new Random();
        
        MaskState state = new MaskState();
        state.setup( boxesAcross , cellsInRow / boxesAcross );
        
        int i , r , c , minEliminated , nCandidates , pick ;
        int[] rCandidates = new int[cellsInRow*cellsInRow] ,
              cCandidates = new int[cellsInRow*cellsInRow] ;
              
        if( fillCentreCell ){
            state.addCell( cellsInRow / 2 , cellsInRow / 2 );
        }
        
        i = 0 ;
        while( i < nBalls ){
            minEliminated = Integer.MAX_VALUE ;
            r = 0 ;
            while( r < cellsInRow / 2 ){
                c = 0 ;
                while( c < cellsInRow ){
                    if( state.nInvulnerable[r][c] < minEliminated ){
                        minEliminated = state.nInvulnerable[r][c];
                    }
                    ++ c ;
                }
                ++ r ;
            }
            if( cellsInRow % 2 == 1 ){
                c = 0 ;
                while( c < cellsInRow / 2 ){
                    if( state.nInvulnerable[r][c] < minEliminated ){
                        minEliminated = state.nInvulnerable[r][c];
                    }
                    ++ c ;                
                }
            }
            nCandidates = 0 ;
            r = 0 ;
            while( r < cellsInRow / 2 ){
                c = 0 ;
                while( c < cellsInRow ){
                    if( state.nInvulnerable[r][c] == minEliminated ){
                        rCandidates[nCandidates] = r ;
                        cCandidates[nCandidates] = c ;
                        ++ nCandidates ;
                    }
                    ++ c ;
                }
                ++ r ;
            }
            if( cellsInRow % 2 == 1 ){
                c = 0 ;
                while( c < cellsInRow / 2 ){
                    if( state.nInvulnerable[r][c] == minEliminated ){
                        rCandidates[nCandidates] = r ;
                        cCandidates[nCandidates] = c ;
                        ++ nCandidates ;
                    }
                    ++ c ;                
                }
            }
            pick = nCandidates > 1 ? Math.abs( generator.nextInt() % nCandidates ) : 0 ;
            r = rCandidates[pick];
            c = cCandidates[pick];
            state.addCell( r , c );
            state.addCell( cellsInRow - 1 - r , cellsInRow - 1 - c );        
            ++ i ;      
        }
        initiate( state.mask );
    }        
*/    
    /**
     * Returns the mask dimension.
     */
    
    public int getCellsInRow(){
        return cellsInRow ;
    }

    /**
     * Returns the number of filled cells in the mask.
     */
    
    public int getFilledCells(){
        return filledCells ;    
    }

    /**
     * Determines whether the mask is symmetric in a line that 
     * extends from the left centre of the grid to the right centre.
     */

    public static boolean isSymmetricLeftRight( boolean[][] mask ){
        int i , j ;
        i = 0 ;
        while( i < mask.length / 2 ){
            j = 0 ;
            while( j < mask.length ){
                if( mask[i][j] != mask[mask.length-1-i][j] ){
                    return false ;
                }
                ++ j ;
            }
            ++ i ;
        }
        if( mask.length % 2 == 1 ){
            j = 0 ;
            while( j < mask.length / 2 ){
                if( mask[i][j] != mask[mask.length-1-i][j] ){
                    return false ;
                }
                ++ j ;
            }
        }
        return true ;        
    }
    
    /**
     * Determines whether the mask is symmetric in a line that 
     * extends from the top centre of the grid to the bottom centre.
     */

    public static boolean isSymmetricTopBottom( boolean[][] mask ){
        int i , j ;
        j = 0 ;
        while( j < mask.length / 2 ){
            i = 0 ;
            while( i < mask.length ){
                if( mask[i][j] != mask[i][mask.length-1-j] ){
                    return false ;
                }
                ++ i ;
            }
            ++ j ;
        }
        if( mask.length % 2 == 1 ){
            i = 0 ;
            while( i < mask.length / 2 ){
                if( mask[i][j] != mask[i][mask.length-1-j] ){
                    return false ;
                }
                ++ i ;
            }
        }
        return true ;
    }
    
    /**
     * Determines whether the mask is symmetric in a line that 
     * extends from the top-left corner of the grid to the bottom-right.
     */

    public static boolean isSymmetricTopLeftBottomRight( boolean[][] mask ){
        int i , j ;
        i = 0 ;
        while( i < mask.length ){
            j = i + 1 ;
            while( j < mask.length ){
                if( mask[i][j] != mask[j][i] ){
                    return false ;
                }
                ++ j ;
            }
            ++ i ;
        }
        return true ;
    }
    
    /**
     * Determines whether the mask is symmetric in a line that extends
     * from the top-right corner of the grid to the bottom-left.
     */

    public static boolean isSymmetricTopRightBottomLeft( boolean[][] mask ){
        int i , j ;
        i = 0 ;
        while( i < mask.length ){
            j = 0 ;
            while( j < mask.length - 1 - i ){
                if( mask[i][j] != mask[mask.length-1-j][mask.length-1-i] ){
                    return false ;
                }
                ++ j ;
            }
            ++ i ;
        }
        return true ;
    }

    /**
     * Determines whether a mask has rotational symmetry of order 2.
     */

    public static boolean isSymmetricOrder2( boolean[][] mask ){
        int i , j ;
        i = 0 ;
        while( i < mask.length / 2 ){
            j = 0 ;
            while( j < mask.length ){
                if( mask[i][j] != mask[mask.length-1-i][mask.length-1-j] ){
                    return false ;
                }
                ++ j ;
            }
            ++ i ;
        }
        if( mask.length % 2 == 1 ){
            j = 0 ;
            while( j < mask.length / 2 ){
                if( mask[i][j] != mask[mask.length-1-i][mask.length-1-j] ){
                    return false ;
                }
                ++ j ;
            }
        }
        return true ;
    }

    /**
     * Determines whether a mask has rotational symmetry of order 4.
     */

    public static boolean isSymmetricOrder4( boolean[][] mask ){
        int i , j ;
        final int jMax = mask.length / 2 + ( mask.length % 2 == 1 ? 1 : 0 ); 
        i = 0 ;
        while( i < mask.length / 2 ){
            j = 0 ;
            while( j < jMax ){
                if( mask[i][j] != mask[j][mask.length-1-i] ||
                    mask[j][mask.length-1-i] != mask[mask.length-1-i][mask.length-1-j] ||
                    mask[mask.length-1-i][mask.length-1-j] != mask[mask.length-1-j][i] ){
                    return false ;
                }
                ++ j ;
            }
            ++ i ;
        }
        return true ;
    }
    
    /**
     * Class test program takes the form MaskFactory [-c cellsInRow]
     * [-r|-i|-a boxes across] [-d] filledCells. When the -a option 
     * is stipulated, the factory will ensure that the sequence starts 
     * at a mask where the filled cells are distributed evenly across
     * the grid. When -r is selected, the sequence will start at a random 
     * position while when -i is selected, the initial mask will be read from
     * standard input. The option -t requests trace output. Defaults are nine cells 
     * in a row and no debug. 
     */
    
    public static void main( String[] args ){
       int i , 
           size = 9 , 
           filledCells = 0 ,
           boxesAcross = 0 ;
           
       boolean debug = false ,
               random = false ,
               standardInput = false ;
               
       final String usage = "Usage: MaskFactory [-c cellsInRow] [-r|-a boxes across] [-v] -i|filledCells";
       
       // Parse command-line arguments. 
       if( args.length == 0 ){
           System.err.println( usage );
           System.exit( 1 );
       }           
       i = 0 ;
       while( i < args.length - 1 ){
           if( args[i].equals("-v") ){
               debug = true ;
           } else if( args[i].equals("-r") ){
               random = true ;
           } else if( args[i].equals("-c") ){
               try {
                   size = Integer.parseInt( args[++i] );
               } catch ( NumberFormatException e ) {
                   System.err.println( usage );
                   System.exit( 1 );
               }
           } else if( args[i].equals("-a") ){
               try {
                   boxesAcross = Integer.parseInt( args[++i] );
               } catch ( NumberFormatException e ) {
                   System.err.println( usage );
                   System.exit( 1 );
               }
           } else {
               System.err.println( usage );
               System.exit( 1 );
           }
           ++ i ; 
       }
       if( boxesAcross > 0 && size % boxesAcross != 0 ){
           System.err.println("Numbers of boxes across and cells per row are incompatible");
           System.exit( 2 );
       }
       if( random && boxesAcross != 0 ){
           System.err.println("The -a and -r options are mutually exclusive");
           System.exit( 2 );
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
       // Initiate the mask sequence.
/*       
       try {        
           MaskFactory maskFactory = null ;
           if( standardInput ){
               String text ;
               StringBuffer maskText = new StringBuffer();
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
               maskFactory = new MaskFactory( maskText.toString() );
           } else if( boxesAcross > 0 ){
               maskFactory = new MaskFactory( size , filledCells , boxesAcross );
           } else if( random ) {
               maskFactory = new MaskFactory( size , filledCells , true );
           } else {
               maskFactory = new MaskFactory( size , filledCells );               
           }
       } catch ( Exception e ) {
           System.err.println( e.getMessage() );
           System.exit( 2 );
       }
       
       // Iterate through.
       i = 0 ;
       while( maskFactory.hasMoreElements() ){
           ++ i ;
           if( debug ){
               System.out.println( i + "." );
               System.out.println( maskFactory.toString() );    
           }
           maskFactory.nextElement();
       }
       
       System.out.println( i + " distinct masks found");
*/
    }
}
