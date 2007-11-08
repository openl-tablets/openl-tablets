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

package com.act365.sudoku ;

/**
 * LeastCandidatesHybrid combines the Least Candidates Cell and Least 
 * Candidates Number strategies. 
 */

public class LeastCandidatesHybrid extends StrategyBase implements IStrategy {

    enum Nishio { DEFINITE , POSSIBLE , NULL };
    
    // Link types. The code makes use of the numerical values of the
    // following enumerations, so they haven't been converted to enums.
    
    final static int NONE = 0 ,
                     MATCH = 1 ,
                     STRONG = 2 ,
                     WEAK = 3 ,
                     CELL = 4 ,
                     EXTENDED = 5 ,
                     NISHIO = 6 ,
                     INFERRED = 7 ;
    
//    enum LinkType { NONE , MATCH , STRONG , WEAK , CELL , EXTENDED , NISHIO , INFERRED };
    
    final static int LEFT_LEFT   = 0 ,
                     LEFT_RIGHT  = 1 ,
                     RIGHT_LEFT  = 2 ,
                     RIGHT_RIGHT = 3 ;
    
//    enum LinkPosition { LEFT_LEFT , LEFT_RIGHT , RIGHT_LEFT , RIGHT_RIGHT };
    
    final static int DONT_KNOW     = -1 ,
                     FALSE         = 0 ,
                     TRUE          = 1 ;
    
//    enum LinkBoolean { DONT_KNOW , FALSE , TRUE };
    
    // Members
    
    LeastCandidatesNumber lcn ;
    
    LeastCandidatesCell lcc ;

    IStrategy better ;

    InvulnerableState invulnerableState ;
    
    LinearSystemState linearSystemState ;
    
    boolean useDisjointSubsets ,
            useLockedSectorCandidates ,
            useTwoSectorDisjointSubsets ,
            useSingleValuedChains ,
            useManyValuedChains ,
            useNishio ,
            useAdjacentSectorPermutation ,
            useGuesses ,
            updateInvulnerableState ,
            updateLinearSystemState ,
            reportChains ,
            reportPerms ;
            
    int singleCandidatureCalls ,
        disjointSubsetsCalls ,
        disjointSubsetsEliminations ,
        maxDisjointSubsetsSize ,
        lockedSectorCandidatesCalls ,
        lockedSectorCandidatesEliminations ,
        twoSectorDisjointSubsetsCalls ,
        twoSectorDisjointSubsetsEliminations ,
        maxtwoSectorDisjointSubsetsSize ,
        chainsEliminations ,
        singleValuedChainsCalls ,
        singleValuedChainsEliminations ,
        manyValuedChainsCalls ,
        manyValuedChainsEliminations ,
        nishioCalls ,
        nishioEliminations ,
        adjacentSectorPermutationCalls ,
        adjacentSectorPermutationEliminations ,
        nGuesses ,
        nEliminated ;
    
    short nChains ;
    
    byte[] eliminatedX ,
           eliminatedY ,
           eliminatedValues ;
          
    // Arrays defined as members in order to improve performance.

    transient int linkedValuesSize , linkedCellsSize ;
    
    transient int[] x , y , linkedValues , linkedCells ;
    
    transient byte[] chainR0 , 
                     chainC0 , 
                     chainR1 , 
                     chainC1 ,
                     chainV0 ,
                     chainV1 ,
                     chainLength /*,
                     chainNComponents*/ ;
    
    transient byte[][] chainOtherEnd0 ,
                       chainOtherEnd1 ;
    
    transient short[][][] chainSectorTableSize ,
                          chainCellTableSize ;
                       
    transient boolean[][][][] chainSectorTableEnd0 ,
                              chainCellTableEnd0 ;
    
    transient short[][][][] chainSectorTableIndex ,
                            chainCellTableIndex ;
    
    transient short[] chainTableIndices ,
                      linkCategory ;

    transient boolean[][] isLinkAscending ;
    
    transient boolean[] isCellUsed ,
                        isValueUsed ,
                        isValueInSet1 ,
                        isValueInSet2 ;
            
    transient short[][] chainRoute /*,
                        chainComponents*/ ; 

    transient Nishio[][] mask ;
    
    transient byte[][][] sectorPerms ;
    
    transient int[] nSectorPerms ;
    
    transient int[][][] sectorOffsetCount ;
    
    // Max array dimensions
    
    public final static short maxChains       = 30000 ,
                              maxTabledChains = 2500 ,
                              maxChainLength  = 15 ,
                              maxSectorPerms  = 500 ;
    
    /**
     * Sets up a LeastCandidatesHybrid II strategy with an optional random element.
     * @param randomize whether the final candidates should be chosen randomly from its peers
     * @param updateInvulnerableState indicates whether the moves should be post-filtered using the Invulnerable state grid.
     * @param updateLinearState indicates whether the LinearStateGrid should be updated
     * @param useAllLogicalMethods whether the solver should look for X-Wings and Nishio
     * @param explain whether explanatory debug should be produced
     */    
    
    public LeastCandidatesHybrid( boolean randomize , 
                                  boolean updateInvulnerableState ,
                                  boolean updateLinearSystemState ,
                                  boolean useAllLogicalMethods ,
                                  boolean explain ){
        super( randomize , explain );
        lcn = new LeastCandidatesNumber( randomize || updateInvulnerableState , randomize , explain );
        lcc = new LeastCandidatesCell( randomize || updateInvulnerableState , randomize , explain );        
        if( ( this.updateInvulnerableState = updateInvulnerableState ) ){
            invulnerableState = new InvulnerableState();
        }
        if( ( this.updateLinearSystemState = updateLinearSystemState ) ){
            linearSystemState = new LinearSystemState();
        }
        // It's not strictly necessary for the Invulnerable State grid to be updated in order for
        // Single Sector Candidates and Disjoint Subsets to work but they are a general feature
        // of Least Candidates Hybrid II, so they default to true.
        useDisjointSubsets = useLockedSectorCandidates = updateInvulnerableState ;
        useTwoSectorDisjointSubsets = useSingleValuedChains = useManyValuedChains = useNishio = useAdjacentSectorPermutation = useAllLogicalMethods ;
        useGuesses = true ;
    }

    /**
     * Sets up the strategy to solve the given grid.
     * @see com.act365.sudoku.IStrategy#setup(Grid)
     */
        
