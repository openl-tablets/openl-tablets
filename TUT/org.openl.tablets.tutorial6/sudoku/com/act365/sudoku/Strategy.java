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

/**
 * Provides a single point of access to the various strategy classes.
 */

public class Strategy {

	public final static int FIRST_AVAILABLE = 0,
                            LEAST_CANDIDATES_CELL = 1,
                            RANDOM_LEAST_CANDIDATES_CELL = 2,
                            LEAST_CANDIDATES_NUMBER = 3 ,
                            RANDOM_LEAST_CANDIDATES_NUMBER = 4 ,
                            LEAST_CANDIDATES_HYBRID = 5 ,
                            RANDOM_LEAST_CANDIDATES_HYBRID = 6 ,
                            LEAST_CANDIDATES_HYBRID_II = 7 ,
                            RANDOM_LEAST_CANDIDATES_HYBRID_II = 8 ,
                            MOST_CANDIDATES = 9 ,
                            RANDOM_MOST_CANDIDATES = 10 ;

	public final static String[] strategyNames = new String[] { "First Available", 
                                                                "Least Candidates Cell" ,
                                                                "Random Least Candidates Cell" ,
                                                                "Least Candidates Number" ,
                                                                "Random Least Candidates Number" ,
                                                                "Least Candidates Hybrid" ,
                                                                "Random Least Candidates Hybrid" ,
                                                                "Least Candidates Hybrid II" ,
                                                                "Random Least Candidates Hybrid II" ,
                                                                "Most Candidates" ,
                                                                "Random Most Candidates" };
    
    /**
     * Creates a new strategy instance to solve the given grid.
     */

    public static IStrategy create( int strategy , boolean explain ){
        
        switch( strategy ){
            case FIRST_AVAILABLE:
            return new FirstAvailable();
            
            case LEAST_CANDIDATES_CELL:
            return new LeastCandidatesCell( false , explain );
            
            case RANDOM_LEAST_CANDIDATES_CELL :
            return new LeastCandidatesCell( true , explain );
            
            case LEAST_CANDIDATES_NUMBER:
            return new LeastCandidatesNumber( false , explain );
            
            case RANDOM_LEAST_CANDIDATES_NUMBER :
            return new LeastCandidatesNumber( true , explain );
            
            case LEAST_CANDIDATES_HYBRID:
            return new LeastCandidatesHybrid( false , false , false , false , explain );
            
            case RANDOM_LEAST_CANDIDATES_HYBRID :
            return new LeastCandidatesHybrid( true , false , false , false , explain );
            
            case LEAST_CANDIDATES_HYBRID_II:
            return new LeastCandidatesHybrid( false , true , false , true , explain );
            
            case RANDOM_LEAST_CANDIDATES_HYBRID_II :
            return new LeastCandidatesHybrid( true , true , false , true , explain );
            
            case MOST_CANDIDATES :
            return new MostCandidates( null , false );
            
            case RANDOM_MOST_CANDIDATES :
            return new MostCandidates( null , true );
                      
            default:
            return null ;
        }
    }
    
    public static IStrategy create( String strategy , boolean explain ){
        int i = 0 ;
        while( i < strategyNames.length ){
            if( strategy.equalsIgnoreCase( strategyNames[i] ) ){
                return create( i , explain );
            }
            ++ i ;
        }
        return null ;
    }
}
