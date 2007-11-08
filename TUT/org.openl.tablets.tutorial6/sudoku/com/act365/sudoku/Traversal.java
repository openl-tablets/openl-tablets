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

/**
 * A Traversal object is used to store the moves made during
 * an attempt to solve a Su Doku puzzle. All moves are recorded,
 * even those that are subsequently unwound. The object isn't
 * used in anger anywhere throughout the project but it
 * provides useful debug.
 */

public class Traversal {

    int nMoves , currentDepth ;
    
    int[] depth , x , y , z ;
    
    /**
     * 
     * @param capacity
     */
    
    public Traversal( int capacity ){
        x = new int[capacity];
        y = new int[capacity];
        z = new int[capacity];
        depth = new int[capacity];
        nMoves = currentDepth = 0 ;
    }
    
    /**
     * Adds the move represented by the state trio (mx,my,mz) to
     * the traversal. The trio could represent the move (mx,my):=mz,
     * however, e.g. in the case of Least Candidates Number, it might
     * have another meaning. The Traversal doesn't care.
     */

    public boolean addMove( int mx , int my , int mz ) {
        if( nMoves == x.length ){
            System.out.println("Traversal overload " + nMoves );
            return false ;
        }
        // Store the move.   
        x[nMoves] = mx ;
        y[nMoves] = my ;
        z[nMoves] = mz ;
        depth[nMoves++] = currentDepth ++ ;
        return true ;
    }

    /**
     * Unwinds the last move from the traversal. (Of course, the move
     * is still recorded, but the traversal progresses as though the
     * move weren't there.)
     */
    
    public boolean unwind(){
        if( currentDepth == 0 || nMoves == x.length ){
            return false ;
        }
        -- currentDepth ;
        return true ;
    }

    /**
     * Looks for the sequence of moves (findX,findY,findZ) of length nFindMoves
     * within the traversal between the position start and end. A match is
     * considered to have a occurred if some non-trivial subset of the moves 
     * is found. The returned value is the end position where the match
     * occurred. When no match is found, -1 will be returned.  
     */

    public int find( int start , int end , int[] findX , int[] findY , int[] findZ , int nFindMoves ){
        int d , s , e = nextEnd( start );
        while( e < end ){
            if( ( d = match( start , e , findX , findY , findZ , nFindMoves ) ) == 1 + depth[e-1] ){
                return e ;
            } 
            s = nextStart( e , d );
            e = nextEnd( s );
        }
        return -1 ;
    }

    /**
     * Looks for the sequence of moves (findX,findY,findZ) of length nFindMoves
     * anywhere within the traversal. A match is considered to have a occurred 
     * if some non-trivial subset of the moves is found. The returned value is 
     * the end position where the match occurred. When no match is found, -1 
     * will be returned.  
     */

    public int find( int[] findX , int[] findY , int[] findZ , int nFindMoves ){
        return find( 0 , nMoves , findX , findY , findZ , nFindMoves );
    }
    
    /**
     * Looks for the next start point, i.e. the next point at which a move
     * is made after a sequence of unwinds, after the given position within
     * the traversal. When startDepth is specified, only start points with
     * that depth will be considered. The value nMoves will be returned if
     * the end of the traversal has been found. 
     */
    
    int nextStart( int position , int startDepth ){
        int start = position ;
        while( ++ start < nMoves ){
            if( depth[start] == startDepth ){
                break ;
            }
        }
        return start ;
    }
    
    /**
     * Looks for the next end point, i.e. the next point at which an 
     * unwind occurs, after the given position within the traversal. 
     * The value nMoves will be returned if the end of the traversal 
     * has been found. 
     */
    
    int nextEnd( int position ){
        int end = position ;
        while( ++ end < nMoves ){
            if(  depth[end] != depth[end-1] + 1 ){
                 break;
            }
        }
        return end ;
    }
    
    /**
     * Determines at which depth the segment of the traversal [start,end}
     * differs from the given sequence of moves. When the segment matches
     * the sequence, -1 is returned. 
     */
        
    int match( int start , int end , int[] matchX , int[] matchY , int[] matchZ , int nMatchMoves ){
        int i , d = end > 0 ? 1 + depth[end-1] : 0 , lowestMismatchDepth ;
        if( d > nMatchMoves ){
            return nMatchMoves ;
        }
        lowestMismatchDepth = d ;
        while( -- end >= start ){
            if( depth[end] >= d ){
                continue;
            }
            -- d ;
            i = 0 ;
            while( i < nMatchMoves ){
                if( x[end] == matchX[i] && y[end] == matchY[i] && z[end] == matchZ[i] ){
                    break;
                }
                ++ i ;
            }
            if( i == nMatchMoves ){
                lowestMismatchDepth = d ;
            }
        }        
        return lowestMismatchDepth ;
    }
    
    /**
     * Resets the traversal.
     */
    
    public void reset(){
        nMoves = currentDepth = 0 ;
    }
    
    /**
     * Returns the current size.
     */
    
    public int size(){
        return nMoves ;
    }
    
    /**
     * Returns the current size.
     */
    
    public int depth(){
        return currentDepth ;
    }
    
    /**
     * Returns the traversal capacity.
     */
    
    public int capacity(){
        return x.length ;
    }
    
    /**
     * Returns a string representation of the traversal that lists
     * each move made.
     */
    
    public String toString(){
        return toString( 0 , nMoves );
    }
    
    /**
     * Returns a string represeantation of a traversal segment.
     */

    String toString( int start , int end ){
        StringBuilder sb = new StringBuilder();
        int d = end > 0 ? 1 + depth[end-1] : 0 ;
        while( -- end >= start ){
            if( depth[end] >= d ){
                continue;
            }
            sb.append( end + ". [" + depth[end] + "](" + x[end] + "," + y[end] + "," + z[end] + ")\n");
            -- d ;
        }
        return sb.toString();    
    }    
}