    @Override public void setup( Grid grid ) throws Exception {
        super.setup( grid );
        lcn.setup( grid );
        lcc.setup( grid );
        if( updateInvulnerableState ){
            invulnerableState.setup( grid.boxesAcross , grid.boxesDown );
        }
        if( updateLinearSystemState ){
            linearSystemState.setup( grid.boxesAcross , grid.boxesDown );
        }
        if( updateInvulnerableState || updateLinearSystemState ){
            int i , j ;
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( grid.data[i][j] > 0 ){
                        if( updateInvulnerableState ){
                            invulnerableState.addMove( i , j , grid.data[i][j] - 1 );
                        }
                        if( updateLinearSystemState ){
                            invulnerableState.addMove( i , j , grid.data[i][j] - 1 );
                        }
                    }
                    ++ j ;
                }
                ++ i ;
            }
        }
        if( explain ){
            if( updateInvulnerableState ){
                invulnerableState.pushState( 0 );
            }
/*                
            if( updateLinearState ){
                linearSystemState.pushState( 0 );
            }
*/                
            eliminatedX = new byte[2*grid.cellsInRow];
            eliminatedY = new byte[2*grid.cellsInRow];
            eliminatedValues = new byte[2*grid.cellsInRow];
        }
        if( useDisjointSubsets ){
            x = new int[grid.cellsInRow];
            y = new int[grid.cellsInRow];
            linkedValues = new int[grid.cellsInRow];
            linkedCells = new int[grid.cellsInRow];
            isCellUsed = new boolean[grid.cellsInRow];
            isValueUsed = new boolean[grid.cellsInRow];
            isValueInSet1 = new boolean[grid.cellsInRow];
            isValueInSet2 = new boolean[grid.cellsInRow];
        }
        if( useSingleValuedChains || useManyValuedChains ){
            reportChains = true ;
            chainR0 = new byte[maxChains];
            chainC0 = new byte[maxChains];
            chainR1 = new byte[maxChains];
            chainC1 = new byte[maxChains];
            chainV0 = new byte[maxChains];
            chainV1 = new byte[maxChains];
            chainLength = new byte[maxChains];
            chainOtherEnd0 = new byte[maxChains][2];
            chainOtherEnd1 = new byte[maxChains][2];
            linkCategory = new short[maxChains];
                
            chainSectorTableSize = new short[3*grid.cellsInRow][grid.cellsInRow][2];
            chainCellTableSize = new short[grid.cellsInRow][grid.cellsInRow][2];
            chainSectorTableEnd0 = new boolean[3*grid.cellsInRow][grid.cellsInRow][2][maxTabledChains];
            chainCellTableEnd0 = new boolean[grid.cellsInRow][grid.cellsInRow][2][maxTabledChains];
            chainSectorTableIndex = new short[3*grid.cellsInRow][grid.cellsInRow][2][maxTabledChains];
            chainCellTableIndex = new short[grid.cellsInRow][grid.cellsInRow][2][maxTabledChains];
            chainTableIndices = new short[grid.cellsInRow];
                
            if( explain && reportChains ){
                chainRoute = new short[maxChains][maxChainLength];
                isLinkAscending = new boolean[maxChains][maxChainLength];
/*                
                chainNComponents = new byte[maxChains];
                chainComponents = new short[maxChains][maxChainLength];                    
*/
            }
        }
        if( useNishio ){
            mask = new Nishio[grid.cellsInRow][grid.cellsInRow];
        }
        if( useAdjacentSectorPermutation ){
            reportPerms = true ;
            sectorPerms = new byte[grid.cellsInRow][maxSectorPerms][grid.cellsInRow];
            nSectorPerms = new int[grid.cellsInRow];
            sectorOffsetCount = new int[2][grid.cellsInRow][grid.cellsInRow];
        }
        maxDisjointSubsetsSize = maxtwoSectorDisjointSubsetsSize = explain ? grid.cellsInRow : 6 /* Rule-of-thumb */ ;
        singleCandidatureCalls = 0 ;
        disjointSubsetsCalls = disjointSubsetsEliminations = 0 ;
        lockedSectorCandidatesCalls = lockedSectorCandidatesEliminations = 0 ;
        twoSectorDisjointSubsetsCalls = twoSectorDisjointSubsetsEliminations = 0 ;
        singleValuedChainsCalls = singleValuedChainsEliminations = 0 ;
        manyValuedChainsCalls = manyValuedChainsEliminations = 0 ;
        nishioCalls = nishioEliminations = 0 ;
        adjacentSectorPermutationCalls = adjacentSectorPermutationEliminations = 0 ;
        nGuesses = 0 ;
        nEliminated = 0 ;
    }
    
	/**
     * Finds candidates for the next move.
     * The LeastCandidatesCell search is performed before the LeastCandidatesNumber
     * search because it will find a result much quicker in the very common case
     * that very few candidates exist.  
	 * @see com.act365.sudoku.IStrategy#findCandidates()
	 */
     
	public int findCandidates() {
        
        StringBuilder sb = explain ? new StringBuilder() : null ;

        try {
            singleCandidature();
        } catch ( Exception e ){
            score = 0 ;
            return ( nCandidates = 0 );            
        }
        
        // When no indisputable candidate exists, employ the various
        // rules in order to try to eliminate candidates.
        // The code is only executed for Least Candidates Hybrid II. 
        if( score > 1 ){
            nEliminated = 0 ;
            try {
               while( true ){
                    if( useLockedSectorCandidates && lockedSectorCandidates( sb ) ){
                        if( explain && nEliminated > 0 ){
                            appendEliminations( sb );
                        }
                        if( singleCandidature() ){
                            break ;
                        }
                        continue ;
                    }
                    if( useDisjointSubsets && disjointSubsets( sb ) ){
                        if( explain && nEliminated > 0 ){
                            appendEliminations( sb );
                        }
                        if( singleCandidature() ){
                            break ;
                        }
                        continue ;
                    }
                    if( useTwoSectorDisjointSubsets && twoSectorDisjointSubsets( sb ) ){
                        if( explain && nEliminated > 0 ){
                            appendEliminations( sb );
                        }
                        if( singleCandidature() ){
                            break ;
                        }
                        continue ;
                    }
                    if( useSingleValuedChains && singleValuedChains( sb ) ){
                        if( explain && nEliminated > 0 ){
                            appendEliminations( sb );
                        }
                        if( singleCandidature() ){
                            break ;
                        }
                        continue ;
                    }
                   if( useManyValuedChains && manyValuedChains( sb ) ){
                       if( explain && nEliminated > 0 ){
                           appendEliminations( sb );
                       }
                       if( singleCandidature() ){
                           break ;
                       }
                       continue ;
                   }
                   if( useNishio && nishio( sb ) ){
                       if( explain && nEliminated > 0 ){
                           appendEliminations( sb );
                       }
                       if( singleCandidature() ){
                           break ;
                       }
                       continue ;
                   }
                   if( useAdjacentSectorPermutation && adjacentSectorPermutation( sb ) ){
                       if( explain && nEliminated > 0 ){
                           appendEliminations( sb );
                       }
                       if( singleCandidature() ){
                           break ;
                       }
                       continue ;
                   }
                   break ;
                }
            } catch ( Exception e ) {
                score = 0 ;
                return ( nCandidates = 0 );
            }
        }
        if( score > 1 ){
            if( useGuesses ){
                ++ nGuesses ;
            } else {
                score = 0 ;
                return ( nCandidates = 0 );
            }
        }
        nCandidates = 0 ;        
        while( nCandidates < better.getNumberOfCandidates() ){
            xCandidates[nCandidates] = better.getXCandidate( nCandidates );    
            yCandidates[nCandidates] = better.getYCandidate( nCandidates );    
            valueCandidates[nCandidates] = better.getValueCandidate( nCandidates );
            if( explain ){
                reasonCandidates[nCandidates] = new StringBuilder();
                reasonCandidates[nCandidates].append( sb.toString() );
                reasonCandidates[nCandidates].append( better.getReasonCandidate( nCandidates ) );
            }
            ++ nCandidates ;    
        }        
/*        
        if( explain && nCandidates == 0 ){
            System.out.println( sb );
        }
*/
        if( updateInvulnerableState ){
            int i , minInvulnerable = Integer.MAX_VALUE ;
            i = 0 ;
            while( i < better.getNumberOfCandidates() ){
                if( invulnerableState.nInvulnerable[valueCandidates[i]-1][xCandidates[i]][yCandidates[i]] < minInvulnerable ){
                    minInvulnerable = invulnerableState.nInvulnerable[valueCandidates[i]-1][xCandidates[i]][yCandidates[i]];    
                }
                ++ i ;
            }
            nCandidates = 0 ;
            i = 0 ;
            while( i < better.getNumberOfCandidates() ){
                if( invulnerableState.nInvulnerable[valueCandidates[i]-1][xCandidates[i]][yCandidates[i]] == minInvulnerable ){
                    xCandidates[nCandidates] = xCandidates[i];    
                    yCandidates[nCandidates] = yCandidates[i];    
                    valueCandidates[nCandidates] = valueCandidates[i];
                    if( explain ){
                        reasonCandidates[nCandidates] = reasonCandidates[i];
                    }
                    ++ nCandidates ;    
                    if( ! randomize ){
                        return nCandidates ;
                    }
                }
                ++ i ;
            }
        }
        
        return nCandidates ;
	}

    /**
     * Eliminates the move (x,y):=v from all state grids.
     */

    void eliminateMove( int x , int y , int v ){
        ((NumberState) lcn.state ).eliminateMove( x , y , v );
        ((CellState) lcc.state ).eliminateMove( x , y , v );
        if( updateInvulnerableState ){
            invulnerableState.eliminateMove( x , y , v );
        }
        if( updateLinearSystemState ){
            linearSystemState.eliminateMove( x , y , v );
        }
        if( explain ){
            eliminatedX[nEliminated] = (byte) x ;
            eliminatedY[nEliminated] = (byte) y ;
            eliminatedValues[nEliminated] = (byte) v ;
            ++ nEliminated ;            
        }
    }
    
    /**
     * Adds the move (x,y):=v to all state grids.
     */

    int addMove( int x , int y , int v ) {
        CellState cellState = (CellState) lcc.state ; 
        int i = 0 , nEliminated = 0 ;
        while( i < grid.cellsInRow ){
            if( i != v && ! cellState.eliminated[x][y][i] ){
                eliminateMove( x , y , i );
                ++ nEliminated ;
            }
            ++ i ;
        }
        return nEliminated ;
    }
    
    /**
     * Appends a summary of moves eliminated in the current 
     * cycle to the given string Builder.
     */

    void appendEliminations( StringBuilder sb ){
        sb.append("- The move");
        if( nEliminated > 1 ){
            sb.append("s");
        }
        int i = 0 ;
        while( i < nEliminated ){
            if( i == 0 ){
                sb.append(' ');
            } else if( i < nEliminated - 1 ) {
                sb.append(", ");
            } else {
                sb.append(" and ");
            }
            SuDokuUtils.appendMove( sb , eliminatedX[i] , eliminatedY[i] , eliminatedValues[i] );
            ++ i ;
        }
        sb.append(" ha");
        if( nEliminated == 1 ){
            sb.append("s");
        } else {
            sb.append("ve");
        }
        sb.append(" been eliminated.\n");
        nEliminated = 0 ;
    }
        
    /**
     * Determines which underlying strategy to prefer.  
     * @return whether an undisputed candidate has been found
     * @throws Exception bad grid state
     */
    
    boolean singleCandidature() throws Exception {
        ++ singleCandidatureCalls ;
        if( lcc.findCandidates() == 0 || lcc.getScore() > 1 && lcn.findCandidates() == 0 ){
            throw new Exception("Bad grid state");
        }
        if( lcc.getScore() == 1 || lcc.getScore() < lcn.getScore() ){
            better = lcc ;
        } else {
            better = lcn ;
        }
        return ( score = better.getScore() ) == 1 ;
    }
    
    /**
     * Checks whether some strictly smaller subset of the candidates for a 
     * row, column or box will fit into some subset of the available cells,
     * in which case eliminations will be possible. 
     * @param sb explanation
     * @return whether eliminations have been performed
     * @throws Exception the grid is in a bad state
     */
    
    boolean disjointSubsets( StringBuilder sb ) throws Exception {
        ++ disjointSubsetsCalls ;
        final CellState cellState = (CellState) lcc.state ;
        boolean anyMoveEliminated = false ;
        int i , j , k , s ;
        s = 0 ;
        while( s < 3 * grid.cellsInRow ){
            linkedValues[0] = 0 ;
            while( linkedValues[0] < grid.cellsInRow && findDisjointSubsets( s ) ){
                i = 0 ;
                while( i < linkedCellsSize ){
                    if( s < grid.cellsInRow ){
                        x[i] = s ;
                        y[i] = linkedCells[i] ;
                    } else if( s < 2 * grid.cellsInRow ){
                        x[i] = linkedCells[i] ;
                        y[i] = s - grid.cellsInRow ;
                    } else {
                        x[i] = ( s - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + linkedCells[i] / grid.boxesDown ;
                        y[i] = ( s - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + linkedCells[i] % grid.boxesDown ;
                    }
                    j = 0 ;
                    eliminate:
                    while( j < grid.cellsInRow ){
                        if( cellState.eliminated[x[i]][y[i]][j] ){
                            ++ j ;
                            continue ;
                        }                                    
                        k = 0 ;
                        while( k < linkedValuesSize ){
                            if( j == linkedValues[k] ){
                                ++ j ;
                                continue eliminate ;
                            }
                            ++ k ;
                        }
                        eliminateMove( x[i] , y[i] , j );
                        anyMoveEliminated = true ;
                        ++ disjointSubsetsEliminations ; 
                        ++ j ;
                    }
                    ++ i ;
                }
                if( anyMoveEliminated ){
                    if( explain ){
                        sb.append("The values ");
                        SuDokuUtils.appendValue( sb , linkedValues[0] );
                        i = 1 ;
                        while( i < linkedValuesSize - 1 ){
                            sb.append(", ");
                            SuDokuUtils.appendValue( sb , linkedValues[i++] );
                        }
                        sb.append(" and ");
                        SuDokuUtils.appendValue( sb , linkedValues[i] );
                        sb.append(" occupy the cells ");
                        SuDokuUtils.appendCell( sb , x[0] , y[0] );
                        i = 1 ;
                        while( i < linkedCellsSize - 1 ){
                            sb.append(", ");
                            SuDokuUtils.appendCell( sb , x[i] , y[i] );
                            ++ i ;
                        }
                        sb.append(" and ");
                        SuDokuUtils.appendCell( sb , x[i] , y[i] );
                        sb.append(" in some order.\n");
                    }
                    return true ;
                }
                ++ linkedValues[0];
            }
            ++ s ;
        }
        return false ;
    }
  
    boolean twoSectorDisjointSubsets( StringBuilder sb ) throws Exception {
        final CellState cellState = (CellState) lcc.state ;
        int b , r , c , v , br , bc , rStart , rEnd , cStart , cEnd ;
        int nCells , nValues , nIntersectionCells , nSet1Cells , nSet1Values , nSet2Cells , nSet2Values ;
        boolean anyMoveEliminated = false , headerAdded ;
        b = 0 ;
        while( b < grid.cellsInRow ){
            rStart = b / grid.boxesAcross * grid.boxesAcross ;
            rEnd   = ( b / grid.boxesAcross + 1 )* grid.boxesAcross ;
            cStart = b % grid.boxesAcross * grid.boxesDown ;
            cEnd   = ( b % grid.boxesAcross + 1 )* grid.boxesDown ;
            // Consider each row.
            r = rStart ;
            while( r < rEnd ){
                // Calculate the complete list of candidates for the intersection cells.
                nValues = nCells = 0 ;
                v = 0 ;
                while( v < grid.cellsInRow ){
                    isValueUsed[v] = isValueInSet1[v] = isValueInSet2[v] = false ;
                    ++ v ;
                }
                c = cStart ;
                while( c < cEnd ){
                    if( cellState.nEliminated[r][c] < grid.cellsInRow - 1 ){
                        v = 0 ;
                        while( v < grid.cellsInRow ){
                            if( ! isValueUsed[v] && ! cellState.eliminated[r][c][v] ){
                                isValueUsed[v] = true ;
                                ++ nValues ;
                            }
                            ++ v ;
                        }
                        x[nCells] = r ;
                        y[nCells] = c ;
                        ++ nCells ;
                    }
                    ++ c ;
                }
                nIntersectionCells = nCells ;
                // Attempt to construct a symmetric difference.
                if( nValues - nCells < 2 ){
                    ++ r ;
                    continue ;
                }
                // Look for suitable cells along the row (Set 1).
                nSet1Cells = nSet1Values = 0 ;
                c = 0 ;
                while(  c < grid.cellsInRow && nCells < nValues ){
                    if( ( c < cStart || c > cEnd ) && cellState.nEliminated[r][c] < grid.cellsInRow - 1 ){
                        v = 0 ;
                        while( v < grid.cellsInRow ){
                            if( ! cellState.eliminated[r][c][v] && ! isValueUsed[v] ){
                                break ;
                            }
                            ++ v ;
                        }
                        if( v == grid.cellsInRow ){
                            // Each candidate for this cell is an element of the union.
                            x[nCells] = r ;
                            y[nCells] = c ;
                            ++ nCells ;
                            ++ nSet1Cells ;
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                if( ! cellState.eliminated[r][c][v] && ! isValueInSet1[v] ){
                                    isValueInSet1[v] = true ;
                                    ++ nSet1Values ;
                                }
                                ++ v ;
                            }
                        }
                    }
                    ++ c ;
                }
                // Look for suitable cells in the box (Set 2).
                nSet2Cells = nSet2Values = 0 ;
                br = rStart ;
                while( br < rEnd && nCells < nValues ){
                    if( br == r ){
                        ++ br ;
                        continue ;
                    }
                    bc = cStart ;
                    while( bc < cEnd && nCells < nValues ){
                        if( cellState.nEliminated[br][bc] < grid.cellsInRow - 1 ){
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                if( ! cellState.eliminated[br][bc][v] && ( ! isValueUsed[v] || isValueInSet1[v] ) ){
                                    break ;
                                }
                                ++ v ;
                            }
                            if( v == grid.cellsInRow ){
                                x[nCells] = br ;
                                y[nCells] = bc ;
                                ++ nCells ;                            
                                ++ nSet2Cells ;
                                v = 0 ;
                                while( v < grid.cellsInRow ){
                                    if( ! cellState.eliminated[br][bc][v] && ! isValueInSet2[v] ){
                                        isValueInSet2[v] = true ;
                                        ++ nSet2Values ;
                                    }
                                    ++ v ;
                                }
                            }                        
                        }
                        ++ bc ;
                    }                    
                    ++ br ;
                }
                // Have we succeeded?
                if( nCells == nValues && nSet1Cells > 0 && nSet2Cells > 0 ){
                    
                    anyMoveEliminated |= ( headerAdded = set1Eliminations( sb , 
                                                                           r , 
                                                                           nIntersectionCells ,
                                                                           nSet1Values ,
                                                                           nSet1Cells , 
                                                                           nSet2Values ,
                                                                           nSet2Cells ) );
                    
                    anyMoveEliminated |= set2Eliminations( sb , 
                                                           b , 
                                                           nIntersectionCells ,
                                                           nSet1Values ,
                                                           nSet1Cells , 
                                                           nSet2Values ,
                                                           nSet2Cells ,
                                                           headerAdded );
                }
                ++ r ;
            }
            // Consider each column.
            c = cStart ;
            while( c < cEnd ){
                // Calculate the complete list of candidates for the intersection cells.
                nValues = nCells = 0 ;
                v = 0 ;
                while( v < grid.cellsInRow ){
                    isValueUsed[v] = isValueInSet1[v] = isValueInSet2[v] = false ;
                    ++ v ;
                }
                r = rStart ;
                while( r < rEnd ){
                    if( cellState.nEliminated[r][c] < grid.cellsInRow - 1 ){
                        v = 0 ;
                        while( v < grid.cellsInRow ){
                            if( ! isValueUsed[v] && ! cellState.eliminated[r][c][v] ){
                                isValueUsed[v] = true ;
                                ++ nValues ;
                            }
                            ++ v ;
                        }
                        x[nCells] = r ;
                        y[nCells] = c ;
                        ++ nCells ;
                    }
                    ++ r ;
                }
                nIntersectionCells = nCells ;
                // Attempt to construct a symmetric difference.
                if( nValues - nCells < 2 ){
                    ++ c ;
                    continue ;
                }
                // Look for suitable cells along the column (Set 1).
                nSet1Cells = nSet1Values = 0 ;
                r = 0 ;
                while(  r < grid.cellsInRow && nCells < nValues ){
                    if( ( r < rStart || r > rEnd ) && cellState.nEliminated[r][c] < grid.cellsInRow - 1 ){
                        v = 0 ;
                        while( v < grid.cellsInRow ){
                            if( ! cellState.eliminated[r][c][v] && ! isValueUsed[v] ){
                                break ;
                            }
                            ++ v ;
                        }
                        if( v == grid.cellsInRow ){
                            // Each candidate for this cell is an element of the union.
                            x[nCells] = r ;
                            y[nCells] = c ;
                            ++ nCells ;
                            ++ nSet1Cells ;
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                if( ! cellState.eliminated[r][c][v] && ! isValueInSet1[v] ){
                                    isValueInSet1[v] = true ;
                                    ++ nSet1Values ;
                                }
                                ++ v ;
                            }
                        }
                    }
                    ++ r ;
                }
                // Look for suitable cells in the box (Set 2).
                nSet2Cells = nSet2Values = 0 ;
                 br = rStart ;
                while( br < rEnd && nCells < nValues ){
                    bc = cStart ;
                    while( bc < cEnd && nCells < nValues ){
                        if( bc == c ){
                            ++ bc ;
                            continue ;
                        }
                        if( cellState.nEliminated[br][bc] < grid.cellsInRow - 1 ){
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                if( ! cellState.eliminated[br][bc][v] && ( ! isValueUsed[v] || isValueInSet1[v] ) ){
                                    break ;
                                }
                                ++ v ;
                            }
                            if( v == grid.cellsInRow ){
                                x[nCells] = br ;
                                y[nCells] = bc ;
                                ++ nCells ;                            
                                ++ nSet2Cells ;
                                v = 0 ;
                                while( v < grid.cellsInRow ){
                                    if( ! cellState.eliminated[br][bc][v] && ! isValueInSet2[v] ){
                                        isValueInSet2[v] = true ;
                                        ++ nSet2Values ;
                                    }
                                    ++ v ;
                                }
                            }                        
                        }
                        ++ bc ;
                    }                    
                    ++ br ;
                }
                // Have we succeeded?
                if( nCells == nValues && nSet1Cells > 0 && nSet2Cells > 0 ){
                    
                    anyMoveEliminated |= ( headerAdded = set1Eliminations( sb , 
                                                                           c + grid.cellsInRow , 
                                                                           nIntersectionCells ,
                                                                           nSet1Values ,
                                                                           nSet1Cells , 
                                                                           nSet2Values ,
                                                                           nSet2Cells ) );
                    
                    anyMoveEliminated |= set2Eliminations( sb , 
                                                           b , 
                                                           nIntersectionCells ,
                                                           nSet1Values ,
                                                           nSet1Cells , 
                                                           nSet2Values ,
                                                           nSet2Cells ,
                                                           headerAdded );
                }
                ++ c ;
            }
            ++ b ;
        }
        return anyMoveEliminated ;
    }

    /**
     * Looks for (almost) disjoint subsets in sector s.
     * The lowest-valued candidate is returned if the function is successful,
     * otherwise grid.cellsInRow is returned. The number of cells occupied by
     * the value set is allowed to exceed the size of the value set by at most
     * offset. The arrays linkedValues and linkedCells are populated. 
     */

    boolean findDisjointSubsets( int s , int offset ) throws Exception {
        NumberState numberState = (NumberState) lcn.state ;
        int i , j , nUnfilled , nUnconsideredValues ;
        // Calculate the number of unfilled cells in the sector.
        nUnfilled = 0 ;
        i = 0 ;
        while( i < grid.cellsInRow ){
            if( ! numberState.isFilled[i][s] ){
                ++ nUnfilled ;
            }
            ++ i ;
        }
        // Ensure that the last value in the subset is sensible.
        while( linkedValues[0] < grid.cellsInRow &&
               ( numberState.nEliminated[linkedValues[0]][s] == 0 ||
                 numberState.nEliminated[linkedValues[0]][s] == grid.cellsInRow - 1 ) ){
               ++ linkedValues[0] ;
        }
        if( linkedValues[0] == grid.cellsInRow ){
            return false ;
        }
        // Count the number of unconsidered values.
        nUnconsideredValues = 0 ;
        i = linkedValues[0] + 1 ;
        while( i < grid.cellsInRow ){
            if( ! numberState.isFilled[i][s] ){
                ++ nUnconsideredValues ;
            }
            ++ i ;
        }
        // Calculate union size
        linkedValuesSize = 1 ;
        linkedCellsSize = grid.cellsInRow - numberState.nEliminated[linkedValues[0]][s] ;                    
        while( true ){
            // Check the union size.
            if( linkedCellsSize < linkedValuesSize ){
                throw new Exception("Bad grid state");
            } else if( linkedCellsSize < linkedValuesSize + offset && linkedCellsSize > 1 && linkedCellsSize < nUnfilled ) {
                // Legal but not as requested.
                return false ;
            } else if( linkedCellsSize == linkedValuesSize + offset && linkedCellsSize > 1 && linkedCellsSize < nUnfilled ){
                i = 0 ;
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( isCellUsed[j] ){
                        linkedCells[i++] = j ;
                    }
                    ++ j ;
                }
                return true ;
            } else if( linkedCellsSize >= maxDisjointSubsetsSize || linkedCellsSize > linkedValuesSize + nUnconsideredValues || linkedCellsSize >= nUnfilled ) {
                ++ linkedValues[linkedValuesSize-1];
            } else {
                linkedValues[linkedValuesSize] = linkedValues[linkedValuesSize-1] + 1 ;
                ++ linkedValuesSize ;
            }
            // Ensure that the last value in the subset is sensible.
            while( linkedValuesSize > 0 ){
                while( linkedValues[linkedValuesSize-1] < grid.cellsInRow &&
                       ( numberState.nEliminated[linkedValues[linkedValuesSize-1]][s] == 0 ||
                         numberState.nEliminated[linkedValues[linkedValuesSize-1]][s] == grid.cellsInRow - 1 ) ){
                       ++ linkedValues[linkedValuesSize-1];
                }
                if( linkedValues[linkedValuesSize-1] == grid.cellsInRow ){
                    if( -- linkedValuesSize > 0 ){
                        ++ linkedValues[linkedValuesSize-1];
                    }
                } else {
                    break;
                }
            }
            if( linkedValuesSize == 0 ){
                break;
            }                            
            // Count the number of unconsidered values.
            nUnconsideredValues = 0 ;
            i = linkedValues[linkedValuesSize-1] + 1 ;
            while( i < grid.cellsInRow ){
                if( ! numberState.isFilled[i][s] ){
                    ++ nUnconsideredValues ;
                }
                ++ i ;
            }
            // Calculate the union size for the new subset.
            linkedCellsSize = 0 ;
            i = 0 ;
            while( i < grid.cellsInRow ){
                isCellUsed[i] = isValueUsed[i] = false ;
                ++ i ;
            }
            j = 0 ;
            while( j < linkedValuesSize ){
                isValueUsed[linkedValues[j]] = true ;
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( ! isCellUsed[i] && ! numberState.eliminated[linkedValues[j]][s][i] ){
                        isCellUsed[i] = true ;
                        ++ linkedCellsSize ;
                    }
                    ++ i ;
                }
                ++ j ;
            }
        }
        return false ;
    }

    boolean findDisjointSubsets( int s ) throws Exception {
        return findDisjointSubsets( s , 0 );
    }
    
    boolean set1Eliminations( StringBuilder sb , 
                              int s , 
                              int nIntersectionCells , 
                              int nSet1Values ,
                              int nSet1Cells ,
                              int nSet2Values ,
                              int nSet2Cells ){
        final CellState cellState = (CellState) lcc.state ;
        final int nCells = nSet1Cells + nSet2Cells + nIntersectionCells ;
        boolean anyMoveEliminated = false ;
        int i , r , c , v , offset ;
        offset = 0 ;
        scanSector:
        while( offset < grid.cellsInRow ){
            if( s < grid.cellsInRow ){
                r = s ;
                c = offset ;
            } else {
                r = offset ;
                c = s - grid.cellsInRow ;
            }
            if( cellState.nEliminated[r][c] == grid.cellsInRow - 1 ){
                ++ offset ;
                continue ;
            }
            i = 0 ;
            while( i < nCells ){
                if( r == x[i] && c == y[i] ){
                    ++ offset ;
                    continue scanSector ;
                }
                ++ i ;
            }
            i = 0 ;
            while( i < grid.cellsInRow ){
                if( ! cellState.eliminated[r][c][i] && isValueUsed[i] && ! isValueInSet2[i] ){
                    anyMoveEliminated = true ;
                    eliminateMove( r , c , i );
                    ++ twoSectorDisjointSubsetsEliminations ;
                }
                ++ i ;
            }
            ++ offset ;
        }
        if( anyMoveEliminated && explain ){
            appendTwoSectorHeader( sb , 
                                   nIntersectionCells ,
                                   nSet1Values ,
                                   nSet1Values - nSet1Cells ,
                                   nSet2Values ,
                                   nSet2Values - nSet2Cells );
            sb.append("The values ");
            i = 0 ;
            v = 0 ;
            while( v < grid.cellsInRow ){
                if( isValueUsed[v] && ! isValueInSet2[v] ){
                    SuDokuUtils.appendValue( sb , v );
                    if( i < nCells - nSet2Values - 2 ){
                        sb.append(", ");
                    } else if( i == nCells - nSet2Values - 2 ){
                        sb.append(" and ");
                    }
                    ++ i ;
                }
                ++ v ;
            }
            assert i == nCells - nSet2Values ;
            sb.append(" occupy ");
            sb.append( i );
            sb.append(" of the cells ");
            i = 0 ;
            while( i < nCells - nSet2Cells ){
                SuDokuUtils.appendCell( sb , x[i] , y[i] );
                if( i < nCells - nSet2Cells - 2 ){
                    sb.append(", ");
                } else if( i == nCells - nSet2Cells - 2 ){
                    sb.append(" and ");
                }
                ++ i ;
            }
            sb.append(" in some order.\n");            
        }
        return anyMoveEliminated ;
    }

    boolean set2Eliminations( StringBuilder sb , 
                              int b , 
                              int nIntersectionCells , 
                              int nSet1Values ,
                              int nSet1Cells ,
                              int nSet2Values ,
                              int nSet2Cells ,
                              boolean headerAppended ){
        final CellState cellState = (CellState) lcc.state ;
        final int nCells = nSet1Cells + nSet2Cells + nIntersectionCells ;
        final int rStart = b / grid.boxesAcross * grid.boxesDown ,
                  rEnd   = ( b / grid.boxesAcross + 1 )* grid.boxesDown ,
                  cStart = b % grid.boxesAcross * grid.boxesAcross ,
                  cEnd   = ( b % grid.boxesAcross + 1 )* grid.boxesAcross ;
        boolean anyMoveEliminated = false ;
        int i , r , c , v ;
        r = rStart ;
        while( r < rEnd ){
            c = cStart ;
            scanColumn:
            while( c < cEnd ){
                if( cellState.nEliminated[r][c] == grid.cellsInRow - 1 ){
                    ++ c ;
                    continue ;
                }
                i = 0 ;
                while( i < nCells ){
                    if( r == x[i] && c == y[i] ){
                        ++ c ;
                        continue scanColumn ;
                    }
                    ++ i ;
                }
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( ! cellState.eliminated[r][c][i] && isValueUsed[i] && ! isValueInSet1[i] ){
                        anyMoveEliminated = true ;
                        eliminateMove( r , c , i );
                        ++ twoSectorDisjointSubsetsEliminations ;
                    }
                    ++ i ;
                }
               
                ++ c ;
            }
            ++ r ;
        }
        if( anyMoveEliminated && explain ){
            if( ! headerAppended ){
                appendTwoSectorHeader( sb , 
                                       nIntersectionCells , 
                                       nSet1Values ,
                                       nSet1Values - nSet1Cells ,
                                       nSet2Values ,
                                       nSet2Values - nSet2Cells );
            }
            sb.append("The values ");
            i = 0 ;
            v = 0 ;
            while( v < grid.cellsInRow ){
                if( isValueUsed[v] && ! isValueInSet1[v] ){
                    SuDokuUtils.appendValue( sb , v );
                    if( i < nCells - nSet1Values - 2 ){
                        sb.append(", ");
                    } else if( i == nCells - nSet1Values - 2 ){
                        sb.append(" and ");
                    }
                    ++ i ;
                }
                ++ v ;
            }
            assert i == nCells - nSet1Values ;
            sb.append(" occupy ");
            sb.append( i );
            sb.append(" of the cells ");
            i = 0 ;
            while( i < nIntersectionCells ){
                SuDokuUtils.appendCell( sb , x[i] , y[i] );
                if( i < nCells - nSet2Cells - 2 ){
                    sb.append(", ");
                } else if( i == nCells - nSet2Cells - 2 ){
                    sb.append(" and ");
                }
                ++ i ;
            }
            i = nCells - nSet2Cells ;
            while( i < nCells ){
                SuDokuUtils.appendCell( sb , x[i] , y[i] );
                if( i < nCells - nSet2Cells - 2 ){
                    sb.append(", ");
                } else if( i == nCells - nSet2Cells - 2 ){
                    sb.append(" and ");
                }
                ++ i ;
            }
            sb.append(" in some order.\n");            
        }
        return anyMoveEliminated ;
    }
    
    StringBuilder appendTwoSectorHeader( StringBuilder sb , 
                                         int nIntersectionCells ,
                                         int nSet1Values ,
                                         int set1Overspill ,
                                         int nSet2Values ,
                                         int set2Overspill ){
        final int nIntersectionValues = nIntersectionCells - set1Overspill - set2Overspill ;
        int i , v ;
        sb.append("The cells ");
        i = 0 ;
        while( i < nIntersectionCells ){
            SuDokuUtils.appendCell( sb , x[i] , y[i] );
            if( i == nIntersectionCells - 2 ){
                sb.append(" and ");                
            } else if( i < nIntersectionCells - 2 ) {
                sb.append(", ");                
            }
            ++ i ;
        }
        sb.append(" contain ");
        // Intersection 
        if( nIntersectionValues > 0 ){
            sb.append("the value");
            if( nIntersectionValues == 1 ){
                sb.append(' ');
            } else {
                sb.append("s ");
            }
            i = 0 ;
            v = 0 ;
            while( v < grid.cellsInRow ){
                if( isValueUsed[v] && ! isValueInSet1[v] && ! isValueInSet2[v] ){
                    SuDokuUtils.appendValue( sb , v );
                    if( i == nIntersectionValues - 2 ){
                        sb.append(" and ");
                    } else {
                        sb.append(", ");
                    }
                    ++ i ;
                }
                ++ v ;
            }
            assert i == nIntersectionValues ;
        }
        // Set 1
        sb.append( set1Overspill );
        sb.append(" value");
        if( set1Overspill > 1 ){
            sb.append('s');
        }
        sb.append(" from {");
        i = 0 ;
        v = 0 ;
        while( v < grid.cellsInRow ){
            if( isValueInSet1[v] ){
                SuDokuUtils.appendValue( sb , v );
                if( i < nSet1Values - 1 ){
                    sb.append(',');
                }
                ++ i ;
            }
            ++ v ;
        }
        assert i == nSet1Values ;
        sb.append("} and ");
        // Set 2
        sb.append( set2Overspill );
        sb.append(" value");
        if( set1Overspill > 1 ){
            sb.append('s');
        }
        sb.append(" from {");
        i = 0 ;
        v = 0 ;
        while( v < grid.cellsInRow ){
            if( isValueInSet2[v] ){
                SuDokuUtils.appendValue( sb , v );
                if( i < nSet2Values - 1 ){
                    sb.append(',');
                }
                ++ i ;
            }
            ++ v ;
        }
        assert i == nSet2Values ;
        sb.append("}.\n");
        
        return sb ;
    }
    
    /**
     * Checks whether the candidates for a row, box or column are restricted to
     * a single sector, in which case eliminations might be possible.
     * @param sb explanation
     * @return whether eliminations have been performed
     * @throws Exception the grid is in a bad state
     */
    
    boolean lockedSectorCandidates( StringBuilder sb ) throws Exception {
        ++ lockedSectorCandidatesCalls ;
        CellState cellState = (CellState) lcc.state ;
        NumberState numberState = (NumberState) lcn.state ; 
        boolean anyMoveEliminated ;
        int i , j , s , value , box , row , column , x0 , y0 , xLower , xUpper , yLower , yUpper ;
        value = 0 ;
        while( value < grid.cellsInRow ){
            s = 0 ;
            while( s < 2 * grid.cellsInRow ){
                if( numberState.nEliminated[value][s] == grid.cellsInRow ){
                    throw new Exception("Bad grid state");
                } else if( numberState.nEliminated[value][s] == grid.cellsInRow - 1 ){
                    ++ s ;
                    continue ;
                }
                box = -1 ;
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( numberState.eliminated[value][s][i] ){
                        ++ i ;
                        continue ;
                    }
                    if( s < grid.cellsInRow ){
                        x0 = s ;
                        y0 = i ;
                    } else {
                        x0 = i ;
                        y0 = s - grid.cellsInRow ;
                    }
                    if( box == -1 ){
                        box = x0 / grid.boxesAcross * grid.boxesAcross + y0 / grid.boxesDown ;
                    } else if( box != x0 / grid.boxesAcross * grid.boxesAcross + y0 / grid.boxesDown ){
                        break;
                    }                        
                    ++ i ;
                }
                anyMoveEliminated = false ;
                if( i == grid.cellsInRow ){
                    xLower = box / grid.boxesAcross * grid.boxesAcross ;
                    xUpper = ( box / grid.boxesAcross + 1 )* grid.boxesAcross ;
                    yLower = box % grid.boxesAcross * grid.boxesDown ;
                    yUpper = ( box % grid.boxesAcross + 1 )* grid.boxesDown ;
                    j = 0 ;
                    x0 = xLower ;
                    while( x0 < xUpper ){
                        if( s < grid.cellsInRow && s == x0 ){
                            ++ x0 ;
                            continue ;
                        }
                        y0 = yLower ;
                        while( y0 < yUpper ){
                            if( s >= grid.cellsInRow && s - grid.cellsInRow == y0 ){
                                ++ y0 ;
                                continue ;
                            }
                            if( ! cellState.eliminated[x0][y0][value] ){
                                eliminateMove( x0 , y0 , value );
                                anyMoveEliminated = true ;
                                ++ lockedSectorCandidatesEliminations ;
                            }
                            ++ y0 ;                                
                        }
                        ++ x0 ;
                    }
                    if( anyMoveEliminated ){
                        if( explain ){
                            sb.append("The value ");
                            SuDokuUtils.appendValue( sb , value );
                            sb.append(" in ");
                            SuDokuUtils.appendBox( sb , grid.boxesAcross , box );
                            sb.append(" must lie in ");
                            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s );
                            sb.append(".\n");
                        }
                        return true ;
                    }
                }
                ++ s ;
            }
            while( s < 3 * grid.cellsInRow ){
                if( numberState.nEliminated[value][s] == grid.cellsInRow ){
                    throw new Exception("Bad grid state");
                } else if( numberState.nEliminated[value][s] == grid.cellsInRow - 1 ){
                    ++ s ;
                    continue ;
                }
                row = column = -1 ;
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( numberState.eliminated[value][s][i] ){
                        ++ i ;
                        continue ;
                    }
                    x0 = ( s - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + i / grid.boxesDown ;
                    y0 = ( s - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + i % grid.boxesDown ;
                    if( row == -1 && column == -1 ){
                        row = x0 ;
                        column = y0 ;                        
                    } else if( row == -1 ){
                        if( y0 != column ){
                            break ;
                        }
                    } else if( column == -1 ){
                        if( x0 != row ) {                                
                            break ;
                        }
                    } else {
                        if( x0 == row ){
                            column = -1 ;
                        } else if( y0 == column ){
                            row = -1 ;
                        } else {
                            break ;
                        }
                    }
                    ++ i ;
                }
                anyMoveEliminated = false ;
                if( i == grid.cellsInRow ){
                    xLower = ( s - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross ;
                    xUpper = ( ( s - 2 * grid.cellsInRow ) / grid.boxesAcross + 1 )* grid.boxesAcross ;
                    yLower = ( s - 2 * grid.cellsInRow ) % grid.boxesAcross * grid.boxesDown ;
                    yUpper = ( ( s - 2 * grid.cellsInRow ) % grid.boxesAcross + 1 )* grid.boxesDown ;
                    j = 0 ;
                    while( j < grid.cellsInRow ){
                        if( column == -1 ){
                            x0 = row ;
                            y0 = j ;
                        } else {
                            x0 = j ;
                            y0 = column ;
                        }
                        if( xLower <= x0 && x0 < xUpper && yLower <= y0 && y0 < yUpper ){
                            ++ j ;
                            continue ;
                        }
                        if( ! cellState.eliminated[x0][y0][value] ){
                            eliminateMove( x0 , y0 , value );
                            anyMoveEliminated = true ;
                            ++ lockedSectorCandidatesEliminations ;
                        }
                        ++ j ;
                    }
                    if( anyMoveEliminated ){
                        if( explain ){
                            sb.append("The value ");
                            SuDokuUtils.appendValue( sb , value );
                            sb.append(" in ");
                            if( column == -1 ){
                                SuDokuUtils.appendRow( sb , row );
                            } else {
                                SuDokuUtils.appendColumn( sb , column );
                            }
                            sb.append(" must lie in ");
                            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s );
                            sb.append(".\n");
                        }
                        return true ;
                    }
                }
                ++ s ;
            }
            ++ value ;
        }
        return false ;
    }
    
    boolean singleValuedChains( StringBuilder sb ) throws Exception {
        boolean anyMoveEliminated = false ;
        int v ;
        ++ singleValuedChainsCalls ;
        v = 0 ;
        while( ! anyMoveEliminated && v < grid.cellsInRow ){
            chainsEliminations = 0 ;
            // Use STRONG and WEAK links - nothing else.
            nChains = 0 ;
            addUnitChains( null , v , true , false , false , false );
            anyMoveEliminated = addLongChains( sb , true , false );
            singleValuedChainsEliminations += chainsEliminations ; 
            ++ v ;   
        }
        return anyMoveEliminated ;
    }
    
    boolean manyValuedChains( StringBuilder sb ) throws Exception {
        int v ;
        ++ manyValuedChainsCalls ;
        chainsEliminations = 0 ;
        // Just STRONG links.
        resetChainTables( false );
        v = 0 ;
        while( v < grid.cellsInRow ){
            addUnitChains( null , v , false , false , false , false );
            ++ v ;   
        }
        if( addLongChains( sb , false , false ) ){
            manyValuedChainsEliminations += chainsEliminations ;
            return true ;
        }
        // STRONG and WEAK links.
        resetChainTables( false );
        v = 0 ;
        while( v < grid.cellsInRow ){
            addUnitChains( null , v , true , false , false , false );
            ++ v ;   
        }
        if( addLongChains( sb , true , false ) ){
            manyValuedChainsEliminations += chainsEliminations ;
            return true ;
        }
        // STRONG and WEAK links with Tables.
        resetChainTables( true );
        v = 0 ;
        while( v < grid.cellsInRow ){
            addUnitChains( null , v , true , false , false , true );
            ++ v ;   
        }
        if( addLongChains( sb , true , true ) ){
            manyValuedChainsEliminations += chainsEliminations ;
            return true ;
        }
        // STRONG, WEAK and EXTENDED links with Tables.
        resetChainTables( true );
        v = 0 ;
        while( v < grid.cellsInRow ){
            addUnitChains( null , v , true , true , false , true );
            ++ v ;   
        }
        if( addLongChains( sb , true , true ) ){
            manyValuedChainsEliminations += chainsEliminations ;
            return true ;
        }
        // Give up.
        return false ;           
    }
    
    void resetChainTables( boolean useTable ){
        int i , s ;
        nChains = 0 ;
        if( useTable ){
            s = 0 ;
            while( s < 3 * grid.cellsInRow ){
                i = 0 ;
                while( i < grid.cellsInRow ){
                    chainSectorTableSize[s][i][TRUE] = 0 ;
                    chainSectorTableSize[s][i][FALSE] = 0 ;
                    if( s < grid.cellsInRow ){
                        chainCellTableSize[s][i][TRUE] = 0 ;
                        chainCellTableSize[s][i][FALSE] = 0 ;
                    }
                    ++ i ;
                }                
                ++ s ;
            }
        }
    }
    
    /**
     * Finds unit chains.
     */    

    boolean addUnitChains( StringBuilder sb ,
                           int v , 
                           boolean weakLinks ,
                           boolean extendedLinks ,
                           boolean nishioLinks ,
                           boolean useTable ){
        final CellState cellState = (CellState) lcc.state ;
        final NumberState numberState = (NumberState) lcn.state ;
        int i ,  s , t0 , t1 , x0 , x1 , y0 , y1 , b , b0 , b1 , r ,c , nCandidates ;
        int firstRegularLink , lastRegularLink , otherEnd0True , otherEnd1True , r0 , c0 , r1 , c1 ;
        boolean extended , extendedLinkStillSought ;
        firstRegularLink = nChains ;
        s = 0 ;
        while( s < 3 * grid.cellsInRow && nChains < maxChains ){
            extendedLinkStillSought = extendedLinks ;
            if( numberState.nEliminated[v][s] == grid.cellsInRow - 1 ){
                ++ s ;
                continue ;
            }
            t0 = 0 ;
            while( true ){
                while( t0 < grid.cellsInRow - 1 && numberState.eliminated[v][s][t0] ){
                    ++ t0 ;
                }
                if( t0 == grid.cellsInRow - 1 ){
                    break ;
                }
                if( s < grid.cellsInRow ){
                    x0 = s ;
                    y0 = t0 ;
                    b0 = x0 / grid.boxesAcross * grid.boxesAcross + y0 / grid.boxesDown ;
                } else if( s < 2 * grid.cellsInRow ){
                    x0 = t0 ;
                    y0 = s - grid.cellsInRow ;
                    b0 = x0 / grid.boxesAcross * grid.boxesAcross + y0 / grid.boxesDown ;
                } else {                    
                    x0 = ( s - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + t0 / grid.boxesDown ;
                    y0 = ( s - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + t0 % grid.boxesDown ;
                    b0 = -1 ;
                }
                t1 = t0 + 1 ;
                while( true ){
                    while( t1 < grid.cellsInRow && numberState.eliminated[v][s][t1] ){
                        ++ t1 ;
                    }
                    if( t1 == grid.cellsInRow ){
                        break ;
                    }
                    if( s < grid.cellsInRow ){
                        x1 = s ;
                        y1 = t1 ;
                        b1 = x1 / grid.boxesAcross * grid.boxesAcross + y1 / grid.boxesDown ;
                    } else if( s < 2 * grid.cellsInRow ){
                        x1 = t1 ;
                        y1 = s - grid.cellsInRow ;
                        b1 = x1 / grid.boxesAcross * grid.boxesAcross + y1 / grid.boxesDown ;
                    } else {                    
                        x1 = ( s - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + t1 / grid.boxesDown ;
                        y1 = ( s - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + t1 % grid.boxesDown ;
                        b1 = -1 ;
                        if( x0 == x1 || y0 == y1 ){
                            ++ t1 ;
                            continue ;
                        }
                    }
                    chainR0[nChains] = (byte) x0 ;
                    chainC0[nChains] = (byte) y0 ;
                    chainR1[nChains] = (byte) x1 ;
                    chainC1[nChains] = (byte) y1 ;
                    chainV0[nChains] = chainV1[nChains] = (byte) v ;
                    chainLength[nChains] = 1 ;
                    if( numberState.nEliminated[v][s] == grid.cellsInRow - 2 ){
                        linkCategory[nChains] = STRONG ;
                        chainOtherEnd0[nChains][FALSE] = chainOtherEnd1[nChains][FALSE] = TRUE ;
                        chainOtherEnd0[nChains][TRUE]  = chainOtherEnd1[nChains][TRUE]  = FALSE ;
                        addChainToTables( nChains );
                    } else if( weakLinks ){
                        linkCategory[nChains] = WEAK ;
                        chainOtherEnd0[nChains][FALSE] = chainOtherEnd1[nChains][FALSE] = DONT_KNOW ;
                        chainOtherEnd0[nChains][TRUE]  = chainOtherEnd1[nChains][TRUE]  = FALSE ;
                        addChainToTables( nChains );
                    } else {
                        ++ t1 ;
                        continue ;                        
                    }
                    if( explain && reportChains && maxChainLength >= 1 ){
                        chainRoute[nChains][0] = nChains ;
                        isLinkAscending[nChains][0] = true ;
//                        chainNComponents[nChains] = 0 ;
                    }
                    if( ++ nChains == maxChains ){
                        System.err.println("Chain Builder is full with " + maxChains + " elements");
                        return false ;
                    }
                    ++ t1 ;
                }
                if( extendedLinkStillSought && s < 2 * grid.cellsInRow && numberState.nEliminated[v][s] <= grid.cellsInRow - 3 ){
                    extended = true ;
                    b = -1 ;
                    t1 = 0 ;
                    while( extended ){
                        while( t1 < grid.cellsInRow && ( t0 == t1 || numberState.eliminated[v][s][t1] ) ){
                            ++ t1 ;
                        }
                        if( t1 == grid.cellsInRow ){
                            break ;
                        }
                        if( s < grid.cellsInRow ){
                            x1 = s ;
                            y1 = t1 ;
                        } else {
                            x1 = t1 ;
                            y1 = s - grid.cellsInRow ;
                        }
                        b1 = x1 / grid.boxesAcross * grid.boxesAcross + y1 / grid.boxesDown ;
                        if( b0 == b1 ){
                            extended = false ;
                        } else if( b == -1 ) {
                            b = b1 ;
                        } else {
                            extended = b == b1 ;
                        }
                        ++ t1 ;
                    }
                    if( extended ){
                        // There's an EXTENDED link between (x0,y0) and a candidate in
                        // Box b1 that doesn't lie in sector s - provided it's the only candidate.
                        assert b != -1 ;
                        x1 = y1 = -1 ;
                        nCandidates = 0 ;
                        t1 = 0 ;
                        while( t1 < grid.cellsInRow ){
                            if( ! numberState.eliminated[v][b+2*grid.cellsInRow][t1] ){
                                r = b / grid.boxesAcross * grid.boxesAcross + t1 / grid.boxesDown ;
                                c = b % grid.boxesAcross * grid.boxesDown + t1 % grid.boxesDown ;
                                if( s < grid.cellsInRow ){
                                    if( x0 == r ){
                                        ++ t1 ;
                                        continue ;
                                    }
                                } else if( y0 == c ){
                                    ++ t1 ;
                                    continue ;                                    
                                }
                                // Good candidate.
                                if( ++ nCandidates == 2 ){
                                    break ;
                                }
                                x1 = r ;
                                y1 = c ;
                            }
                            ++ t1 ;
                        }
                        // Check whether it's a good candidate.
                        if( nCandidates == 1 ){
                            // Check whether the chain direction should be reversed.
                            if( x0 > x1 || x0 == x1 && y0 > y1 ){
                                int tmp ;
                                tmp = x0 ; x0 = x1 ; x1 = tmp ;
                                tmp = y0 ; y0 = y1 ; y1 = tmp ;
                            }
                            // Record the link. 
                            chainR0[nChains] = (byte) x0 ;
                            chainC0[nChains] = (byte) y0 ;
                            chainR1[nChains] = (byte) x1 ;
                            chainC1[nChains] = (byte) y1 ;
                            chainV0[nChains] = chainV1[nChains] = (byte) v ;
                            chainLength[nChains] = 1 ;
                            linkCategory[nChains] = EXTENDED ;
                            chainOtherEnd0[nChains][FALSE] = chainOtherEnd1[nChains][FALSE] = FALSE ;
                            chainOtherEnd0[nChains][TRUE]  = chainOtherEnd1[nChains][TRUE]  = TRUE ;
                            addChainToTables( nChains );
                            if( explain && reportChains && maxChainLength >= 1 ){
                                chainRoute[nChains][0] = nChains ;
                                isLinkAscending[nChains][0] = true ;
        /*                        
                                if( addChains ){
                                    chainNComponents[nChains] = 1 ;
                                    chainComponents[nChains][0] = nChains ;
                                }
        */                        
                            }
                            if( ++ nChains > maxChains ){
                                System.err.println("Chain Builder is full with " + maxChains + " elements");
                                return false ;
                            }
                            extendedLinkStillSought = false ;
                        }
                    }
                }
                ++ t0 ;
            }
            ++ s ;            
        }
        lastRegularLink = nChains ;
        // Append NISHIO links.
        if( nishioLinks ){
            x0 = 0 ;
            while( x0 < grid.cellsInRow ){
                y0 = 0 ;
                while( y0 < grid.cellsInRow ){
                    if( cellState.eliminated[x0][y0][v] || cellState.nEliminated[x0][y0] == grid.cellsInRow - 1 ){
                        ++ y0 ;
                        continue ;
                    }
                    // Reduce the system.
                    nishioInitiate( v );
                    if( ! nishioReduce( x0 , y0 ) ){
                        eliminateMove( x0 , y0 , v );
                        ++ chainsEliminations ;
                        if( explain ){
                            sb.append("The move ");
                            SuDokuUtils.appendMove( sb , x0 , y0 , v );
                            sb.append(" would make it impossible to place the remaining ");
                            SuDokuUtils.appendValue( sb , v );
                            sb.append("s.\n");
                        }
                        return true ;
                    }
                    // Look for newly-inferred information.
                    x1 = 0 ;
                    while( x1 < grid.cellsInRow ){
                        y1 = 0 ;
                        nextCell:
                        while( y1 < grid.cellsInRow ){
                            if( x0 == x1 && y0 == y1 ){
                                ++ y1 ;
                                continue ;
                            }
                            otherEnd0True = otherEnd1True = DONT_KNOW ;
                            if( mask[x1][y1] == Nishio.DEFINITE && cellState.nEliminated[x1][y1] < grid.cellsInRow - 1 ){
                                otherEnd0True = TRUE ;
                            } else if( mask[x1][y1] == Nishio.NULL && ! cellState.eliminated[x1][y1][v] ){
                                otherEnd0True = FALSE ; 
                            } else {
                                ++ y1 ;
                                continue ;
                            }
                            // Check whether the chain direction should be reversed.
                            r0 = x0 ;
                            c0 = y0 ;
                            r1 = x1 ;
                            c1 = y1 ;                            
                            if( r0 > r1 || r0 == r1 && c0 > c1 ){
                                int tmp ;
                                tmp = r0 ; r0 = r1 ; r1 = tmp ;
                                tmp = c0 ; c0 = c1 ; c1 = tmp ;
                                tmp = otherEnd0True ; otherEnd0True = otherEnd1True ; otherEnd1True = tmp ;
                            }
                            // Check whether the link is a previously-discovered regular link.
                            i = firstRegularLink ;
                            while( i < lastRegularLink ){
                                if( r0 == chainR0[i] && c0 == chainC0[i] && r1 == chainR1[i] && c1 == chainC1[i] ){
                                    ++ y1 ;
                                    continue nextCell ;
                                }
                                ++ i ;
                            }
                            // Check whether the link is a previously-discovered NISHIO link.
                            while( i < nChains ){
                                if( r0 == chainR0[i] && c0 == chainC0[i] && r1 == chainR1[i] && c1 == chainC1[i] ){
                                    // The link shouldn't have the newly-inferred info.
                                    if( otherEnd0True != DONT_KNOW ){
                                        assert chainOtherEnd0[i][TRUE] == DONT_KNOW ;
                                        chainOtherEnd0[i][TRUE] = (byte) otherEnd0True ;
                                    } else if( otherEnd1True != DONT_KNOW ){
                                        assert chainOtherEnd1[i][TRUE] == DONT_KNOW ;
                                        chainOtherEnd1[i][TRUE] = (byte) otherEnd1True ;
                                    }                                    
                                    ++ y1 ;
                                    continue nextCell ;
                                }
                                ++ i ;                                
                            }
                            // It's a brand-new link.
                            chainR0[nChains] = (byte) r0 ;
                            chainC0[nChains] = (byte) c0 ;
                            chainR1[nChains] = (byte) r1 ;
                            chainC1[nChains] = (byte) c1 ;
                            chainV0[nChains] = chainV1[nChains] = (byte) v ;
                            chainLength[nChains] = 1 ;
                            linkCategory[nChains] = NISHIO ;
                            chainOtherEnd0[nChains][FALSE] = chainOtherEnd1[nChains][FALSE] = DONT_KNOW ;
                            chainOtherEnd0[nChains][TRUE]  = (byte) otherEnd0True ;
                            chainOtherEnd1[nChains][TRUE]  = (byte) otherEnd1True ;
                            if( useTable ){
                                addChainToTables( nChains );
                            }
                            if( explain && reportChains && maxChainLength >= 1 ){
                                chainRoute[nChains][0] = nChains ;
                                isLinkAscending[nChains][0] = true ;
                            }
                            if( ++ nChains == maxChains ){
                                System.err.println("Chain Builder is full with " + maxChains + " elements");
                                return false ;
                            }                                                                                                                                                               
                            ++ y1 ;
                        }
                        ++ x1 ;
                    }
                    ++ y0 ;
                }
                ++ x0 ;
            }            
        }
        return false ;
    }
    
    /**
     * Establishes whether there is a strong link at the point (r,c)
     * for the values v0 and v1. 
     */

    boolean strongLink( int r , int c , int v0 , int v1 ){
        if( v0 == v1 ){
            return true ;   
        }
        CellState cellState = (CellState) lcc.state ;
        return cellState.nEliminated[r][c] == grid.cellsInRow - 2 && 
             ! cellState.eliminated[r][c][v0] && 
             ! cellState.eliminated[r][c][v1] ;               
    }
    
    /**
     * Establishes whether there is a weak link between the points (r0,c0)
     * and (r1,c1) for the values v0 and v1. 
     */

    boolean weakLink( int r0 , int c0 , int v0 , int r1 , int c1 , int v1 ){
        if( v0 == v1 ){
            NumberState numberState = (NumberState) lcn.state ;
            if( r0 == r1 && c0 == c1 ){
                return false ; // It's an exact match.
            } else if( r0 == r1 ){
                return numberState.nEliminated[v0][r0] < grid.cellsInRow - 2 ;
            } else if( c0 == c1 ) {
                return numberState.nEliminated[v0][grid.cellsInRow+c0] < grid.cellsInRow - 2 ;
            } else {
                final int b0 = r0 / grid.boxesAcross * grid.boxesAcross + c0 / grid.boxesDown ,
                          b1 = r1 / grid.boxesAcross * grid.boxesAcross + c1 / grid.boxesDown ;
                return b0 == b1 && numberState.nEliminated[v0][2*grid.cellsInRow+b0] < grid.cellsInRow - 2 ;                    
            }
        } else if( r0 == r1 && c0 == c1 ){
            return cellLink( r0 , c0 , v0 , v1 );
        } else {
            return false ;
        }
    }
    
    /**
     * Establishes whether there is a weak cell link at the point (r,c).
     */

    boolean cellLink( int r , int c , int v0 , int v1 ){
        if( v0 != v1 ){
            CellState cellState = (CellState) lcc.state ;
            return cellState.nEliminated[r][c] < grid.cellsInRow - 2 && 
                 ! cellState.eliminated[r][c][v0] && 
                 ! cellState.eliminated[r][c][v1] ;                                           
        }
        return false ;
    }

    /**
     * Establishes the type of link (if any) that exists between two given strings.
     */

    int linkType( int i , int j , boolean useWeakLinks ){
        if( chainR0[i] == chainR0[j] && chainC0[i] == chainC0[j] &&
            chainR1[i] == chainR1[j] && chainC1[i] == chainC1[j] ){
                return 4 * MATCH ; 
        } else if( chainR0[i] == chainR0[j] && chainC0[i] == chainC0[j] && 
                !( chainR1[i] == chainR1[j] && chainC1[i] == chainC1[j] ) ){
            if( strongLink( chainR0[i] , chainC0[i] , chainV0[i] , chainV0[j] ) ){
                return 4 * STRONG + LEFT_LEFT ;
            } else if( useWeakLinks && cellLink( chainR0[i] , chainC0[i] , chainV0[i] , chainV0[j] ) ){
                return 4 * CELL + LEFT_LEFT ;
            } else {
                return NONE ;   
            }
        } else if( chainR1[i] == chainR1[j] && chainC1[i] == chainC1[j] && 
                !( chainR0[i] == chainR0[j] && chainC0[i] == chainC0[j] ) ){
            if( strongLink( chainR1[i] , chainC1[i] , chainV1[i] , chainV1[j] ) ){
                return 4 * STRONG + RIGHT_RIGHT ;   
            } else if( useWeakLinks && cellLink( chainR1[i] , chainC1[i] , chainV1[i] , chainV1[j] ) ){
                return 4 * CELL + RIGHT_RIGHT ;
            } else {
                return NONE ;   
            }
        } else if( chainR0[i] == chainR1[j] && chainC0[i] == chainC1[j] && 
                !( chainR1[i] == chainR0[j] && chainC1[i] == chainC0[j] ) ){
            if( strongLink( chainR0[i] , chainC0[i] , chainV0[i] , chainV1[j] ) ){
                return 4 * STRONG + LEFT_RIGHT ;   
            } else if( useWeakLinks && cellLink( chainR0[i] , chainC0[i] , chainV0[i] , chainV1[j] ) ){
                return 4 * CELL + LEFT_RIGHT ;
            } else {
                return NONE ;   
            }
        } else if( chainR1[i] == chainR0[j] && chainC1[i] == chainC0[j] && 
                !( chainR0[i] == chainR1[j] && chainC0[i] == chainC1[j] ) ){
            if( strongLink( chainR1[i] , chainC1[i] , chainV1[i] , chainV0[j] ) ){
                return 4 * STRONG + RIGHT_LEFT ;   
            } else if( useWeakLinks && cellLink( chainR1[i] , chainC1[i] , chainV1[i] , chainV0[j] ) ){
                return 4 * CELL + RIGHT_LEFT ;
            } else {
                return NONE ;   
            }    
        } else {
            return NONE ;
        }   
    }

    /**
     * Constructs weakly-linked strings.
     */
    
    boolean addLongChains( StringBuilder sb , boolean useWeakLinks , boolean useTable ){
        int i , j , type ;
        short k , chainsBegin , chainsEnd , nChainsStart ;
        boolean isLinkStrong ;
        chainsBegin = 0 ;
        chainsEnd   = nChains ;
        while( chainsBegin < chainsEnd ){
            // Test for linkage to others.            
            i = 0 ;
            while( i < chainsEnd ){
                j = Math.max( i + 1 , chainsBegin );
                while( j < chainsEnd && nChains < maxChains ){
                    nChainsStart = nChains ;
                    type = linkType( i , j , useWeakLinks );
                    if( type / 4 == STRONG || type / 4 == CELL ){
                        if( ! connect( i , j , type ) ){
                            ++ j ;
                            continue ;
                        }
                    } else if( type / 4 == MATCH && nChains < maxChains - 1 ){
                        isLinkStrong = strongLink( chainR0[i] , chainC0[i] , chainV0[i] , chainV0[j] );
                        connect( i , j , 4 *( isLinkStrong ? STRONG : WEAK ) + LEFT_LEFT );
                        isLinkStrong = strongLink( chainR1[i] , chainC1[i] , chainV1[i] , chainV1[j] );
                        connect( i , j , 4 *( isLinkStrong ? STRONG : WEAK ) + RIGHT_RIGHT );
                    }           
                    // Examine each newly-created chain.
                    k = nChainsStart ;
                    while( k < nChains ){ 
                        // The end-points share ...
                        // ... a cell and values.
                        if( chainR0[k] == chainR1[k] && chainC0[k] == chainC1[k] && chainV0[k] == chainV1[k] && perfectCyclicChain( k , sb ) ){
                            return true ;
                        // ... a cell but not values or values and a sector
                        } else if( weakLink( chainR0[k] , chainC0[k] , chainV0[k] , chainR1[k] , chainC1[k] , chainV1[k] ) && cyclicChain( k , sb ) ){
                            return true ;
                        // ... a sector but not values
                        } else if( forcedDisjointSubsets( sb , k ) ){
                            return true ;
                        // ... none of the above
                        } else if( useTable && ( tableEliminations( k , true , sb ) || tableEliminations( k , false , sb ) ) ){
                            return true ;
                        }
                        ++ k ;
                    }
                    // Next link
                    ++ j ;
                }
                ++ i ;
            }
            // Consider any newly-created links.
            chainsBegin = chainsEnd ;
            chainsEnd = nChains ;
        }
        return false ;
    }
    
    /**
     * Tests for a cyclic chain at positions.
     */

    boolean cyclicChain( int s , StringBuilder sb ){   
        boolean anyValueEliminated = false ;
        if( chainOtherEnd0[s][TRUE]  == TRUE ||
            chainOtherEnd0[s][FALSE] == TRUE ||
            chainOtherEnd1[s][TRUE]  == TRUE ||
            chainOtherEnd1[s][FALSE] == TRUE ){
            if( explain && reportChains ){
                sb.append("Consider the chain ");
                appendChain( sb , s );
                sb.append(".\n");
            }
            if( chainOtherEnd0[s][TRUE] == TRUE ){
                anyValueEliminated |= cyclicElimination1( sb , chainR0[s] , chainC0[s] , chainV0[s] , chainR1[s] , chainC1[s] , chainV1[s] );
            }
            if( chainOtherEnd1[s][TRUE] == TRUE ){
                anyValueEliminated |= cyclicElimination1( sb , chainR1[s] , chainC1[s] , chainV1[s] , chainR0[s] , chainC0[s] , chainV0[s] );
            }                
            if( chainOtherEnd0[s][FALSE] == TRUE ){
                anyValueEliminated |= cyclicElimination2( sb , chainR0[s] , chainC0[s] , chainV0[s] , chainR1[s] , chainC1[s] , chainV1[s] );
            } else if( chainOtherEnd1[s][FALSE] == TRUE ){
                anyValueEliminated |= cyclicElimination2( sb , chainR1[s] , chainC1[s] , chainV1[s] , chainR0[s] , chainC0[s] , chainV0[s] );
            }                
        }
        return anyValueEliminated ;
    }
    
    boolean perfectCyclicChain( int s , StringBuilder sb ){  
        boolean contains ;
        if( chainOtherEnd0[s][FALSE] == TRUE ||
            chainOtherEnd1[s][FALSE] == TRUE ){
            contains = false ;
        } else if( chainOtherEnd0[s][TRUE]== FALSE ||
                   chainOtherEnd1[s][TRUE]== FALSE ){
            contains = true ;
        } else {
            return false ;
        }        
        if( explain ){
            if( reportChains ){
                sb.append("Consider the chain ");
                appendChain( sb , s );
                sb.append(".\n");
            }
            sb.append("When the cell ");
            SuDokuUtils.appendCell( sb , chainR0[s] , chainC0[s] );
            if( contains ){
                sb.append(" contains ");
            } else {
                sb.append(" doesn't contain ");
            }
            sb.append("the value ");
            SuDokuUtils.appendValue( sb , chainV0[s] );
            sb.append(", the chain is self-contradicting.\nTherefore, the cell ");
            SuDokuUtils.appendCell( sb , chainR0[s] , chainC0[s] );
            if( contains ){
                sb.append(" cannot");
            } else {
                sb.append(" must");
            }
            sb.append(" contain the value ");
            SuDokuUtils.appendValue( sb , chainV0[s] );
            sb.append(".\n");                                
        }
        if( contains ){
            eliminateMove( chainR0[s] , chainC0[s] , chainV0[s] );
            ++ chainsEliminations ;
        } else {
            chainsEliminations += addMove( chainR0[s] , chainC0[s] , chainV0[s] );
        }
        return true ;
    }
        
    boolean cyclicElimination1( StringBuilder sb , int r0 , int c0 , int v0 , int r1 , int c1 , int v1 ){
        if( explain ){
            sb.append("When the cell ");
            SuDokuUtils.appendCell( sb , r0 , c0 );
            sb.append(" contains the value ");
            SuDokuUtils.appendValue( sb , v0 );
            sb.append(", ");
            if( v0 == v1 ){
                sb.append("so does the cell ");
                SuDokuUtils.appendCell( sb , r1 , c1 );
            } else {
                sb.append("it likewise contains the value ");
                SuDokuUtils.appendValue( sb , v1 );
            }
            sb.append(" - a contradiction.\nTherefore, the cell ");
            SuDokuUtils.appendCell( sb , r0 , c0 );
            sb.append(" cannot contain the value ");
            SuDokuUtils.appendValue( sb , v0 );
            sb.append(".\n");
        }
        eliminateMove( r0 , c0 , v0 );
        ++ chainsEliminations ;
        return true ;    
    }
    
    boolean cyclicElimination2( StringBuilder sb , int r0 , int c0 , int v0 , int r1 , int c1 , int v1 ){
        int i ;
        if( explain ){
            sb.append("The cell ");
            SuDokuUtils.appendCell( sb , r1 , c1 );
            sb.append(" must contain the value ");
            SuDokuUtils.appendValue( sb , v1 );
            sb.append(" if ");
            if( v0 == v1 ){
                sb.append("the cell ");
                SuDokuUtils.appendCell( sb , r0 , c0 );
                sb.append(" doesn't.\n");
            } else {
                sb.append("it doesn't contain the value ");
                SuDokuUtils.appendValue( sb , v0 );
                sb.append(".\n");
            }
            sb.append("Therefore, these two ");
            if( v0 == v1 ){
                sb.append("cells");
            } else {
                sb.append("values");
            }
            sb.append(" are the only candidates for ");
            if( v0 == v1 ){
                sb.append("the value ");
                SuDokuUtils.appendValue( sb , v1 );
                sb.append(" in ");                        
                if( r0 == r1 ){
                    SuDokuUtils.appendRow( sb , r0 );
                } else if( c0 == c1 ) {
                    SuDokuUtils.appendColumn( sb , c0 );
                } else {
                    SuDokuUtils.appendBox( sb , grid.boxesAcross , grid.boxesDown , r0 , c0 );
                }
            } else {
                sb.append("the cell ");
                SuDokuUtils.appendCell( sb , r0 , c0 );
            }
            sb.append(".\n");
        }                
        // Eliminate.
        if( v0 == v1 ){
            NumberState numberState = (NumberState) lcn.state ;
            if( r0 == r1 ){
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( ! numberState.eliminated[v0][r0][i] && i != c0 && i != c1 ){
                        eliminateMove( r0 , i , v0 );
                        ++ chainsEliminations ;
                    }
                    ++ i ;
                }
            } else if( c0 == c1 ) {
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( ! numberState.eliminated[v0][grid.cellsInRow+c0][i] && i != r0 && i != r1 ){
                        eliminateMove( i , c0 , v0 );
                        ++ chainsEliminations ;
                    }
                    ++ i ;
                }
            } else {
                int x , y ;
                final int b = r0 / grid.boxesAcross * grid.boxesAcross + c0 / grid.boxesDown ;
                i = 0 ;
                while( i < grid.cellsInRow ){
                    x = b / grid.boxesAcross * grid.boxesAcross + i / grid.boxesDown ;
                    y = b % grid.boxesAcross * grid.boxesDown + i % grid.boxesDown ;                                    
                    if( ! numberState.eliminated[v0][2*grid.cellsInRow+b][i] && !( x == r0 && y == c0 || x == r1 && y == c1 ) ){
                        eliminateMove( x , y , v0 );
                        ++ chainsEliminations ;
                    }
                    ++ i ;
                }
            }
        } else {
            CellState cellState = (CellState) lcc.state ;
            i = 0 ;
            while( i < grid.cellsInRow ){
                if( ! cellState.eliminated[r0][c0][i] && i != v0 && i != v1 ){
                    eliminateMove( r0 , c0 , i );
                    ++ chainsEliminations ;
                }
                ++ i ;
            }
        }
        return true ;
    }
    
    boolean forcedDisjointSubsets( StringBuilder sb , int s ){
        boolean anyMoveEliminated = false ,
                exactlyOne = false ;        
        // The criteria for the chain are that:
        // i. The end-points should lie in a shared sector.
        // ii. When v1 (given) doesn't appear at one-end, some value <> v1 should appear at the other.
        if( chainV0[s] == chainV1[s] ){
            anyMoveEliminated = false ;
        } else if( chainOtherEnd0[s][FALSE] == TRUE ){
            exactlyOne = chainOtherEnd0[s][TRUE] == FALSE ;
            anyMoveEliminated |= forcedDisjointSubsetsCoordinates( sb , s , chainR0[s] , chainC0[s] , chainV0[s] , chainR1[s] , chainC1[s] , chainV1[s] , exactlyOne );
        } else if( chainOtherEnd1[s][FALSE] == TRUE ){
            exactlyOne = chainOtherEnd1[s][TRUE] == FALSE ;
            anyMoveEliminated |= forcedDisjointSubsetsCoordinates( sb , s , chainR1[s] , chainC1[s] , chainV1[s] , chainR0[s] , chainC0[s] , chainV0[s] , exactlyOne );
        }        
        return anyMoveEliminated ;
    }
    
    boolean forcedDisjointSubsetsCoordinates( StringBuilder sb , int s , int r0 , int c0 , int v0 , int r1 , int c1 , int v1 , boolean exactlyOne ){
        final NumberState numberState = (NumberState) lcn.state ;
        final CellState cellState = (CellState) lcc.state ;
        int i , j , r , c , nCells , nValues ;
        boolean anyMoveEliminated = false , endValuesMatch = false , otherValuesMatch = false ;
        // Know that when (r0,c0)<>v0, (r1,c1):=v1.
        // Confirm that the end-points share a sector.
        int sharedSector = -1 ;
        if( r0 == r1 && c0 == c1 ){
            return false ;
        } else if( r0 == r1 ) {
            sharedSector = r0 ;            
        } else if( c0 == c1 ) {
            sharedSector = grid.cellsInRow + c0 ;
        } else if( r0 / grid.boxesAcross == r1 / grid.boxesAcross && c0 / grid.boxesDown == c1 / grid.boxesDown ){
            sharedSector = 2 * grid.cellsInRow + r0 / grid.boxesAcross * grid.boxesAcross + c0 / grid.boxesDown ;
        } else {
            return false ;
        }
        if( sharedSector >= 0 ){
            if( ! cellState.eliminated[r0][c0][v1] ){
                if( explain ){
                    if( reportChains ){
                        sb.append("Consider the chain ");
                        appendChain( sb , s );
                        sb.append(".\n");
                    }
                    sb.append("When the cell ");
                    SuDokuUtils.appendCell( sb , r0 , c0 );
                    sb.append(" contains the value ");
                    SuDokuUtils.appendValue( sb , v1 );
                    sb.append(", so does the cell ");
                    SuDokuUtils.appendCell( sb , r1 , c1 );
                    sb.append(" - a contradiction.\nTherefore, the cell ");
                    SuDokuUtils.appendCell( sb , r0 , c0 );
                    sb.append(" cannot contain the value ");
                    SuDokuUtils.appendValue( sb , v1 );
                    sb.append(".\n");
                }
                eliminateMove( r0 , c0 , v1 );
                ++ chainsEliminations ;
                anyMoveEliminated = true ;
            }
            if( ! cellState.eliminated[r1][c1][v0] ){
                if( explain ){
                    if( reportChains && ! anyMoveEliminated ){
                        sb.append("Consider the chain ");
                        appendChain( sb , s );
                        sb.append(".\n");
                    }
                    sb.append("When the cell ");
                    SuDokuUtils.appendCell( sb , r1 , c1 );
                    sb.append(" contains the value ");
                    SuDokuUtils.appendValue( sb , v0 );
                    sb.append(", some other value must occupy the cell ");
                    SuDokuUtils.appendCell( sb , r0 , c0 );
                    sb.append(", which means that the value ");
                    SuDokuUtils.appendValue( sb , v1 );
                    sb.append(" must occupy the cell ");
                    SuDokuUtils.appendCell( sb , r1 , c1 );
                    sb.append(" - a contradiction.\nTherefore, the cell ");
                    SuDokuUtils.appendCell( sb , r1 , c1 );
                    sb.append(" cannot contain the value ");
                    SuDokuUtils.appendValue( sb , v0 );
                    sb.append(".\n");
                }
                eliminateMove( r1 , c1 , v0 );
                ++ chainsEliminations ;
                anyMoveEliminated = true ;
            }
            if( anyMoveEliminated || ! exactlyOne ){
                return anyMoveEliminated ;
            }            
            // Find the set of alternative candidates for (r0,c0) & (r1,c1).
            nValues = 0 ;
            i = 0 ;
            while( i < grid.cellsInRow ){                
                if( ( isValueUsed[i] = i != v0 && i != v1 && ( ! cellState.eliminated[r0][c0][i] || ! cellState.eliminated[r1][c1][i] ) ) ){
                    ++ nValues ;
                }
                isCellUsed[i] = false ;
                ++ i ;
            };
            // Look for a single cell with {v0,v1} as candidates ...
            r = c = -1 ;
            if( numberState.nEliminated[v0][sharedSector] < grid.cellsInRow - 2 ||
                numberState.nEliminated[v1][sharedSector] < grid.cellsInRow - 2 ){
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( sharedSector < grid.cellsInRow ){
                        r = sharedSector ;
                        c = i ;
                    } else if( sharedSector < 2 * grid.cellsInRow ){
                        r = i ;
                        c = sharedSector - grid.cellsInRow ;
                    } else {                    
                        r = ( sharedSector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + i / grid.boxesDown ;
                        c = ( sharedSector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + i % grid.boxesDown ;
                    }
                    if( cellState.nEliminated[r][c] == grid.cellsInRow - 2 &&
                        ! cellState.eliminated[r][c][v0] &&
                        ! cellState.eliminated[r][c][v1] ){
                        x[0] = r ;
                        y[0] = c ;
                        break ;
                    }
                    ++ i ;
                }
                if( i < grid.cellsInRow ){
                    j = 0 ;
                    while( j < grid.cellsInRow ){
                        if( j == i ){
                            ++ j ;
                            continue ;
                        }
                        if( sharedSector < grid.cellsInRow ){
                            r = sharedSector ;
                            c = j ;
                        } else if( sharedSector < 2 * grid.cellsInRow ){
                            r = j ;
                            c = sharedSector - grid.cellsInRow ;
                        } else {                    
                            r = ( sharedSector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + j / grid.boxesDown ;
                            c = ( sharedSector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + j % grid.boxesDown ;
                        }
                        if( r == r0 && c == c0 || r == r1 && c == c1 ){
                            ++ j ;
                            continue ;
                        }
                        if( ! cellState.eliminated[r][c][v0] ){
                            eliminateMove( r , c , v0 );
                            ++ chainsEliminations ;
                            anyMoveEliminated |= ( endValuesMatch = true );
                        }
                        if( ! cellState.eliminated[r][c][v1] ){
                            eliminateMove( r , c , v1 );
                            ++ chainsEliminations ;
                            anyMoveEliminated |= ( endValuesMatch = true );
                        }
                        ++ j ;
                    }
                }
                if( anyMoveEliminated && explain ){
                    appendForcedDisjointSubsetsPreamble( sb , s , r0 , c0 , v0 , r1 , c1 , v1 , nValues );
                    sb.append("The values ");
                    SuDokuUtils.appendValue( sb , v0 );
                    sb.append(" and ");
                    SuDokuUtils.appendValue( sb , v1 );
                    sb.append(" occupy two of the cells ");
                    SuDokuUtils.appendCell( sb , r0 , c0 );
                    sb.append(", ");
                    SuDokuUtils.appendCell( sb , r1 , c1 );
                    sb.append(" and ");
                    SuDokuUtils.appendCell( sb , x[0] , y[0] );
                    sb.append(" in some order.\n");
                }                        
            }
            // ... or (nValues-1) cells with candidates from isValueUsed[].           
            x[0] = r0 ;
            y[0] = c0 ;
            x[1] = r1 ;
            y[1] = c1 ;
            nCells = 0 ;
            i = 0 ;
            while( i < grid.cellsInRow ){
                if( sharedSector < grid.cellsInRow ){
                    r = sharedSector ;
                    c = i ;
                } else if( sharedSector < 2 * grid.cellsInRow ){
                    r = i ;
                    c = sharedSector - grid.cellsInRow ;
                } else {                    
                    r = ( sharedSector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + i / grid.boxesDown ;
                    c = ( sharedSector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + i % grid.boxesDown ;
                }
                if( r == r0 && c == c0 || r == r1 && c == c1 ){
                    ++ i ;
                    continue ;
                }                
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( ! isValueUsed[j] && ! cellState.eliminated[r][c][j] ){        
                        break ;
                    }
                    ++ j ;
                }
                if( j == grid.cellsInRow ){
                    x[nCells+2] = r ;
                    y[nCells+2] = c ;
                    isCellUsed[i] = true ;
                    ++ nCells ;
                }
                ++ i ;
            }        
            if( nCells == nValues - 1 ){
                i = 0 ;
                while( i < grid.cellsInRow ){
                    if( isCellUsed[i] ){
                        ++ i ;
                        continue ;
                    }
                    if( sharedSector < grid.cellsInRow ){
                        r = sharedSector ;
                        c = i ;
                    } else if( sharedSector < 2 * grid.cellsInRow ){
                        r = i ;
                        c = sharedSector - grid.cellsInRow ;
                    } else {                    
                        r = ( sharedSector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + i / grid.boxesDown ;
                        c = ( sharedSector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + i % grid.boxesDown ;
                    }
                    if( r == r0 && c == c0 || r == r1 && c == c1 ){
                        ++ i ;
                        continue ;
                    } 
                    j = 0 ;
                    while( j < grid.cellsInRow ){
                        if( isValueUsed[j] && ! cellState.eliminated[r][c][j] ){
                            eliminateMove( r , c , j );
                            ++ chainsEliminations ;
                            anyMoveEliminated |= ( otherValuesMatch = true );
                        }
                        ++ j ;
                    }
                    ++ i ;
                }
                if( otherValuesMatch && explain ){
                    if( ! endValuesMatch ){
                        appendForcedDisjointSubsetsPreamble( sb , s , r0 , c0 , v0 , r1 , c1 , v1 , nValues );
                    }
                    //
                    sb.append("The value");
                    if( nValues > 1 ){
                        sb.append("s ");
                    } else {
                        sb.append(' ');
                    }
                    i = 0 ;
                    j = 0 ;
                    while( j < nValues ){
                        while( ! isValueUsed[i] ){
                            ++ i ;
                        }
                        SuDokuUtils.appendValue( sb , i ++ );
                        if( j < nValues - 2 ){
                            sb.append(", ");
                        } else if( j == nValues - 2 ) {
                            sb.append(" and ");
                        }
                        ++ j ;
                    }
                    if( nValues > 1 ){
                        sb.append(" occupy ");
                    } else {
                        sb.append(" occupies ");
                    }
                    sb.append( nValues );                    
                    sb.append(" of the cells ");                        
                    j = 0 ;
                    while( j < nValues + 1 ){
                        SuDokuUtils.appendCell( sb , x[j] , y[j] );
                        if( j < nValues - 1 ){
                            sb.append(", ");
                        } else if( j == nValues - 1 ) {
                            sb.append(" and ");
                        }
                        ++ j ;
                    }
                    if( nValues > 1 ){
                        sb.append(" in some order");                        
                    }
                    sb.append(".\n");
                }                        
            }            
        }
        return anyMoveEliminated ;
    }
    
    StringBuilder appendForcedDisjointSubsetsPreamble( StringBuilder sb ,
                                                       int s ,
                                                       int r0 ,
                                                       int c0 ,
                                                       int v0 ,
                                                       int r1 ,
                                                       int c1 ,
                                                       int v1 ,
                                                       int nValues ){
        int i , j ;
        if( reportChains ){
            sb.append("Consider the chain ");
            appendChain( sb , s );
            sb.append(".\n");
        }
        sb.append("The cells ");
        SuDokuUtils.appendCell( sb , r0 , c0 );
        sb.append(" and ");
        SuDokuUtils.appendCell( sb , r1 , c1 );
        sb.append(" contain one value from the set {");
        SuDokuUtils.appendValue( sb , v0 );
        sb.append(',');
        SuDokuUtils.appendValue( sb , v1 );
        sb.append("} and ");
        if( nValues > 1 ){
            sb.append("another from {");
            i = 0 ;
            j = 0 ;
            while( j < nValues ){
                while( ! isValueUsed[i] ){
                    ++ i ;
                }
                SuDokuUtils.appendValue( sb , i ++ );
                if( j < nValues - 1 ){
                    sb.append(',');
                }
                ++ j ;
            }
            sb.append('}');
        } else {
            sb.append("the value ");
            i = 0 ;
            while( ! isValueUsed[i] ){
                ++ i ;
            }
            SuDokuUtils.appendValue( sb , i );
        }
        sb.append(".\n");                        
        
        return sb ;
    }
    
    /**
     * Connects string pairs.
     */

    boolean connect( int i , int j , int type ){
        int r0 = 0 , c0 = 0 , v0 = 0 , r1 = 0 , c1 = 0 , v1 = 0 ;
        int k , kOffset ;
        boolean isIReversed = false , 
                isJReversed = false , 
                isOrderReversed ;
        int otherEnd0True = 0 ,
            otherEnd0False = 0 ,
            otherEnd1True = 0 ,
            otherEnd1False = 0 ;
        switch( type % 4 ){
            case LEFT_LEFT :
            r0 = chainR1[i];
            c0 = chainC1[i];
            v0 = chainV1[i];
            r1 = chainR1[j];
            c1 = chainC1[j];
            v1 = chainV1[j];  
            if( type / 4 == STRONG ){
                if( chainV0[i] == chainV0[j] ){
                    otherEnd0True  = chainOtherEnd1[i][TRUE] != DONT_KNOW ? chainOtherEnd0[j][chainOtherEnd1[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd1[i][FALSE] != DONT_KNOW ? chainOtherEnd0[j][chainOtherEnd1[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd1[j][TRUE] != DONT_KNOW ? chainOtherEnd0[i][chainOtherEnd1[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd1[j][FALSE] != DONT_KNOW ? chainOtherEnd0[i][chainOtherEnd1[j][FALSE]] : DONT_KNOW ;
                } else {
                    otherEnd0True  = chainOtherEnd1[i][TRUE] != DONT_KNOW ? chainOtherEnd0[j][1-chainOtherEnd1[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd1[i][FALSE] != DONT_KNOW ? chainOtherEnd0[j][1-chainOtherEnd1[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd1[j][TRUE] != DONT_KNOW ? chainOtherEnd0[i][1-chainOtherEnd1[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd1[j][FALSE] != DONT_KNOW ? chainOtherEnd0[i][1-chainOtherEnd1[j][FALSE]] : DONT_KNOW ;                            
                }
            } else {
                otherEnd0True  = chainOtherEnd1[i][TRUE] == TRUE ? chainOtherEnd0[j][FALSE] : DONT_KNOW ;
                otherEnd0False = chainOtherEnd1[i][FALSE] == TRUE ? chainOtherEnd0[j][FALSE] : DONT_KNOW ;
                otherEnd1True  = chainOtherEnd1[j][TRUE] == TRUE ? chainOtherEnd0[i][FALSE] : DONT_KNOW ;
                otherEnd1False = chainOtherEnd1[j][FALSE] == TRUE ? chainOtherEnd0[i][FALSE] : DONT_KNOW ;
            }
            isIReversed = true ;
            isJReversed = false ;
            break ;
        case RIGHT_RIGHT :
            r0 = chainR0[i];
            c0 = chainC0[i];
            v0 = chainV0[i];
            r1 = chainR0[j];
            c1 = chainC0[j];
            v1 = chainV0[j];
            if( type / 4 == STRONG ){
                if( chainV1[i] == chainV1[j] ){
                    otherEnd0True  = chainOtherEnd0[i][TRUE] != DONT_KNOW ? chainOtherEnd1[j][chainOtherEnd0[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd0[i][FALSE] != DONT_KNOW ? chainOtherEnd1[j][chainOtherEnd0[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd0[j][TRUE] != DONT_KNOW ? chainOtherEnd1[i][chainOtherEnd0[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd0[j][FALSE] != DONT_KNOW ? chainOtherEnd1[i][chainOtherEnd0[j][FALSE]] : DONT_KNOW ;
                } else {
                    otherEnd0True  = chainOtherEnd0[i][TRUE] != DONT_KNOW ? chainOtherEnd1[j][1-chainOtherEnd0[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd0[i][FALSE] != DONT_KNOW ? chainOtherEnd1[j][1-chainOtherEnd0[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd0[j][TRUE] != DONT_KNOW ? chainOtherEnd1[i][1-chainOtherEnd0[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd0[j][FALSE] != DONT_KNOW ? chainOtherEnd1[i][1-chainOtherEnd0[j][FALSE]] : DONT_KNOW ;
                }
            } else {
                otherEnd0True  = chainOtherEnd0[i][TRUE] == TRUE ? chainOtherEnd1[j][FALSE] : DONT_KNOW ;
                otherEnd0False = chainOtherEnd0[i][FALSE] == TRUE ? chainOtherEnd1[j][FALSE] : DONT_KNOW ;
                otherEnd1True  = chainOtherEnd0[j][TRUE] == TRUE ? chainOtherEnd1[i][FALSE] : DONT_KNOW ;
                otherEnd1False = chainOtherEnd0[j][FALSE] == TRUE ? chainOtherEnd1[i][FALSE] : DONT_KNOW ;
            }
            isIReversed = false ;
            isJReversed = true ;
            break ;
        case LEFT_RIGHT :
            r0 = chainR1[i];
            c0 = chainC1[i];
            v0 = chainV1[i];
            r1 = chainR0[j];
            c1 = chainC0[j];
            v1 = chainV0[j];
            if( type / 4 == STRONG ){
                if( chainV0[i] == chainV1[j] ){
                    otherEnd0True  = chainOtherEnd1[i][TRUE] != DONT_KNOW ? chainOtherEnd1[j][chainOtherEnd1[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd1[i][FALSE] != DONT_KNOW ? chainOtherEnd1[j][chainOtherEnd1[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd0[j][TRUE] != DONT_KNOW ? chainOtherEnd0[i][chainOtherEnd0[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd0[j][FALSE] != DONT_KNOW ? chainOtherEnd0[i][chainOtherEnd0[j][FALSE]] : DONT_KNOW ;
                } else {
                    otherEnd0True  = chainOtherEnd1[i][TRUE] != DONT_KNOW ? chainOtherEnd1[j][1-chainOtherEnd1[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd1[i][FALSE] != DONT_KNOW ? chainOtherEnd1[j][1-chainOtherEnd1[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd0[j][TRUE] != DONT_KNOW ? chainOtherEnd0[i][1-chainOtherEnd0[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd0[j][FALSE] != DONT_KNOW ? chainOtherEnd0[i][1-chainOtherEnd0[j][FALSE]] : DONT_KNOW ;
                }
            } else {
                otherEnd0True  = chainOtherEnd1[i][TRUE] == TRUE ? chainOtherEnd1[j][FALSE] : DONT_KNOW ;
                otherEnd0False = chainOtherEnd1[i][FALSE] == TRUE ?  chainOtherEnd1[j][FALSE] : DONT_KNOW ;
                otherEnd1True  = chainOtherEnd0[j][TRUE] == TRUE ? chainOtherEnd0[i][FALSE] : DONT_KNOW ;
                otherEnd1False = chainOtherEnd0[j][FALSE] == TRUE ? chainOtherEnd0[i][FALSE] : DONT_KNOW ;
            }
            isIReversed = true ;
            isJReversed = true ;
            break ;
        case RIGHT_LEFT :
            r0 = chainR0[i];
            c0 = chainC0[i];
            v0 = chainV0[i];
            r1 = chainR1[j];
            c1 = chainC1[j]; 
            v1 = chainV1[j];
            if( type / 4 == STRONG ){
                if( chainV1[i] == chainV0[j] ){
                    otherEnd0True  = chainOtherEnd0[i][TRUE] != DONT_KNOW ? chainOtherEnd0[j][chainOtherEnd0[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd0[i][FALSE] != DONT_KNOW ? chainOtherEnd0[j][chainOtherEnd0[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd1[j][TRUE] != DONT_KNOW ? chainOtherEnd1[i][chainOtherEnd1[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd1[j][FALSE] != DONT_KNOW ? chainOtherEnd1[i][chainOtherEnd1[j][FALSE]] : DONT_KNOW ;
                } else {
                    otherEnd0True  = chainOtherEnd0[i][TRUE] != DONT_KNOW ? chainOtherEnd0[j][1-chainOtherEnd0[i][TRUE]] : DONT_KNOW ;
                    otherEnd0False = chainOtherEnd0[i][FALSE] != DONT_KNOW ? chainOtherEnd0[j][1-chainOtherEnd0[i][FALSE]] : DONT_KNOW ;
                    otherEnd1True  = chainOtherEnd1[j][TRUE] != DONT_KNOW ? chainOtherEnd1[i][1-chainOtherEnd1[j][TRUE]] : DONT_KNOW ;
                    otherEnd1False = chainOtherEnd1[j][FALSE] != DONT_KNOW ? chainOtherEnd1[i][1-chainOtherEnd1[j][FALSE]] : DONT_KNOW ;
                }
            } else {
                otherEnd0True  = chainOtherEnd0[i][TRUE] == TRUE ? chainOtherEnd0[j][FALSE] : DONT_KNOW ;
                otherEnd0False = chainOtherEnd0[i][FALSE] == TRUE ? chainOtherEnd0[j][FALSE] : DONT_KNOW ;
                otherEnd1True  = chainOtherEnd1[j][TRUE] == TRUE ? chainOtherEnd1[i][FALSE] : DONT_KNOW ;
                otherEnd1False = chainOtherEnd1[j][FALSE] == TRUE ? chainOtherEnd1[i][FALSE] : DONT_KNOW ;
            }
            isIReversed = false ;
            isJReversed = false ;
            break ;
        }
        // Check that some information is retained.
        if( otherEnd0True == DONT_KNOW &&
            otherEnd0False == DONT_KNOW &&
            otherEnd1True == DONT_KNOW &&
            otherEnd1False == DONT_KNOW ){
                return false ;
        }
        // Check whether the chain direction should be reversed.
        if( r0 > r1 || r0 == r1 && c0 > c1 ){
            isOrderReversed = true ;
            isIReversed = ! isIReversed ;
            isJReversed = ! isJReversed ;
            int tmp ;
            tmp = r0 ; r0 = r1 ; r1 = tmp ;
            tmp = c0 ; c0 = c1 ; c1 = tmp ;
            tmp = v0 ; v0 = v1 ; v1 = tmp ;
            tmp = otherEnd0True ; otherEnd0True = otherEnd1True ; otherEnd1True = tmp ;
            tmp = otherEnd0False ; otherEnd0False = otherEnd1False ; otherEnd1False = tmp ;            
        } else {
            isOrderReversed = false ;
        }
        if( ! recordChain( r0 , c0 , v0 , otherEnd0True , otherEnd0False ,
                           r1 , c1 , v1 , otherEnd1True , otherEnd1False ) ){
            return false ;
        }
        // Record the route, if necessary
        chainLength[nChains] = (byte)( chainLength[i] + chainLength[j] );
        if( explain && reportChains && chainLength[nChains] <= maxChainLength ){
//            chainNComponents[nChains] = 0 ;
            // Write 'i' string.
            kOffset = isOrderReversed ? chainLength[j] : 0 ;
            k = 0 ;
            while( k < chainLength[i] ){
                if( isIReversed ){
                    chainRoute[nChains][k+kOffset] = chainRoute[i][chainLength[i]-1-k];
                    isLinkAscending[nChains][k+kOffset] = ! isLinkAscending[i][chainLength[i]-1-k];
                } else {
                    chainRoute[nChains][k+kOffset] = chainRoute[i][k];
                    isLinkAscending[nChains][k+kOffset] = isLinkAscending[i][k];
                }
                ++ k ;
            }
            // Write 'j' string.
            kOffset = isOrderReversed ? 0 : chainLength[i] ;
            k = 0 ;
            while( k < chainLength[j] ){
                if( isJReversed ){
                    chainRoute[nChains][k+kOffset] = chainRoute[j][chainLength[j]-1-k];
                    isLinkAscending[nChains][k+kOffset] = ! isLinkAscending[j][chainLength[j]-1-k];
                } else {
                    chainRoute[nChains][k+kOffset] = chainRoute[j][k];
                    isLinkAscending[nChains][k+kOffset] = isLinkAscending[j][k];
                }
                ++ k ;
            }
        }
        ++ nChains ;
        
        return true ;
    }

    boolean recordChain( int r0 ,
                         int c0 ,
                         int v0 ,
                         int otherEnd0True ,
                         int otherEnd0False ,
                         int r1 ,
                         int c1 ,
                         int v1 ,
                         int otherEnd1True ,
                         int otherEnd1False ){
        final CellState cellState = (CellState) lcc.state ;
        int k ;
        boolean hasNewInfo , twoCandidates ;
        // The chain should have been inverted if necessary prior to this call.
        assert r0 < r1 || r0 == r1 && c0 <= c1 ;
        // Check whether the chain, or a stronger form of it, has already been recorded.
        k = 0 ;
        while( k < nChains ){
            if( r0 == chainR0[k] && c0 == chainC0[k] && v0 == chainV0[k] && 
                r1 == chainR1[k] && c1 == chainC1[k] && v1 == chainV1[k] ){
                // Check whether the new chain has any new info not in the old.
                hasNewInfo = false ;
                twoCandidates = cellState.nEliminated[r0][c0] < grid.cellsInRow - 2 ;
                if( otherEnd0True != DONT_KNOW ){
                    hasNewInfo = chainOtherEnd0[k][TRUE] == DONT_KNOW || isAssertionConfirmed( chainV0[k] , chainOtherEnd0[k][TRUE] , v0 , otherEnd0True , twoCandidates ) == 2 ;
                }
                if( ! hasNewInfo && otherEnd0False != DONT_KNOW ){
                    hasNewInfo = chainOtherEnd0[k][FALSE] == DONT_KNOW || isAssertionConfirmed( chainV0[k] , chainOtherEnd0[k][FALSE] , v0 , otherEnd0False , twoCandidates ) == 2 ;
                }
                twoCandidates = cellState.nEliminated[r1][c1] < grid.cellsInRow - 2 ;
                if( ! hasNewInfo && otherEnd1True != DONT_KNOW ){
                    hasNewInfo = chainOtherEnd1[k][TRUE] == DONT_KNOW || isAssertionConfirmed( chainV1[k] , chainOtherEnd1[k][TRUE] , v1 , otherEnd1True , twoCandidates ) == 2 ;
                }
                if( ! hasNewInfo && otherEnd1False != DONT_KNOW ){
                    hasNewInfo = chainOtherEnd1[k][FALSE] == DONT_KNOW || isAssertionConfirmed( chainV1[k] , chainOtherEnd1[k][FALSE] , v1 , otherEnd1False , twoCandidates ) == 2 ;
                }                
                if( ! hasNewInfo ){
                    return false ;
                }
            }
            ++ k ;
        }
        // Write to the list.
        if( nChains == maxChains ){
            System.err.println("Chain Builder is full with " + maxChains + " elements");
            return false ;
        }
        chainR0[nChains] = (byte) r0 ;
        chainC0[nChains] = (byte) c0 ;
        chainV0[nChains] = (byte) v0 ;
        chainOtherEnd0[nChains][TRUE] = (byte) otherEnd0True;   
        chainOtherEnd0[nChains][FALSE] = (byte) otherEnd0False;   
        chainR1[nChains] = (byte) r1 ;
        chainC1[nChains] = (byte) c1 ;
        chainV1[nChains] = (byte) v1 ;
        chainOtherEnd1[nChains][TRUE] = (byte) otherEnd1True;   
        chainOtherEnd1[nChains][FALSE] = (byte) otherEnd1False;
        
        return true ;
    }
/*    
    void recordRoute( int c1 , 
                      int c2 ,
                      boolean isC1Reversed ,
                      boolean isC2Reversed ,
                      boolean isOrderReversed ){
        int k , kOffset ; 
        chainLength[nChains] = (byte)( chainLength[c1] + chainLength[c2] );
        // Record the route, if necessary
        if( explain && reportChains && chainLength[nChains] <= maxChainLength ){
//            chainNComponents[nChains] = 0 ;
            // Write 'i' string.
            kOffset = isOrderReversed ? chainLength[c2] : 0 ;
            k = 0 ;
            while( k < chainLength[c1] ){
                if( isC1Reversed ){
                    chainRoute[nChains][k+kOffset] = chainRoute[c1][chainLength[c1]-1-k];
                    isLinkAscending[nChains][k+kOffset] = ! isLinkAscending[c1][chainLength[c1]-1-k];
                } else {
                    chainRoute[nChains][k+kOffset] = chainRoute[c1][k];
                    isLinkAscending[nChains][k+kOffset] = isLinkAscending[c1][k];
                }
                ++ k ;
            }
            // Write 'j' string.
            kOffset = isOrderReversed ? 0 : chainLength[c1] ;
            k = 0 ;
            while( k < chainLength[c2] ){
                if( isC2Reversed ){
                    chainRoute[nChains][k+kOffset] = chainRoute[c2][chainLength[c2]-1-k];
                    isLinkAscending[nChains][k+kOffset] = ! isLinkAscending[c2][chainLength[c2]-1-k];
                } else {
                    chainRoute[nChains][k+kOffset] = chainRoute[c2][k];
                    isLinkAscending[nChains][k+kOffset] = isLinkAscending[c2][k];
                }
                ++ k ;
            }
        }
    }
*/    
    void addChainToTables( short nChain ){
        // Reject cyclic chains.
        if( chainR0[nChain] == chainR1[nChain] && chainC0[nChain] == chainC1[nChain] ){
            return ;
        }
        // Otherwise, write whatever information is available.
        if( chainOtherEnd0[nChain][TRUE] != DONT_KNOW ){
            addChainToTables( nChain , chainR0[nChain] , chainC0[nChain] , TRUE , true );
        }
        if( chainOtherEnd0[nChain][FALSE] != DONT_KNOW ){
            addChainToTables( nChain , chainR0[nChain] , chainC0[nChain] , FALSE , true );
        }
        if( chainOtherEnd1[nChain][TRUE] != DONT_KNOW ){
            addChainToTables( nChain , chainR1[nChain] , chainC1[nChain] , TRUE , false );
        }
        if( chainOtherEnd1[nChain][FALSE] != DONT_KNOW ){
            addChainToTables( nChain , chainR1[nChain] , chainC1[nChain] , FALSE , false );
        }
    }
    
    void addChainToTables( short nChain , int r , int c , int bool , boolean end0 ){
        assert bool != DONT_KNOW ;
        final int boxSector = 2 * grid.cellsInRow + r / grid.boxesAcross * grid.boxesAcross + c / grid.boxesDown ,
                  boxOffset = r % grid.boxesAcross * grid.boxesDown + c % grid.boxesDown ;
        addChainToCellTable( nChain , r , c , bool , end0 );
        addChainToSectorTable( nChain , r , c , bool , end0 );
        addChainToSectorTable( nChain , c + grid.cellsInRow , r , bool , end0 );
        addChainToSectorTable( nChain , boxSector , boxOffset , bool , end0 );        
    }
    
    void addChainToSectorTable( short nChain , int sector , int offset , int bool , boolean end0 ){
        if( chainSectorTableSize[sector][offset][bool] < maxTabledChains ){
            chainSectorTableIndex[sector][offset][bool][chainSectorTableSize[sector][offset][bool]] = nChain ;
            chainSectorTableEnd0[sector][offset][bool][chainSectorTableSize[sector][offset][bool]] = end0 ;
            ++ chainSectorTableSize[sector][offset][bool];          
        } else {
            System.err.println("Tabled chains Builder is full with " + maxTabledChains + " elements");            
        }
    }
    
    void addChainToCellTable( short nChain , int r , int c , int bool , boolean end0 ){
        if( chainCellTableSize[r][c][bool] < maxTabledChains ){
            chainCellTableIndex[r][c][bool][chainCellTableSize[r][c][bool]] = nChain ;
            chainCellTableEnd0[r][c][bool][chainCellTableSize[r][c][bool]] = end0 ;
            ++ chainCellTableSize[r][c][bool];          
        } else {
            System.err.println("Tabled chains Builder is full with " + maxTabledChains + " elements");            
        }
    }

    /**
     * Calculates whether the second assertion is consistent with the first.
     */

    boolean isAssertionConsistent( int b1 , int v1 , int b2 , int v2 , boolean twoCandidates ){
        return  b1 == FALSE && b2 == FALSE && ( ! twoCandidates || v1 == v2 ) || 
                v1 == v2 && b1 == TRUE && b2 == TRUE || 
                v1 != v2 && b1 != b2 ;
    }

    /**
     * Calculates whether the second assertion confirms the first.
     * Returns 1 if it does, 2 if it doersn't but the first assertion
     * confirms the second or 0 otherwise.
     */

    int isAssertionConfirmed( int b1 , int v1 , int b2 , int v2 , boolean twoCandidates ){
        if( b1 == b2 && v1 == v2 || 
            b1 == FALSE && b2 == TRUE && v1 != v2 ||
            twoCandidates && b1 == TRUE && b2 == FALSE && v1 != v2 ){
            return 1 ;
        } else if( b1 == TRUE && b2 == FALSE && v1 != v2 ) {
            return 2 ;
        } else {
            return 0 ;
        }
    }
    
    /**
     * Scans through the table for the lowest-indexed entry that
     * confirms (confirm=true) or contradicts (confirm=false) the
     * assertion defined by {row,column,value,bool}. When no 
     * match is found, tableSize[assertionType] is returned.
     */

    int tableMatchConfirmations( int value ,
                                 int otherEndRow ,
                                 int otherEndColumn ,
                                 int otherEndValue ,
                                 int bool ,
                                 int assertionType ,
                                 short[] tableSize , 
                                 short[][] tableIndex , 
                                 boolean[][] tableEnd0 ){
        final CellState cellState = (CellState) lcc.state ;
        boolean twoCandidates ; 
        int info , rowInfo , columnInfo , thisValue , valueInfo , booleanInfo ;
        int i = 0 ;
        while( i < tableSize[assertionType] ){
            info = tableIndex[assertionType][i];
            if( tableEnd0[assertionType][i] ){
                rowInfo     = chainR1[info];
                columnInfo  = chainC1[info];
                thisValue   = chainV0[info];
                valueInfo   = chainV1[info];
                booleanInfo = chainOtherEnd0[info][assertionType];
            } else {
                rowInfo     = chainR0[info];
                columnInfo  = chainC0[info];
                thisValue   = chainV1[info];
                valueInfo   = chainV0[info];
                booleanInfo = chainOtherEnd1[info][assertionType];
            }
            if( thisValue != value ){
                ++ i ;
                continue ;
            }
            if( rowInfo == otherEndRow && columnInfo == otherEndColumn ){
                twoCandidates = grid.cellsInRow - cellState.nEliminated[otherEndRow][otherEndColumn] == 2 ; 
                switch( isAssertionConfirmed( bool , otherEndValue , booleanInfo , valueInfo , twoCandidates ) ){
                case 1 :
                    return i ;
                case 2 :
                    return i + tableSize[assertionType];
                }
            }
            ++ i ;
        }
        return 2 * tableSize[assertionType];
    }
    
    /**
     * Scans through the table for the lowest-indexed entry that
     * contradicts (confirm=false) the assertion defined 
     * by {row,column,value,bool}. When no match is found, 
     * 4*tableSize[assertionType] is returned.
     */

    int tableMatchContradictions( int value ,
                                  int otherEndRow ,
                                  int otherEndColumn ,
                                  int otherEndValue ,
                                  int bool ,
                                  int assertionType ,
                                  short[] tableSize , 
                                  short[][] tableIndex , 
                                  boolean[][] tableEnd0 ){
        final CellState cellState = (CellState) lcc.state ;
        final NumberState numberState = (NumberState) lcn.state ;
        boolean twoCandidates ; 
        int info , rowInfo , columnInfo , thisValue , valueInfo , booleanInfo ;
        int sectorType , otherEndSector = -1 , otherEndOffset = -1 , sectorInfo = -1 , offsetInfo = -1 ;

        int i = 0 ;
        while( i < tableSize[assertionType] ){
            info = tableIndex[assertionType][i];
            if( tableEnd0[assertionType][i] ){
                rowInfo     = chainR1[info];
                columnInfo  = chainC1[info];
                thisValue   = chainV0[info];
                valueInfo   = chainV1[info];
                booleanInfo = chainOtherEnd0[info][assertionType];
            } else {
                rowInfo     = chainR0[info];
                columnInfo  = chainC0[info];
                thisValue   = chainV1[info];
                valueInfo   = chainV0[info];
                booleanInfo = chainOtherEnd1[info][assertionType];
            }
            if( thisValue != value ){
                ++ i ;
                continue ;
            }
            twoCandidates = grid.cellsInRow - cellState.nEliminated[otherEndRow][otherEndColumn] == 2 ; 
            if( rowInfo == otherEndRow && columnInfo == otherEndColumn && 
                ! isAssertionConsistent( bool , otherEndValue , booleanInfo , valueInfo , twoCandidates ) ){
                return i ;
            }
            sectorType = 0 ;
            while( sectorType < 3 ){
                // Convert row/column info into sector/offset.
                switch( sectorType ){
                    case 0:
                    // Row
                    otherEndSector = otherEndRow ;
                    otherEndOffset = otherEndColumn ;
                    sectorInfo = rowInfo ;
                    offsetInfo = columnInfo ;
                    break;
                    
                    case 1:
                    // Column
                    otherEndSector = otherEndColumn + grid.cellsInRow ;
                    otherEndOffset = otherEndRow ;
                    sectorInfo = columnInfo + grid.cellsInRow ;
                    offsetInfo = rowInfo ;
                    break;
                    
                    case 2:
                    // Box
                    otherEndSector = 2 * grid.cellsInRow + otherEndRow / grid.boxesAcross * grid.boxesAcross + otherEndColumn / grid.boxesDown ;
                    otherEndOffset = otherEndRow % grid.boxesAcross * grid.boxesDown + otherEndColumn % grid.boxesDown ;
                    sectorInfo = 2 * grid.cellsInRow + rowInfo / grid.boxesAcross * grid.boxesAcross + columnInfo / grid.boxesDown ;
                    offsetInfo = rowInfo % grid.boxesAcross * grid.boxesDown + columnInfo % grid.boxesDown ;
                    break;                
                }
                twoCandidates = grid.cellsInRow - numberState.nEliminated[otherEndValue][otherEndSector] == 2 ; 
                if( valueInfo == otherEndValue && sectorInfo == otherEndSector && 
                    ! isAssertionConsistent( bool , otherEndOffset , booleanInfo , offsetInfo , twoCandidates ) ){
                    return i + ( sectorType + 1 )* tableSize[assertionType];
                }
                ++ sectorType ;
            }
            ++ i ;
        }
        return 4 * tableSize[assertionType];
    }
    
    boolean tableEliminations( short nChain , boolean end0 , StringBuilder sb ){
        boolean anyValuesEliminated = false ;
        // Reject cyclic chains.
        if( chainR0[nChain] == chainR1[nChain] && chainC0[nChain] == chainC1[nChain] ){
            return anyValuesEliminated ;
        }
        // Check whether the new information would contradict any existing information.
        if( end0 ){
            if( chainOtherEnd0[nChain][TRUE] != DONT_KNOW ){
                anyValuesEliminated = tableContradictions( nChain , true , TRUE , sb );
            }
            if( ! anyValuesEliminated && chainOtherEnd0[nChain][FALSE] != DONT_KNOW ){
                anyValuesEliminated = tableContradictions( nChain , true , FALSE , sb );
            }
        } else {
            if( chainOtherEnd1[nChain][TRUE] != DONT_KNOW ){
                anyValuesEliminated = tableContradictions( nChain , false , TRUE , sb );
            }
            if( ! anyValuesEliminated && chainOtherEnd1[nChain][FALSE] != DONT_KNOW ){
                anyValuesEliminated = tableContradictions( nChain , false , FALSE , sb );
            }
        }
        // Check that useful information is available.
        if( ! anyValuesEliminated ){
            if( end0 && chainOtherEnd0[nChain][TRUE] != DONT_KNOW ||
              ! end0 && chainOtherEnd1[nChain][TRUE] != DONT_KNOW ){
                if( anyValuesEliminated = cellTableEliminations( nChain , end0 , TRUE , sb ) || sectorTableEliminations( nChain , end0 , TRUE , sb ) ){
                    return anyValuesEliminated ;
                }
            }
            if( end0 && chainOtherEnd0[nChain][FALSE] != DONT_KNOW ||
              ! end0 && chainOtherEnd1[nChain][FALSE] != DONT_KNOW  ){
                anyValuesEliminated = cellTableEliminations( nChain , end0 , FALSE , sb ) || sectorTableEliminations( nChain , end0 , FALSE , sb );
            }            
        }
        return anyValuesEliminated ;
    }

    boolean tableContradictions( int nChain , boolean end0 , int bool , StringBuilder sb ){
        int i , t , size ;
        final CellState cellState = (CellState) lcc.state ;
        final int r = end0 ? chainR0[nChain] : chainR1[nChain] , 
                  rOther = end0 ? chainR1[nChain] : chainR0[nChain] , 
                  c = end0 ? chainC0[nChain] : chainC1[nChain] , 
                  cOther = end0 ? chainC1[nChain] : chainC0[nChain] , 
                  v = end0 ? chainV0[nChain] : chainV1[nChain] ,
                  vOther = end0 ? chainV1[nChain] : chainV0[nChain] ,
                  b = end0 ? chainOtherEnd0[nChain][bool] : chainOtherEnd1[nChain][bool] ;
        // Check whether the new chain contradicts an earlier chain.  
        
        // Check versus other chains that make the same assumption.
        size = chainCellTableSize[r][c][bool];
        i = tableMatchContradictions( v ,
                                      rOther , 
                                      cOther , 
                                      vOther , 
                                      b , 
                                      bool , 
                                      chainCellTableSize[r][c] , 
                                      chainCellTableIndex[r][c] , 
                                      chainCellTableEnd0[r][c] );                
        if( i < size ){
            if( explain ){
                if( reportChains ){
                    sb.append("Consider the chains ");
                    appendChain( sb , nChain );
                    sb.append(" and ");
                    appendChain( sb , chainCellTableIndex[r][c][bool][i] );
                    sb.append(".\n");
                }
                sb.append("When the cell ");
                SuDokuUtils.appendCell( sb , r , c );
                if( bool == TRUE ){
                    sb.append(" contains");
                } else {
                    sb.append(" doesn't contain");
                }
                sb.append(" the value ");
                SuDokuUtils.appendValue( sb , v );
                sb.append(", one chain states that the cell ");
                SuDokuUtils.appendCell( sb , rOther , cOther );
                sb.append(" contains the value ");
                SuDokuUtils.appendValue( sb , vOther );
                sb.append(" while the other says it doesn't - a contradiction.\nTherefore, the cell ");
                SuDokuUtils.appendCell( sb , r , c );
                if( bool == TRUE ){
                    sb.append(" cannot");
                } else {
                    sb.append(" must");
                }
                sb.append(" contain the value ");
                SuDokuUtils.appendValue( sb , v );
                sb.append(".\n");
            }
            if( bool == TRUE ){
                eliminateMove( r , c , v );
                ++ chainsEliminations ;
            } else {
                chainsEliminations += addMove( r , c , v );
            }
            return true ;
        } else if( i < 4 * size ){
            if( explain ){
                if( reportChains ){
                    sb.append("Consider the chains ");
                    appendChain( sb , nChain );
                    sb.append(" and ");
                    appendChain( sb , chainCellTableIndex[r][c][bool][i%size] );
                    sb.append(".\n");
                }
                sb.append("When the cell ");
                SuDokuUtils.appendCell( sb , r , c );
                if( bool == TRUE ){
                    sb.append(" contains");
                } else {
                    sb.append(" doesn't contain");
                }
                sb.append(" the value ");
                SuDokuUtils.appendValue( sb , v );
                sb.append(", one chain states that the value ");
                SuDokuUtils.appendValue( sb , vOther );
                sb.append(" in ");
                if( i < 2 * size ){
                    SuDokuUtils.appendRow( sb , rOther );
                } else if( i < 3 * size ){
                    SuDokuUtils.appendColumn( sb , cOther );
                } else {
                    SuDokuUtils.appendBox( sb , grid.boxesAcross , grid.boxesDown , rOther , cOther );
                }
                sb.append(" belongs in the cell ");
                SuDokuUtils.appendCell( sb , rOther , cOther );
                sb.append(" while the other says it doesn't - a contradiction.\nTherefore, the cell ");
                SuDokuUtils.appendCell( sb , r , c );
                if( bool == TRUE ){
                    sb.append(" cannot");
                } else {
                    sb.append(" must");
                }
                sb.append(" contain the value ");
                SuDokuUtils.appendValue( sb , v );
                sb.append(".\n");
            }
            if( bool == TRUE ){
                eliminateMove( r , c , v );
                ++ chainsEliminations ;
            } else {
                chainsEliminations += addMove( r , c , v );
            }
            return true ;
        }         
        // Check versus other chains.
        size = chainCellTableSize[r][c][notBoolean( bool )];
        t = 0 ;
        while( t < grid.cellsInRow ){
            if( t == v || cellState.eliminated[r][c][t] ){
                ++ t ;
                continue ;
            }
            i = tableMatchContradictions( t ,
                                          rOther , 
                                          cOther , 
                                          vOther , 
                                          b , 
                                          notBoolean( bool ) , 
                                          chainCellTableSize[r][c] , 
                                          chainCellTableIndex[r][c] , 
                                          chainCellTableEnd0[r][c] );                
            if( i < size ){
                if( explain ){
                    if( reportChains ){
                        sb.append("Consider the chains ");
                        appendChain( sb , nChain );
                        sb.append(" and ");
                        appendChain( sb , chainCellTableIndex[r][c][notBoolean( bool )][i] );
                        sb.append(".\n");
                    }
                    sb.append("When the cell ");
                    SuDokuUtils.appendCell( sb , r , c );
                    sb.append(" contains the value ");
                    if( bool == TRUE ){
                        SuDokuUtils.appendValue( sb , v );
                    } else {
                        SuDokuUtils.appendValue( sb , t );                        
                    }
                    sb.append(", one chain states that the cell ");
                    SuDokuUtils.appendCell( sb , rOther , cOther );
                    sb.append(" contains the value ");
                    SuDokuUtils.appendValue( sb , vOther );
                    sb.append(" while the other says it doesn't - a contradiction.\nTherefore, the cell ");
                    SuDokuUtils.appendCell( sb , r , c );
                    sb.append(" cannot contain the value ");
                    if( bool == TRUE ){
                        SuDokuUtils.appendValue( sb , v );
                    } else {
                        SuDokuUtils.appendValue( sb , t );
                    }
                    sb.append(".\n");
                }
                if( bool == TRUE ){
                    eliminateMove( r , c , v );
                } else {
                    eliminateMove( r , c , t );
                }
                ++ chainsEliminations ;                        
                return true ;
            } else if( i < 4 * size ){
                if( explain ){
                    if( reportChains ){
                        sb.append("Consider the chains ");
                        appendChain( sb , nChain );
                        sb.append(" and ");
                        appendChain( sb , chainCellTableIndex[r][c][notBoolean( bool )][i%size] );
                        sb.append(".\n");
                    }
                    sb.append("When the cell ");
                    SuDokuUtils.appendCell( sb , r , c );
                    sb.append(" contains the value ");
                    if( bool == TRUE ){
                        SuDokuUtils.appendValue( sb , v );
                    } else {
                        SuDokuUtils.appendValue( sb , t );                        
                    }
                    sb.append(", one chain states that the value ");
                    SuDokuUtils.appendValue( sb , vOther );
                    sb.append(" in ");
                    if( i < 2 * size ){
                        SuDokuUtils.appendRow( sb , rOther );
                    } else if( i < 3 * size ){
                        SuDokuUtils.appendColumn( sb , cOther );
                    } else {                        
                        SuDokuUtils.appendBox( sb , grid.boxesAcross , grid.boxesDown , rOther , cOther );
                    }
                    sb.append(" belongs in the cell ");
                    SuDokuUtils.appendCell( sb , rOther , cOther );
                    sb.append(" while the other says it doesn't - a contradiction.\nTherefore, the cell ");
                    SuDokuUtils.appendCell( sb , r , c );
                    sb.append(" cannot contain the value ");
                    if( bool == TRUE ){
                        SuDokuUtils.appendValue( sb , v );
                    } else {
                        SuDokuUtils.appendValue( sb , t );
                    }
                    sb.append(".\n");
                }
                if( bool == TRUE ){
                    eliminateMove( r , c , v );
                } else {
                    eliminateMove( r , c , t );
                }
                ++ chainsEliminations ;                        
                return true ;
            } 
            ++ t ;
        }
        return false ;
    }
    
    boolean cellTableEliminations( short nChain , boolean end0 , int bool , StringBuilder sb ){
        int i , t , tCount , tOmitted , tMatched , assertedBoolean , assertedValue ;
        final CellState cellState = (CellState) lcc.state ;
        int r = end0 ? chainR0[nChain] : chainR1[nChain] , 
            rOther = end0 ? chainR1[nChain] : chainR0[nChain] , 
            c = end0 ? chainC0[nChain] : chainC1[nChain] , 
            cOther = end0 ? chainC1[nChain] : chainC0[nChain] , 
            v = end0 ? chainV0[nChain] : chainV1[nChain] ,
            vOther = end0 ? chainV1[nChain] : chainV0[nChain] ,
            b = end0 ? chainOtherEnd0[nChain][bool] : chainOtherEnd1[nChain][bool];
        final int size = chainCellTableSize[r][c][bool] ,
                  nCandidates = grid.cellsInRow - cellState.nEliminated[r][c] ;
        // The chain shouldn't be cyclic.
        assert r != rOther || c != cOther ;
        //
        if( size > 0 && nCandidates > 2 ){
            // Check whether the new information is shared by all other candidates for the cell.
            assertedBoolean = b ;
            assertedValue = vOther ;
            tOmitted = tMatched = -1 ;
            tCount = 0 ;
            t = 0 ;
            storeInfo:
            while( t < grid.cellsInRow ){
                if( t == v || cellState.eliminated[r][c][t] ){
                    ++ t ;
                    continue ;
                }
                i = tableMatchConfirmations( t ,
                                             rOther , 
                                             cOther , 
                                             assertedValue , 
                                             assertedBoolean , 
                                             bool , 
                                             chainCellTableSize[r][c] , 
                                             chainCellTableIndex[r][c] , 
                                             chainCellTableEnd0[r][c] );
                if( i < 2 * size ){
                    if( i >= size ){
                        assert assertedBoolean == TRUE ;
                        assertedBoolean = FALSE ;
                        int info = chainCellTableIndex[r][c][bool][i%size];
                        if( chainCellTableEnd0[r][c][bool][i%size] ){
                            assertedValue = chainV1[info];
                        } else {
                            assertedValue = chainV0[info];
                        }                    
                    }                    
                    chainTableIndices[tCount++] = chainCellTableIndex[r][c][bool][i%size];
                    tMatched = t ;
                } else {
                    tOmitted = t ;
                }
                ++ t ;
            }
            if( bool == TRUE && tCount == nCandidates - 1 ||
                bool == FALSE && tCount == 1 ){
                // The assertion is definitely true.
                if( explain ){
                    if( reportChains ){
                        sb.append("Consider the chains ");
                        appendChain( sb , nChain );
                        t = 0 ;
                        while( t < tCount - 1 ){
                            sb.append(", ");
                            appendChain( sb , chainTableIndices[t] );
                            ++ t ;
                        }                            
                        sb.append(" and ");
                        appendChain( sb , chainTableIndices[t] );
                        sb.append(".\n");
                    }
                    if( bool == TRUE ){
                        sb.append("Whichever of the ");
                        sb.append( nCandidates );
                        sb.append(" candidate values fills the cell ");
                        SuDokuUtils.appendCell( sb , r , c );
                    } else {
                        assert tMatched >= 0 ;
                        sb.append("Since it is certain that the cell ");
                        SuDokuUtils.appendCell( sb , r , c );
                        sb.append(" will not contain at least one of the values ");
                        SuDokuUtils.appendValue( sb , v );
                        sb.append(" and ");
                        SuDokuUtils.appendValue( sb , tMatched );
                    }
                    sb.append(", the cell ");
                    SuDokuUtils.appendCell( sb , rOther , cOther );
                    if( assertedBoolean == TRUE ){
                        sb.append(" must contain");
                    } else {
                        sb.append(" cannot contain");
                    }
                    sb.append(" the value ");
                    SuDokuUtils.appendValue( sb , assertedValue );
                    sb.append(".\n");
                }                    
                switch( assertedBoolean ){
                case TRUE:
                    chainsEliminations += addMove( rOther , cOther , assertedValue );
                    break;
                case FALSE:
                    eliminateMove( rOther , cOther , assertedValue );
                    ++ chainsEliminations ;
                    break;                    
                }
                return true ;
                
            } else if( bool == TRUE && tCount == nCandidates - 2 ){
                // The assertion will be true provided the cell (r,c) doesn't contain tOmitted.
                // Add an INFERRED link.
                assert tOmitted >= 0 ;
                // The assertion will hold if (r,c):<>tOmitted.
                // Check whether that's already known.
                i = tableMatchConfirmations( tOmitted ,
                                             rOther , 
                                             cOther , 
                                             assertedValue , 
                                             assertedBoolean , 
                                             FALSE , 
                                             chainCellTableSize[r][c] , 
                                             chainCellTableIndex[r][c] , 
                                             chainCellTableEnd0[r][c] );
                    
                if( i >= chainCellTableSize[r][c][FALSE] ){
                    // It's not known, so add a new chain.
                    // Check whether the chain direction should be reversed.
                    int x0 = r ,
                        y0 = c ,
                        v0 = tOmitted ,
                        x1 = rOther ,
                        y1 = cOther ,
                        v1 = assertedValue ,
                        otherEnd0False = assertedBoolean ,
                        otherEnd1False = DONT_KNOW ;
                    if( r > rOther || r == rOther && c > cOther ){
                        int tmp ;
                        tmp = x0 ; x0 = x1 ; x1 = tmp ;
                        tmp = y0 ; y0 = y1 ; y1 = tmp ;
                        tmp = v0 ; v0 = v1 ; v1 = tmp ;
                        tmp = otherEnd0False ; otherEnd0False = otherEnd1False ; otherEnd1False = tmp ;
                    }
                    // Record the endpoint details.                      
                    if( ! recordChain( x0 , y0 , v0 , DONT_KNOW , otherEnd0False ,
                                       x1 , y1 , v1 , DONT_KNOW , otherEnd1False ) ){
                        return false ;
                    }
                    linkCategory[nChains] = INFERRED ;
                    chainLength[nChains] = 1 ;  
                    // Record the route, if necessary
                    chainRoute[nChains][0] = nChains ;
                    isLinkAscending[nChains][0] = true ;
                    ++ nChains ;
                }                              
            }            
        }
        // Store the new information.
        addChainToCellTable( nChain , r , c , bool , end0 );                        
        return false  ;
    }
    
    boolean sectorTableEliminations( short nChain , boolean end0 , int bool , StringBuilder sb ){
        // Check to see which sectors might be appropriate.
        int sectorType , sector = -1 , offset = -1 , t , tCount , tOmitted , tMatched , i , nCandidates , size , assertedBoolean , assertedValue ;
        final NumberState numberState = (NumberState) lcn.state ;
        final int r = end0 ? chainR0[nChain] : chainR1[nChain] , 
                  rOther = end0 ? chainR1[nChain] : chainR0[nChain] , 
                  c = end0 ? chainC0[nChain] : chainC1[nChain] , 
                  cOther = end0 ? chainC1[nChain] : chainC0[nChain] , 
                  v = end0 ? chainV0[nChain] : chainV1[nChain] ,
                  vOther = end0 ? chainV1[nChain] : chainV0[nChain] ,
                  b = end0 ? chainOtherEnd0[nChain][bool] : chainOtherEnd1[nChain][bool];
        sectorType = 0 ;
        while( sectorType < 3 ){
            // Convert row/column info into sector/offset.
            switch( sectorType ){
                case 0:
                // Row
                sector = r ;
                offset = c ;
                break;
                
                case 1:
                // Column
                sector = c + grid.cellsInRow ;
                offset = r ;
                break;
                
                case 2:
                // Box
                sector = 2 * grid.cellsInRow + r / grid.boxesAcross * grid.boxesAcross + c / grid.boxesDown ;
                offset = r % grid.boxesAcross * grid.boxesDown + c % grid.boxesDown ;
                break;                
            }
            // Count the number of candidates.
            if( ( nCandidates = grid.cellsInRow - numberState.nEliminated[v][sector] ) > 2 ){
                // Check whether the new information is shared by all other candidates in the sector.
                assertedBoolean = b ;
                assertedValue = vOther ;
                tOmitted = tMatched = -1 ;
                tCount = 0 ;
                t = 0 ;
                storeInfo:
                while( t < grid.cellsInRow ){
                    if( t == offset || numberState.eliminated[v][sector][t] ){ 
                        ++ t ;
                        continue ;
                    }
                    size = chainSectorTableSize[sector][t][bool];
                    
                    i = tableMatchConfirmations( v ,
                                                 rOther , 
                                                 cOther , 
                                                 assertedValue , 
                                                 assertedBoolean , 
                                                 bool , 
                                                 chainSectorTableSize[sector][t] , 
                                                 chainSectorTableIndex[sector][t] , 
                                                 chainSectorTableEnd0[sector][t] );
                                    
                    if( i < 2 * size ){
                        if( i >= size ){
                            assert assertedBoolean == TRUE ;
                            assertedBoolean = FALSE ;
                            int info = chainSectorTableIndex[sector][t][bool][i%size];
                            if( chainSectorTableEnd0[sector][t][bool][i%size] ){
                                assertedValue = chainV1[info];
                            } else {
                                assertedValue = chainV0[info];
                            }                    
                        }
                        chainTableIndices[tCount++] = chainSectorTableIndex[sector][t][bool][i%size];                    
                        tMatched = t ;
                    } else {
                        tOmitted = t ;
                    }                  
                    ++ t ;
                }
                if( bool == TRUE && tCount == nCandidates - 1 ||
                    bool == FALSE && tCount == 1 ){
                    // The assertion is definitely true.
                    if( explain ){
                        if( reportChains ){
                            sb.append("Consider the chains ");
                            appendChain( sb , nChain );
                            t = 0 ;
                            while( t < tCount - 1 ){
                                sb.append(", ");
                                appendChain( sb , chainTableIndices[t] );
                                ++ t ;
                            }                            
                            sb.append(" and ");
                            appendChain( sb , chainTableIndices[t] );
                            sb.append(".\n");
                        }
                        if( bool == TRUE ){
                            sb.append("Whichever of the ");
                            sb.append( nCandidates );
                            sb.append(" candidates in ");
                            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , sector );
                            sb.append(" contains the value ");
                            SuDokuUtils.appendValue( sb , v );
                        } else {
                            assert tMatched >= 0 ;
                            int rMatched , cMatched ;
                            if( sector < grid.cellsInRow ){
                                rMatched = sector ;
                                cMatched = tMatched ;
                            } else if( sector < 2 * grid.cellsInRow ){
                                rMatched = tMatched ;
                                cMatched = sector - grid.cellsInRow ;
                            } else {                    
                                rMatched = ( sector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + tMatched / grid.boxesDown ;
                                cMatched = ( sector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + tMatched % grid.boxesDown ;
                            }
                            sb.append("Since it is certain that ");
                            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , sector );
                            sb.append(" will not contain the value ");
                            SuDokuUtils.appendValue( sb , v );
                            sb.append(" in at least one of the cells ");
                            SuDokuUtils.appendCell( sb , r , c );
                            sb.append(" and ");
                            SuDokuUtils.appendCell( sb , rMatched , cMatched );
                        }
                        sb.append(", the cell ");
                        SuDokuUtils.appendCell( sb , rOther , cOther );
                        if( assertedBoolean == TRUE ){
                            sb.append(" contains");
                        } else {
                            sb.append(" does not contain");
                        }
                        sb.append(" the value ");
                        SuDokuUtils.appendValue( sb , assertedValue );
                        sb.append(".\n");
                    }
                    switch( assertedBoolean ){
                        case TRUE:
                            chainsEliminations += addMove( rOther , cOther , assertedValue );
                            break;
                        case FALSE:
                            eliminateMove( rOther , cOther , assertedValue );
                            ++ chainsEliminations ;
                            break;                    
                    }
                    return true ;                    
                } else if( bool == TRUE && tCount == nCandidates - 2 ){
                    assert tOmitted >= 0 ;
                    // The assertion will hold if (x0,y0)<>v.
                    int x0 , y0 ;
                    if( sector < grid.cellsInRow ){
                        x0 = sector ;
                        y0 = tOmitted ;
                    } else if( sector < 2 * grid.cellsInRow ){
                        x0 = tOmitted ;
                        y0 = sector - grid.cellsInRow ;
                    } else {                    
                        x0 = ( sector - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + tOmitted / grid.boxesDown ;
                        y0 = ( sector - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + tOmitted % grid.boxesDown ;
                    }
                    // Filter out trivial links.
                    if( x0 != rOther || y0 != cOther ){
                        // Check whether that's already known.
                        i = tableMatchConfirmations( v ,
                                                     rOther , 
                                                     cOther , 
                                                     assertedValue , 
                                                     assertedBoolean , 
                                                     FALSE , 
                                                     chainCellTableSize[x0][y0] , 
                                                     chainCellTableIndex[x0][y0] , 
                                                     chainCellTableEnd0[x0][y0] );
       
                        if( i >= chainCellTableSize[x0][y0][FALSE] ){
                            // It's not known, so add a new chain.
                            // Check whether the chain direction should be reversed.
                            int v0 = v ,
                                x1 = rOther ,
                                y1 = cOther ,
                                v1 = assertedValue ,
                                otherEnd0False = assertedBoolean ,
                                otherEnd1False = DONT_KNOW ;
                            if( x0 > rOther || x0 == rOther && y0 > cOther ){
                                int tmp ;
                                tmp = x0 ; x0 = x1 ; x1 = tmp ;
                                tmp = y0 ; y0 = y1 ; y1 = tmp ;
                                tmp = v0 ; v0 = v1 ; v1 = tmp ;
                                tmp = otherEnd0False ; otherEnd0False = otherEnd1False ; otherEnd1False = tmp ;
                            }
                            // Record the endpoint details.  
                            if( ! recordChain ( x0 , y0 , v0 , DONT_KNOW , otherEnd0False ,
                                                x1 , y1 , v1 , DONT_KNOW , otherEnd1False ) ){
                                return false ;
                            }
                            linkCategory[nChains] = INFERRED ;
                            chainLength[nChains] = 1 ;  
                            // Record the route, if necessary
                            chainRoute[nChains][0] = nChains ;
                            isLinkAscending[nChains][0] = true ;
                                                        
                            ++ nChains ;
                        }                                        
                    }
                }
            }
            // Store the new information.
            addChainToSectorTable( nChain , sector , offset , bool , end0 );                            
            ++ sectorType ;            
        }
        return false ;
    }
    
    /**
     * Appends a description of the given string to the given string Builder.
     */

    void appendChain( StringBuilder sb , int s ){
        appendChain( sb , s , false );
    }
    
    void appendChain( StringBuilder sb , int s , boolean displayBooleans ){
/* 
        if( chainNComponents[s] > 0 ){
            sb.append('{');
            appendComponentChain( sb , chainComponents[s][0] );
            int i = 1 ;
            while( i < chainNComponents[s] ){
                sb.append(',');
                appendComponentChain( sb , chainComponents[s][i] );
                ++ i ;
            }
            sb.append('}');
        } else {   
*/        
            appendComponentChain( sb , s );
/*            
        }
*/       
        if( displayBooleans ){
            sb.append("\nT: ");
            sb.append( booleanChar( chainOtherEnd0[s][TRUE] ));
            sb.append(" ");
            sb.append( booleanChar( chainOtherEnd1[s][TRUE] ));
            sb.append("\n");
            sb.append("F: ");
            sb.append( booleanChar( chainOtherEnd0[s][FALSE] ));
            sb.append(" ");
            sb.append( booleanChar( chainOtherEnd1[s][FALSE] ));
        }
    }
    
    void appendComponentChain( StringBuilder sb , int s ){
        if( chainLength[s] > maxChainLength ){
            sb.append("<Chain length exceeds ");
            sb.append( maxChainLength );
            sb.append('>');
            return ;
        }
        int l = chainRoute[s][0] ;
        SuDokuUtils.appendCell( sb , isLinkAscending[s][0] ? chainR0[l] : chainR1[l] , isLinkAscending[s][0] ? chainC0[l] : chainC1[l] );
        int i = 0 ;
        while( i < chainLength[s] ){
            appendLink( sb , s , i );
            l = chainRoute[s][i] ;
            SuDokuUtils.appendCell( sb , isLinkAscending[s][i] ? chainR1[l] : chainR0[l] , isLinkAscending[s][i] ? chainC1[l] : chainC0[l] );            
            ++ i ;
        }
    }
    
    String booleanChar( int bool ){
        switch( bool ){
            case TRUE:
                return "T";
            case FALSE:
                return "F";
            case DONT_KNOW:
                return "?";
            default:
                return "!";            
        }
    }

    StringBuilder appendLink( StringBuilder sb , 
                              int sector ,
                              int segment ){
        
        final int link = chainRoute[sector][segment] ;
        switch( linkCategory[link] ){
        case NISHIO:
            assert chainV0[link] == chainV1[link] ;
            sb.append( linkChar( linkCategory[link] ) );
            sb.append( logicTableChar( linkCategory[link] , true ) );
            if( isLinkAscending[sector][segment] ){
                sb.append( booleanChar( chainOtherEnd0[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd0[link][FALSE] ) );
                SuDokuUtils.appendValue( sb , chainV1[link] );
                sb.append( booleanChar( chainOtherEnd1[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd1[link][FALSE] ) );                
            } else {
                sb.append( booleanChar( chainOtherEnd1[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd1[link][FALSE] ) );
                SuDokuUtils.appendValue( sb , chainV0[link] );
                sb.append( booleanChar( chainOtherEnd0[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd0[link][FALSE] ) );                
            }
            sb.append( logicTableChar( linkCategory[link] , false ) );
            sb.append( linkChar( linkCategory[link] ) );
            break;
        case INFERRED:
            sb.append( linkChar( linkCategory[link] ) );
            sb.append( logicTableChar( linkCategory[link] , true ) );
            if( isLinkAscending[sector][segment] ){
                sb.append( booleanChar( chainOtherEnd0[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd0[link][FALSE] ) );
                SuDokuUtils.appendValue( sb , chainV0[link] );
                sb.append('|');
                SuDokuUtils.appendValue( sb , chainV1[link] );
                sb.append( booleanChar( chainOtherEnd1[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd1[link][FALSE] ) );                
            } else {
                sb.append( booleanChar( chainOtherEnd1[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd1[link][FALSE] ) );
                SuDokuUtils.appendValue( sb , chainV1[link] );
                sb.append('|');
                SuDokuUtils.appendValue( sb , chainV0[link] );
                sb.append( booleanChar( chainOtherEnd0[link][TRUE] ) );     
                sb.append( booleanChar( chainOtherEnd0[link][FALSE] ) );                
            }
            sb.append( logicTableChar( linkCategory[link] , false ) );
            sb.append( linkChar( linkCategory[link] ) );
            break;
        default:
            sb.append( linkChar( linkCategory[link] ) );
            sb.append( isLinkAscending[sector][segment] ? SuDokuUtils.valueToString( chainV1[link] ) : SuDokuUtils.valueToString( chainV0[link] ) );
            sb.append( linkChar( linkCategory[link] ) );
            break ;
        }
        return sb ;
    }

    char linkChar( int linkCategory ){
        switch( linkCategory ){
        case STRONG:
            return '-';
        case WEAK:
            return '~';
        case EXTENDED:
            return '=';
        case NISHIO:
        case INFERRED:    
            return '+';
        default:
            assert false ;           
        }
        return (char) -1 ;    
    }
    
    char logicTableChar( int linkCategory ,
                         boolean open ){
        switch( linkCategory ){
        case NISHIO:
            return open ? '[' : ']';
        case INFERRED:
            return open ? '{' : '}';
        default:
            assert false ;
        }
        return (char) -1 ;
    }
    
    /**
     * Negates a logical enum.
     */

    int notBoolean( int bool ){
        switch( bool ){
        case TRUE :
            return FALSE ;
        case FALSE:
            return TRUE ;
        default:
            return bool ;
        }
    }
    
    /**
     * Searches for moves that would make it impossible to place the remaining values.
     * @param sb explanation
     * @return whether eliminations have been performed
     * @throws Exception the grid is in a bad state
     */
    
    boolean nishio( StringBuilder sb ){
        ++ nishioCalls ;
        int v ;
        // Generate a list of unit chains that use STRONG, WEAK and EXTENDED links.
        chainsEliminations = 0 ;
        resetChainTables( true );
        v = 0 ;
        while( v < grid.cellsInRow && ! addUnitChains( sb , v , true , true , true , true ) ){
            ++ v ;   
        }
        if( v < grid.cellsInRow ){
            nishioEliminations += chainsEliminations ;
            return true ;            
        }
        // Try to build longer chains.
        if( addLongChains( sb , true , true ) ){
            nishioEliminations += chainsEliminations ;
            return true ;
        }
        return false ;
    }
    
    void nishioInitiate( int v ){
        final CellState cellState = (CellState) lcc.state ;
        int r , c ;
        r = 0 ;
        while( r < grid.cellsInRow ){
            c = 0 ;
            while( c < grid.cellsInRow ){
                if( cellState.eliminated[r][c][v] ){
                    mask[r][c] = Nishio.NULL ;
                } else {
                    if( cellState.nEliminated[r][c] == grid.cellsInRow - 1 ){
                        mask[r][c] = Nishio.DEFINITE ; 
                    } else {
                        mask[r][c] = Nishio.POSSIBLE ; 
                    }
                }
                ++ c ;
            }
            ++ r ;
        }        
    }
    
    boolean nishioReduce( int x0 , int y0 ){
        int r , c , box , xUpper , xLower , yUpper , yLower , nPossibles ;
        boolean candidateNominated = true ;
        // Promote the nominated candidate, remove dependent candidates
        // and check the consistency of the resulting grid.
        checkConsistency:
        while( candidateNominated ){
            candidateNominated = false ;
            // Make definite the possible move (x0,y0):=v.
            mask[x0][y0] = Nishio.DEFINITE ;
            // Remove dependent candidates ...
            // ... from the row,
            c = 0 ;
            while( c < grid.cellsInRow ){
                if( mask[x0][c] == Nishio.POSSIBLE ){
                    mask[x0][c] = Nishio.NULL ;
                }
                ++ c ;
            }
            // ... the column
            r = 0 ;
            while( r < grid.cellsInRow ){
                if( mask[r][y0] == Nishio.POSSIBLE ){
                    mask[r][y0] = Nishio.NULL ;
                }
                ++ r ;
            }
            // ... and the box.
            xLower = ( x0 / grid.boxesAcross )* grid.boxesAcross ;
            xUpper = ( x0 / grid.boxesAcross + 1 )* grid.boxesAcross ;
            yLower = ( y0 / grid.boxesDown )* grid.boxesDown ;
            yUpper = ( y0 / grid.boxesDown + 1 )* grid.boxesDown ;
            r = xLower ;
            while( r < xUpper ){
                c = yLower ;
                while( c < yUpper ){
                    if( mask[r][c] == Nishio.POSSIBLE ){
                        mask[r][c] = Nishio.NULL ;
                    }
                    ++ c ;
                }
                ++ r ;
            }        
            // Check the consistency of
            // ... each row,
            r = 0 ;
            considerRow:
            while( r < grid.cellsInRow ){
                nPossibles = 0 ;
                c = 0 ;
                while( c < grid.cellsInRow ){
                    if( mask[r][c] == Nishio.DEFINITE ){
                        ++ r ;
                        continue considerRow ;
                    } else if( mask[r][c] == Nishio.POSSIBLE ){
                        if( ++ nPossibles > 1 ){
                            ++ r ;
                            continue considerRow ;
                        }
                    }
                    ++ c ;
                }
                if( nPossibles == 0 ){
                    return false ;
                } else if( ! candidateNominated ){
                    c = 0 ;
                    while( mask[r][c] != Nishio.POSSIBLE ){
                        ++ c ;
                    }
                    x0 = r ;
                    y0 = c ;
                    candidateNominated = true ;
                }
                ++ r ;
            }
            // ... column 
            c = 0 ;
            considerColumn:
            while( c < grid.cellsInRow ){
                nPossibles = 0 ;
                r = 0 ;
                while( r < grid.cellsInRow ){
                    if( mask[r][c] == Nishio.DEFINITE ){
                        ++ c ;
                        continue considerColumn ;
                    } else if( mask[r][c] == Nishio.POSSIBLE ){
                        if( ++ nPossibles > 1 ){
                            ++ c ;
                            continue considerColumn ;
                        }
                    }
                    ++ r ;
                }
                if( nPossibles == 0 ){
                    return false ;
                } else if( ! candidateNominated ){
                    r = 0 ;
                    while( mask[r][c] != Nishio.POSSIBLE ){
                        ++ r ;
                    }
                    x0 = r ;
                    y0 = c ;
                    candidateNominated = true ;
                }
                ++ c ;
            }
            // ... and box.
            box = 0 ;
            considerBox:
            while( box < grid.cellsInRow ){
                xLower = box / grid.boxesAcross * grid.boxesAcross ;
                xUpper = ( box / grid.boxesAcross + 1 )* grid.boxesAcross ;
                yLower = box % grid.boxesAcross * grid.boxesDown ;
                yUpper = ( box % grid.boxesAcross + 1 )* grid.boxesDown ;
                nPossibles = 0 ;
                r = xLower ;
                while( r < xUpper ){
                    c = yLower ;
                    while( c < yUpper ){
                        if( mask[r][c] == Nishio.DEFINITE ){
                            ++ box ;
                            continue considerBox ;
                        } else if( mask[r][c] == Nishio.POSSIBLE ){
                            if( ++ nPossibles > 1 ){
                                ++ box ;
                                continue considerBox ;
                            }
                        }
                        ++ c ;
                    }
                    ++ r ;
                }
                if( nPossibles == 0 ){
                    return false ;
                } else if( ! candidateNominated ){
                    r = xLower ;
                    findSolitaryCandidateInBox:
                    while( r < xUpper ){
                        c = yLower ;
                        while( c < yUpper ){
                            if( mask[r][c] == Nishio.POSSIBLE ){
                                x0 = r ;
                                y0 = c ;
                                r = xUpper ;
                                continue findSolitaryCandidateInBox ;
                            }
                            ++ c ;
                        }
                        ++ r ;
                    }
                    candidateNominated = true ;
                }
                ++ box ;
            }
        }             
        return true ;
    }
    
    boolean adjacentSectorPermutation( StringBuilder sb ){
        final CellState cellState = (CellState) lcc.state ;
        final NumberState numberState = (NumberState) lcn.state ;
        boolean anyMoveEliminated = false , boxElimination , isBlockShared ;
        int s , r , c , v , i , j , k , s1 , s2 , p1 , p2 , nValid , boxCount ;
        int dim , nBlocks , block , blockSize , r0 , rStep , c0 , cStep ;
        // Consider each dimension.
        dim = 0 ;
        while( dim < 2 && ! anyMoveEliminated ){
            // Set dimensions.
            if( dim == 0 ){
                // Row block.
                nBlocks = grid.boxesDown ;
                blockSize = grid.boxesAcross ;
            } else {
                // Column block
                nBlocks = grid.boxesAcross ;
                blockSize = grid.boxesDown ;
            }
            // Consider each block.
            s = 0 ;
            while( s < grid.cellsInRow && ! anyMoveEliminated ){
                // Permutate each row within the block.
                if( ( nSectorPerms[s] = permutateSector( dim * grid.cellsInRow + s , sectorPerms[s] , maxSectorPerms ) ) == maxSectorPerms ){
                    // Overflow - move on to the next block.
                    System.err.println("Permutation buffer is full with " + maxSectorPerms + " elements");
                }
                ++ s ;
            }
            // Pairwise comparison of sectors.
            s1 = 0 ;
            while( s1 < grid.cellsInRow - 1 && ! anyMoveEliminated ){
                if( nSectorPerms[s1] == maxSectorPerms ){
                    ++ s1 ;
                    continue ;
                }
                s2 = s1 + 1 ;
                while( s2 < grid.cellsInRow && ! anyMoveEliminated ){
                    if( nSectorPerms[s2] == maxSectorPerms ){
                        ++ s2 ;
                        continue ;
                    }
                    isBlockShared = s1 / blockSize == s2 / blockSize ;
                    // Reset counters.
                    nValid = 0 ;
                    v = 0 ;
                    while( v < grid.cellsInRow ){
                        i = 0 ;
                        while( i < grid.cellsInRow ){
                            sectorOffsetCount[0][v][i] = 0 ;
                            sectorOffsetCount[1][v][i] = 0 ;
                            ++ i ;
                        }
                        ++ v ;
                    }
                    // Determine which sector pairs are valid.
                    p1 = 0 ;
                    while( p1 < nSectorPerms[s1] ){
                        p2 = 0 ;
                        nextPerm:
                        while( p2 < nSectorPerms[s2] ){
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                if( isBlockShared && sectorPerms[s1][p1][v] / nBlocks == sectorPerms[s2][p2][v] / nBlocks ||
                                  ! isBlockShared && sectorPerms[s1][p1][v] == sectorPerms[s2][p2][v] ){
                                    ++ p2 ;
                                    continue nextPerm ;
                                }
                                ++ v ;
                            }
                            // The sector pair is valid, so update the stats.
                            v = 0 ;
                            while( v < grid.cellsInRow ){
                                ++ sectorOffsetCount[0][v][sectorPerms[s1][p1][v]];
                                ++ sectorOffsetCount[1][v][sectorPerms[s2][p2][v]];
                                ++ v ;
                            }
                            ++ nValid ;
                            ++ p2 ;
                        }
                        ++ p1 ;
                    }
                    // Look for values stored in a given position all the time or not at all.
                    v = 0 ;
                    while( v < grid.cellsInRow ){
                        i = 0 ;
                        while( i < grid.cellsInRow ){                                
                            if( sectorOffsetCount[0][v][i] == 0 && ! numberState.eliminated[v][dim*grid.cellsInRow+s1][i] ){
                                if( dim == 0 ){
                                    r = s1 ;
                                    c = i ;
                                } else {
                                    r = i ;
                                    c = s1 ;
                                }
                                sectorEliminateMove( sb , 
                                                     r , 
                                                     c , 
                                                     v ,
                                                     dim * grid.cellsInRow + s1 ,
                                                     dim * grid.cellsInRow + s2 ,
                                                     nValid ,
                                                     anyMoveEliminated ,
                                                     reportPerms );
                                anyMoveEliminated = true ;
                            } 
                            if( sectorOffsetCount[0][v][i] == nValid && numberState.nEliminated[v][dim*grid.cellsInRow+s1] < grid.cellsInRow - 1 ){
                                    if( dim == 0 ){
                                        r = s1 ;
                                        c = i ;
                                    } else {
                                        r = i ;
                                        c = s1 ;
                                    }
                                    sectorAddMove( sb , 
                                                   r , 
                                                   c , 
                                                   v ,
                                                   dim * grid.cellsInRow + s1 ,
                                                   dim * grid.cellsInRow + s2 ,
                                                   nValid ,
                                                   anyMoveEliminated ,
                                                   reportPerms );
                                    anyMoveEliminated = true ;
                            } 
                            if( sectorOffsetCount[1][v][i] == 0 && ! numberState.eliminated[v][dim*grid.cellsInRow+s2][i] ) {
                                if( dim == 0 ){
                                    r = s2 ;
                                    c = i ;
                                } else {
                                    r = i ;
                                    c = s2 ;
                                }
                                sectorEliminateMove( sb , 
                                                     r , 
                                                     c , 
                                                     v ,
                                                     dim * grid.cellsInRow + s1 ,
                                                     dim * grid.cellsInRow + s2 ,
                                                     nValid ,
                                                     anyMoveEliminated ,
                                                     reportPerms );
                                anyMoveEliminated = true ;
                            } 
                            if( sectorOffsetCount[1][v][i] == nValid && numberState.nEliminated[v][dim*grid.cellsInRow+s2] < grid.cellsInRow - 1 ){
                                if( dim == 0 ){
                                    r = s2 ;
                                    c = i ;
                                } else {
                                    r = i ;
                                    c = s2 ;
                                }
                                sectorAddMove( sb , 
                                               r , 
                                               c , 
                                               v ,
                                               dim * grid.cellsInRow + s1 ,
                                               dim * grid.cellsInRow + s2 ,
                                               nValid ,
                                               anyMoveEliminated ,
                                               reportPerms );
                                anyMoveEliminated = true ;
                            } 
                            // Look for values that are certain to appear in a given box.
                            if( isBlockShared ){
                                block = s1 / blockSize ;
                                j = 0 ;
                                while( j < blockSize ){
                                    boxCount = 0 ;
                                    k = 0 ;
                                    while( k < nBlocks ){
                                        boxCount += sectorOffsetCount[0][v][j*nBlocks+k];
                                        boxCount += sectorOffsetCount[1][v][j*nBlocks+k];
                                        ++ k ;
                                    }
                                    if( boxCount == nValid ){
                                        boxElimination = false ;
                                        if( dim == 0 ){
                                            r0    = block * blockSize ;
                                            rStep = blockSize ;
                                            c0    = j * nBlocks ;
                                            cStep = nBlocks ;
                                        } else {
                                            r0    = j * nBlocks ;
                                            rStep = nBlocks ;
                                            c0    = block * blockSize ;
                                            cStep = blockSize ;
                                        }
                                        r = r0 ;
                                        while( r < r0 + rStep ){
                                            if( dim == 0 && ( r == s1 || r == s2 ) ){
                                                ++ r ;
                                                continue ;
                                            }
                                            c = c0 ;
                                            while( c < c0 + cStep ){
                                                if( dim == 1 && ( c == s1 || c == s2 ) ){
                                                    ++ c ;
                                                    continue ;
                                                }
                                                if( ! cellState.eliminated[r][c][v] ){
                                                    eliminateMove( r , c , v );
                                                    boxElimination = true ;
                                                    ++ adjacentSectorPermutationEliminations ;
                                                }
                                                ++ c ;
                                            }                                        
                                            ++ r ;
                                        }
                                        if( boxElimination ){
                                            if( explain ){
                                                if( ! anyMoveEliminated ){
                                                    appendAdjacentSectorPermutationHeader( sb , dim * grid.cellsInRow + s1 , dim * grid.cellsInRow + s2 , nValid , reportPerms );
                                                }
                                                sb.append("In each combination, the value ");
                                                SuDokuUtils.appendValue( sb , v );
                                                sb.append(" in ");
                                                SuDokuUtils.appendBox( sb , blockSize , /*dim * grid.cellsInRow + */ block * blockSize + j );
                                                sb.append(" appears in ");
                                                SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , dim * grid.cellsInRow + s1 );
                                                sb.append(" or ");
                                                SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , dim * grid.cellsInRow + s2 );
                                                sb.append(".\n");                                                   
                                            }
                                            anyMoveEliminated = true ;                                                                            
                                        }
                                    }
                                    ++ j ;                                   
                                
                            }
                            }                                                            
                            ++ i ;
                        }
                        ++ v ;
                    }                                                                           
                    ++ s2 ;
                }
                ++ s1 ;
            }
            ++ dim ;
        }
        
        return anyMoveEliminated ;
    }
    
    int permutateSector( int s , byte[][] perms , int maxPerms ){
        final NumberState numberState = (NumberState) lcn.state ;
        int i , j , v , nPerms ;
        nPerms = 0 ;
        perms[nPerms][0] = 0 ;
        // Permutate
        v = 0 ;
        while( true ){
            // Place the current value in a new slot.
            // Find a candidate slot that isn't occupied by a lower value.
            i = perms[nPerms][v];
            findCandidateSlot:
            while( i < grid.cellsInRow ){
                // Check the slot's a candidate.
                if( numberState.eliminated[v][s][i] ){
                    ++ i ;
                    continue ;
                }
                // Check the slot isn't occupied by a lower value.
                j = 0 ;
                while( j < v ){
                    if( perms[nPerms][j] == i ){
                        ++ i ;
                        continue findCandidateSlot ;
                    }
                    ++ j ;
                }
                // Put the value into the slot and consider the next value.
                perms[nPerms][v++] = (byte) i ;
                if( v < grid.cellsInRow ){
                    perms[nPerms][v] = 0 ;                        
                }
                break ;
            }    
            if( i == grid.cellsInRow ){
                // When no slot is available, revert to the previous value.
                if( v > 0 ){
                    ++ perms[nPerms][--v];
                } else {
                    break ;
                }
            }
            if( v == grid.cellsInRow ){
                // Set up the next permutation.
                if( ++ nPerms < maxPerms ){
                    i = 0 ;
                    while( i < grid.cellsInRow ){
                        perms[nPerms][i] = perms[nPerms-1][i];
                        ++ i ;
                    }
                    ++ perms[nPerms][--v];
                } else {
                    break ;
                }
            }
        }
        
        return nPerms ;
    }

    StringBuilder sectorEliminateMove( StringBuilder sb , 
                                       int r , 
                                       int c , 
                                       int v ,
                                       int s1 ,
                                       int s2 ,
                                       int nCombos ,
                                       boolean headerAdded ,
                                       boolean listPerms ){
        if( explain ){
            if( ! headerAdded ){
                appendAdjacentSectorPermutationHeader( sb , s1 , s2 , nCombos , listPerms );
            }
            sb.append("No combination has the value ");
            SuDokuUtils.appendValue( sb , v );
            sb.append(" as a candidate for the cell ");
            SuDokuUtils.appendCell( sb , r , c );
            sb.append(".\n");
        }
        eliminateMove( r , c , v );
        ++ adjacentSectorPermutationEliminations ;
        
        return sb ;
    }
    
    StringBuilder sectorAddMove( StringBuilder sb , 
                                 int r , 
                                 int c , 
                                 int v ,
                                 int s1 , 
                                 int s2 ,
                                 int nCombos ,
                                 boolean headerAdded ,
                                 boolean listPerms ){
        if( explain ){
            if( ! headerAdded ){
                appendAdjacentSectorPermutationHeader( sb , s1 , s2 , nCombos , listPerms );
            }
            sb.append("Each combination has the value ");
            SuDokuUtils.appendValue( sb , v );
            sb.append(" as a candidate for the cell ");
            SuDokuUtils.appendCell( sb , r , c );
            sb.append(".\n");
        }
        adjacentSectorPermutationEliminations += addMove( r , c , v );

        return sb ;
    }

    StringBuilder appendAdjacentSectorPermutationHeader( StringBuilder sb ,
                                                         int s1 ,
                                                         int s2 ,
                                                         int nCombos ,
                                                         boolean listPerms ){
        int i ;
        
        sb.append("The ");
        sb.append( nSectorPerms[s1%grid.cellsInRow] );
        sb.append(" permutations of ");
        SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s1 );
        sb.append(" and ");
        sb.append( nSectorPerms[s2%grid.cellsInRow] );
        sb.append(" permutations of ");
        SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s2 );
        sb.append(" combine legally in ");
        sb.append( nCombos );
        sb.append(" different ways.\n");
        if( listPerms ){
            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s1 );
            sb.append(":\n");
            i = 0 ;
            while( i < nSectorPerms[s1%grid.cellsInRow] ){
                appendPerm( sb , sectorPerms[s1%grid.cellsInRow][i++] );
                sb.append('\n');
            }
            SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , s2 );
            sb.append(":\n");
            i = 0 ;
            while( i < nSectorPerms[s2%grid.cellsInRow] ){
                appendPerm( sb , sectorPerms[s2%grid.cellsInRow][i++] );
                sb.append('\n');
            }            
        }        
        return sb ;
    }
    
    StringBuilder appendPerm( StringBuilder sb ,
                              byte[] perm ){
        int v , offset ;
        offset = 0 ;
        while( offset < grid.cellsInRow ){
            v = 0 ;
            while( perm[v] != offset ){
                ++ v ;
            }
            SuDokuUtils.appendValue( sb , v );
            if( offset < grid.cellsInRow - 1 ){
                sb.append('-');
            }
            ++ offset ;
        }
        return sb ;
    }
    
    /** 
     * Updates state variables.
	 * @see com.act365.sudoku.IStrategy#updateState(int,int,int,String,boolean)
	 */
    
	@Override public boolean updateState(int x , int y , int value , String reason , boolean writeState ) throws Exception {
        if( nMoves == -1 ){
            return false ;
        }
        // Store current state variables on thread.
        if( writeState && ( updateInvulnerableState || updateLinearSystemState ) ){
            if( updateInvulnerableState ){
                invulnerableState.pushState( nMoves );
            }
            if( updateLinearSystemState ){
                linearSystemState.pushState( nMoves ); 
            }
            stateWrite[nMoves] = true ;
        } else {
            stateWrite[nMoves] = false ;
        }        
        // Store move to thread
        xMoves[nMoves] = x ;
        yMoves[nMoves] = y ;
        values[nMoves] = value - 1 ;
        if( explain ){
            reasons[nMoves].append( reason );
        }
        ++ nMoves ;
        // Update state variables
        if( updateInvulnerableState ){
            invulnerableState.addMove( x , y , value - 1 );
        }
        if( updateLinearSystemState ){
            linearSystemState.addMove( x , y , value - 1 );
        }        
        // Underlying state variables
		lcn.updateState( x , y , value , reason , writeState );
        lcc.updateState( x , y , value , reason , writeState );
        return true ;
	}

	/**
     * Unwind the stack.
	 * @see com.act365.sudoku.IStrategy#unwind(int,boolean,boolean)
	 */
    
	@Override public boolean unwind( int newNMoves , boolean reset , boolean eliminate ){
        if( newNMoves < 0 ){
            return false ;
        }
        // Unwind thread.
        if( explain && reset ){
            reasons[newNMoves].append("The move ");
            SuDokuUtils.appendMove( reasons[newNMoves] , xMoves[newNMoves] , yMoves[newNMoves] , grid.data[xMoves[newNMoves]][yMoves[newNMoves]] - 1 );
            reasons[newNMoves].append(" leads to a contradiction.\n");
            int i = newNMoves + 1 ;
            while( i < nMoves ){
                reasons[i++] = new StringBuilder();
            }
        }
        if( updateInvulnerableState ){
            invulnerableState.popState( newNMoves );
            if( eliminate ){
                invulnerableState.eliminateMove( xMoves[newNMoves] , yMoves[newNMoves] , grid.data[xMoves[newNMoves]][yMoves[newNMoves]] - 1 );
            }
        }
        if( updateLinearSystemState ){
            linearSystemState.popState( newNMoves );
            if( eliminate ){
                linearSystemState.eliminateMove( xMoves[newNMoves] , yMoves[newNMoves] , grid.data[xMoves[newNMoves]][yMoves[newNMoves]] - 1 );
            }
        }
		lcn.unwind( newNMoves , false , eliminate );
        lcc.unwind( newNMoves , false , eliminate );
        // Remove the most recent moves from the grid.
        if( reset ){
            int i = newNMoves ;
            while( i < nMoves ){
                grid.data[xMoves[i]][yMoves[i]] = 0 ;
                ++ i ;
            }
        }
        nMoves = newNMoves ;
        return true ;
	}
    
    /**
     * Determines the last move for which two or more alternatives existed.
     */
    
    @Override public int getLastWrittenMove(){
        int lcnMove = lcn.getLastWrittenMove() ,
            lccMove = lcc.getLastWrittenMove() ;
            
        if( lcnMove < lccMove ){
            return lcnMove ; 
        }
        return lccMove ;
    }
    
    /**
     * Prints the current state grid of the given type.
     */
    
    public String printState( int stateType ){
        
        switch( stateType ){            
            case SuDokuUtils.CELL_STATE :
                return lcc.state.toString();
            case SuDokuUtils.NUMBER_STATE :
                return lcn.state.toString();
            case SuDokuUtils.NEIGHBOUR_STATE :
                return invulnerableState.toString();
            default:
                return new String();
        }
    }
    
    /**
     * Dumps the thread to the given output stream.
     */
    
    @Override public String toString(){
        StringBuilder sb = new StringBuilder();
        int i = 0 ;
        while( i < nMoves ){
            sb.append( ( 1 + i ) + ". ");
            SuDokuUtils.appendMove( sb , xMoves[i] , yMoves[i] , grid.data[xMoves[i]][yMoves[i]] - 1 );
            sb.append('\n');
            ++ i ;
        }  
        sb.append("\n");      
        sb.append("Cell State:\n");
        sb.append( lcc.state.toString() );
        sb.append("Number State:\n");
        sb.append( lcn.state.toString() );
        sb.append("Neighbourhood State:\n");
        sb.append( invulnerableState.toString() );
        if( linearSystemState != null ){
            sb.append("Linear System State:\n");
            sb.append( linearSystemState.toString() );            
        }
        
        return sb.toString(); 
    }  
}
