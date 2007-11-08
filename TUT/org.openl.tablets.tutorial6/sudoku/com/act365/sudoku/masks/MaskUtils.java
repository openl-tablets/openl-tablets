/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com August 2005
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

package com.act365.sudoku.masks ;

import com.act365.sudoku.MaskFactory ;

/**
 * MaskUtils provides utilities to manipulate MaskFactory subclasses.
 */

public class MaskUtils {

    public final static int ROTATE_2     = 0 ,
                            ROTATE_4     = 1 ,
                            REFLECT_2    = 2 ,
                            REFLECT_4    = 3 ,
                            DIAGONAL_2   = 4 ,
                            DIAGONAL_4   = 5 ,
                            REFLECT_8    = 6 ,
                            TRANSLATE    = 7 ,
                            ASYMMETRIC   = 8 ,
                            USER_DEFINED = 9 ;
                            
    public final static String[] longLabels = { "Rotational Order 2" ,
                                                "Rotational Order 4" ,
                                                "Vertical Mirror" ,
                                                "Vertical & Horizontal Mirror" ,
                                                "Diagonal Mirror" ,
                                                "Double Diagonal Mirror" ,
                                                "All Mirrors" ,
                                                "Translational" ,
                                                "Asymmetric" ,
                                                "User-Defined" };
    
    public final static String[] shortLabels = { "Rotate2" ,
                                                 "Rotate4" ,
                                                 "VMirror" ,
                                                 "VHMirror" ,
                                                 "DMirror" ,
                                                 "D2Mirror" ,
                                                 "AllMirrors" ,
                                                 "Translate" ,
                                                 "None" ,
                                                 "User" };                                         

    public static MaskFactory createMaskFactory( int type ,
                                                 int cellsInRow ,
                                                 int boxesAcross ,
                                                 boolean[][] mask ) throws Exception {
    
        MaskFactory factory = null ;
        
        switch( type ){
            case ROTATE_2 :
                factory = new Rotate2( cellsInRow );
                break ;
            case ROTATE_4 :
                factory = new Rotate4( cellsInRow );
                break ;
            case REFLECT_2 :
                factory = new Reflect2( cellsInRow );
                break ;
            case REFLECT_4 :
                factory = new Reflect4( cellsInRow );
                break ;
            case DIAGONAL_2 :
                factory = new ReflectDiagonal2( cellsInRow );
                break ;
            case DIAGONAL_4 :
                factory = new ReflectDiagonal4( cellsInRow );
                break ;
            case REFLECT_8 :
                factory = new Reflect8( cellsInRow );
                break ;
            case TRANSLATE :
                if( boxesAcross == 0 ){
                    throw new Exception("The number of boxes across a row must be given for the Translational Symmetry mask type");
                }
                factory = new Translational( boxesAcross , cellsInRow / boxesAcross );
                break ;
            case ASYMMETRIC :
                factory = new Asymmetric( cellsInRow );
                break ;
            case USER_DEFINED :
                if( mask == null ){
                    throw new Exception("A mask must be supplied for the User-Defined mask type");
                }
                factory = new UserDefined( mask );
                break ;
            default:
                throw new Exception("Unsupported Mask type");
        }
        
        return factory ;
    } 

    public static int getMaskType( String shortLabel ) throws Exception {
        int i = 0 ;
        while( i < shortLabels.length ){
            if( shortLabel.equalsIgnoreCase( shortLabels[i] ) ){
                break ;
            }
            ++ i ;
        }
        if( i == shortLabels.length ){
            throw new Exception("Unsupported Mask type");
        }
        return i ;
    }
}
