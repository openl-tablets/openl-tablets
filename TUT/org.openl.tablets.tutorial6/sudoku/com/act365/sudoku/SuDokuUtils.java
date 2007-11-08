/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com March 2005
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

import java.text.DateFormat ;
import java.util.Date ;
import java.util.StringTokenizer ;

/**
 * SuDokuUtils contains utility functions that are called from several other classes.
 */

public class SuDokuUtils {

    /**
     * Controls whether integers greater than or equal to 10 are displayed 
     * as numbers or letters.
     */
    
    public enum ValueFormat { NUMERIC { @Override public String toString() { return "Numeric from 1" ;} } , 
                              ALPHANUMERIC_0 { @Override public String toString() { return "Alphanumeric from 0" ;} } , 
                              ALPHANUMERIC_1 { @Override public String toString() { return "Alphanumeric from 1" ;} } , 
                              TEXT { @Override public String toString() { return "Text" ; } } };
    
    /**
     * The default display format for values greater than or equal to 10.
     */
    
    public static ValueFormat defaultValueFormat = ValueFormat.ALPHANUMERIC_1 ;

    /**
     * How the reasoning should be written.
     */
    
    public enum ReasoningFormat { MATRIX { @Override public String toString() { return "Matrix" ;} } ,
                                  FORUM {@Override public String toString() { return "Forum" ;} } };
    
    
    public static ReasoningFormat defaultReasoningFormat = ReasoningFormat.MATRIX ;
    
    // Copy types
    
    public final static int PLAIN_TEXT          = 0 ,
                            LIBRARY_BOOK        = 1 ,
                            CELL_STATE          = 2 ,
                            NUMBER_STATE        = 3 ,
                            NEIGHBOUR_STATE     = 4 ;
    
    public final static String[] copyTypes = { "Plain Text Puzzle", 
                                               "Library Book Puzzle" , 
                                               "Cell State" ,
                                               "Number State" ,
                                               "Neighbour State" };
    
    public static int defaultCopyType = PLAIN_TEXT ;
    
    public final static String[] featuredGrades = {"Ungraded"};
    
    /**
     * Sets the text string to be used in TEXT mode.
     */

    public static void setText( String newText , int cellsInRow ) throws Exception {
        if( newText.length() != cellsInRow ){
            throw new Exception("The text string should contain as many characters as there are cells in a row");                                
        }
        int i , j ;
        text = new char[cellsInRow];
        i = 0 ;
        while( i < cellsInRow ){
            text[i] = newText.charAt( i );
            ++ i ;
        }
        i = 0 ;
        while( i < cellsInRow - 1 ){
            j = i + 1 ;
            while( j < cellsInRow ){
                if( text[i] == text[j] ){
                    throw new Exception("The text string contains duplicate characters");
                }
                ++ j ;
            }
            ++ i ;
        }
    }
    
    /**
     * Sets a default text string.
     */

    public static void setDefaultText( int cellsInRow ){
        text = new char[cellsInRow];
        int i = 0 ;
        while( i < cellsInRow ){
            text[i] = (char)( 'A' + i );
            ++ i ;
        }
    }

    static char[] text = new char[0];
    
    /**
     * Creates a string representation of a two-dimensional integer array.
     * @param maxDatum maximum permitted value in the data array
     * @param format format for numbers greater than or equal to 10
     */

    public static String toString( byte[][] data , int boxesAcross , int maxDatum , ValueFormat format ){
        
        final int cellsInRow = data.length ,
                  boxesDown = data.length / boxesAcross ;
                  
        StringBuilder sb = new StringBuilder();
        
        int i , j , k ;
        int number = maxDatum , fieldWidth = 1 , numberWidth , boxWidth ;
        if( format == ValueFormat.NUMERIC ){
            while( ( number /= 10 ) >= 1 ){
                ++ fieldWidth ;
            }
        }
        boxWidth = cellsInRow / boxesAcross *( fieldWidth + 1 ) + 2 ;
        i = 0 ;
        while( i < cellsInRow ){
            if( i > 0 && i % boxesAcross == 0 ){
                k = 0 ;
                while( k < ( fieldWidth + 1 )* cellsInRow + ( boxesAcross - 1 )* 2 ){
                    if( k % boxWidth == boxWidth - 1 ){
                        sb.append("+");
                    } else {
                        sb.append("-");
                    }
                    ++ k ;
                }
                sb.append(" \n");
            }
            j = 0 ;
            while( j < cellsInRow ){
                if( j > 0 && j % boxesDown == 0 ){
                    sb.append(" |");
                }
                k = 0 ;
                if( data[i][j] > 0 ){
                    numberWidth = 1 ;
                    number = data[i][j];
                    if( format == ValueFormat.NUMERIC ){
                        while( ( number /= 10 ) >= 1 ){
                            ++ numberWidth ;
                        }
                    }
                    while( k < 1 + fieldWidth - numberWidth ){
                        sb.append(" ");
                        ++ k ;
                    }
                    switch( format ){
                        case NUMERIC:
                            sb.append( data[i][j] );
                            break;
                        case ALPHANUMERIC_0:
                            if( data[i][j] > 10 ){
                                sb.append( (char)( 'A' + data[i][j] - 11 ) );
                            } else {
                                sb.append( data[i][j] - 1 );
                            }
                            break;
                        case ALPHANUMERIC_1:
                            if( data[i][j] >= 10 ){
                                sb.append( (char)( 'A' + data[i][j] - 10 ) );
                            } else {
                                sb.append( data[i][j] );
                            }
                            break;
                        case TEXT:
                            sb.append( text[data[i][j]-1] );
                            break;
                    }
                } else {
                    sb.append(" ");
                    while( k < fieldWidth ){
                        sb.append(".");
                        ++ k ;
                    }
                }
                ++ j ;
            }
            sb.append(" \n");
            ++ i ;
        }
        
        return sb.toString();
    }

    /**
     * Creates a string representation of a two-dimensional integer array.
     * @param maxDatum maximum permitted value in the data array
     */

    public static String toString( byte[][] data , int boxesAcross , int maxDatum ){
        return toString( data , boxesAcross , maxDatum , defaultValueFormat );
    }    

    /**
     * Creates a string representation of a two-dimensional integer array
     * with a maximum value equal to the grid size.
     */

    public static String toString( byte[][] data , int boxesAcross ){
        return toString( data , boxesAcross , data.length , defaultValueFormat );
    }
    
    /**
     * Creates a string representation of a two-dimensional string array.
     * @param maxLength length of the longest string in the array
     */

    public static String toString( String[][] data , int boxesAcross , int[] maxLength ){
        
        if( boxesAcross == 0 ){
            return new String();
        }
        
        final int cellsInRow = data.length ,
                  boxesDown = data.length / boxesAcross ;
                  
        StringBuilder sb = new StringBuilder();
        
        int i , j , k , length ;
        i = 0 ;
        while( i < cellsInRow ){
            if( i > 0 && i % boxesAcross == 0 ){
                j = 0 ;
                while( j < cellsInRow ){
                    k = 0 ;
                    while( k < maxLength[j] + 2 ){
                        sb.append("-");
                        ++ k ;
                    }
                    if( ++ j < cellsInRow && j % boxesDown == 0 ){
                        sb.append("-+");
                    }
                }
                sb.append(" \n");
            }
            j = 0 ;
            while( j < cellsInRow ){
                k = 0 ;
                if( ( length = data[i][j].length() ) > 0 ){
                    while( k < 2 + maxLength[j] - length ){
                        sb.append(" ");
                        ++ k ;
                    }
                    sb.append( data[i][j] );
                } else {
                    sb.append("  ");
                    while( k < maxLength[j] ){
                        sb.append(".");
                        ++ k ;
                    }
                }
                if( ++ j < cellsInRow && j % boxesDown == 0 ){
                    sb.append(" |");
                }
            }
            sb.append(" \n");
            ++ i ;
        }
        
        return sb.toString();
    }
        
    /**
     * Populates a data array according to a string in the given format.
     */

    public static void populate( byte[][] data , String s , ValueFormat format ){   
        String token , whitespace = " \t\n\r*|¦-+";
        StringTokenizer st = new StringTokenizer( s , whitespace );
        int i , j , cursor = 0 ;
        char c ;
        i = 0 ;
        while( i < data.length ){
            j = 0 ;
            while( j < data[0].length ){
                if( data.length >= 10 ){
                    token = st.nextToken();
                } else {
                    c = s.charAt( cursor ++ );
                    while( whitespace.indexOf( c ) != -1 ){
                        c = s.charAt( cursor ++ );
                    }
                    token = Character.toString( c );
                }
                data[i][j++] = parse( token , format );
            }
            ++ i ;
        }
    }

    /**
     * Populates a data array according to a string in the default format.
     */

    public static void populate( byte[][] data , String s ){
        populate( data , s , defaultValueFormat );
    }        
    
    /**
     * Converts a string representation of a cell in the given format 
     * into a data value.
     */
    
    public static byte parse( String s , ValueFormat format ){
        byte datum = 0 ;
        char c ;
        switch( format ){
        case NUMERIC:
            try {
                datum = Byte.parseByte( s );
            } catch( NumberFormatException e ) {
            }
            break;
        case ALPHANUMERIC_0:  
            if( s.length() == 1 ){
                c = s.charAt( 0 );
                if( c >= 'A' && c <= 'Z' ){
                    datum = (byte)( c - 'A' + 11 );
                    break ;
                } else if( c >= 'a' && c <= 'z' ){
                    datum = (byte)( c - 'a' + 11 );
                    break ;
                }
            }
            try {
                datum = (byte)( 1 + Byte.parseByte( s ) );
            } catch( NumberFormatException e ) {
            }
            break;
        case ALPHANUMERIC_1:  
            if( s.length() == 1 ){
                c = s.charAt( 0 );
                if( c >= 'A' && c <= 'Z' ){
                    datum = (byte)( c - 'A' + 10 );
                    break ;
                } else if( c >= 'a' && c <= 'z' ){
                    datum = (byte)( c - 'a' + 10 );
                    break ;
                }
            }
            try {
                datum = Byte.parseByte( s );
            } catch( NumberFormatException e ) {
            }
            break;
        case TEXT:
            if( s.length() == 1 ){
                int i = 0 ;
                while( i < text.length ){
                    if( text[i] == s.charAt( 0 ) ){
                        datum = (byte)( i + 1 );
                    }
                    ++ i ;
                }
            }
        }
        return datum ;
    }
    
    /**
     * Converts a string representation of a cell in the default format 
     * into a data value.
     */
    
    public static byte parse( String s ){
        return parse( s , defaultValueFormat );
    }
    
    /**
     * Writes out the XML header for Pappocom library books.
     */

    public static String libraryBookHeader( String className ,
                                            int cellsInRow ,
                                            int boxesAcross ,
                                            String[] featuredGrades ){
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        sb.append("<sudoku-book>\n");
        sb.append("<note>Generated by ");
        sb.append( className );
        sb.append(" on ");
        sb.append( DateFormat.getDateTimeInstance().format( new Date() ) );
        sb.append(".</note>\n");
        sb.append("<user>0</user>\n");
        sb.append("<last>000000000000</last>\n");
        sb.append("<checked>000000000000</checked>\n");
        sb.append("<xtra>0</xtra>\n");            
        sb.append("<puzzle-type>");
        sb.append( cellsInRow == 9 && boxesAcross == 3 ? "0" : "1" );
        sb.append("</puzzle-type>\n");
        sb.append("<cells-in-row>");
        sb.append( cellsInRow );
        sb.append("</cells-in-row>\n");
        sb.append("<boxes-across>");
        sb.append( boxesAcross );
        sb.append("</boxes-across>\n");
        sb.append("<boxes-down>");
        sb.append( cellsInRow / boxesAcross );
        sb.append("</boxes-down>\n");
            
        int i = 0 ;
        while( i < featuredGrades.length ){
            sb.append("<featuredGrade>");
            sb.append( featuredGrades[i] );
            sb.append("</featuredGrade>\n");
            ++ i ;
        }
        
        return sb.toString();
    }
    
    /**
     * Writes out the XML header for Pappocom library books.
     */

    public static String libraryBookFooter() {
        return "</sudoku-book>\n" ;
    }
    
    public static StringBuilder appendCell( StringBuilder sb , int r , int c , ReasoningFormat format ){
        switch( format ){
        case MATRIX :
            sb.append('(');
            sb.append( 1 + r );
            sb.append(',');
            sb.append( 1 + c );
            sb.append(')');
            break;
        case FORUM :
            sb.append('r');
            sb.append( 1 + r );
            sb.append('c');
            sb.append( 1 + c );
            break;
        default:
            assert false ;
        }
        return sb ;
    }
    
    public static StringBuilder appendCell( StringBuilder sb , int r , int c ){
        return appendCell( sb , r , c , defaultReasoningFormat );
    }
    
    public static StringBuilder appendSector( StringBuilder sb , int cellsInRow , int boxesAcross , int sector , ReasoningFormat format ){
        switch( format ){
        case MATRIX :
            if( sector < cellsInRow ){
                sb.append("Row ");
                sb.append( 1 + sector );
            } else if( sector < 2 * cellsInRow ) {
                sb.append("Column ");
                sb.append( 1 + sector % cellsInRow );
            } else {
                sb.append("Box [");
                sb.append( 1 + sector % cellsInRow / boxesAcross );
                sb.append(',');
                sb.append( 1 + sector % cellsInRow % boxesAcross );
                sb.append(']');
            }
            break ;
        case FORUM :
            if( sector < cellsInRow ){
                sb.append("Row ");
            } else if( sector < 2 * cellsInRow ){
                sb.append("Column ");
            } else {
                sb.append("Box ");
            }
            sb.append( 1 + sector % cellsInRow );
            break ;
        default:
            assert false ;
        }
        return sb ;
    }
    
    public static StringBuilder appendSector( StringBuilder sb , int cellsInRow , int boxesAcross , int sector ){
        return appendSector( sb , cellsInRow , boxesAcross , sector , defaultReasoningFormat );
    }
    
    public static StringBuilder appendRow( StringBuilder sb , int r ){
        sb.append("Row ");
        sb.append( 1 + r );
        return sb ;
    }
    
    public static StringBuilder appendColumn( StringBuilder sb , int c ){
        sb.append("Column ");
        sb.append( 1 + c );
        return sb ;
    }
    
    public static StringBuilder appendBox( StringBuilder sb , int boxesAcross , int box , ReasoningFormat format ){
        sb.append("Box ");
        switch( format ){
        case MATRIX:
            sb.append('[');
            sb.append( 1 + box / boxesAcross );
            sb.append(',');
            sb.append( 1 + box % boxesAcross );
            sb.append(']');
            break ;
        case FORUM:
            sb.append( 1 + box );
            break ;
        }
        return sb ;
    }
    
    public static StringBuilder appendBox( StringBuilder sb , int boxesAcross , int box ){
        return appendBox( sb , boxesAcross , box , defaultReasoningFormat );
    }
    
    public static StringBuilder appendBox( StringBuilder sb , int boxesAcross , int boxesDown , int r , int c , ReasoningFormat format ){
        sb.append("Box ");
        switch( format ){
        case MATRIX:
            sb.append('[');
            sb.append( 1 + r / boxesAcross );
            sb.append(',');
            sb.append( 1 + c / boxesDown );
            sb.append(']');
            break ;
        case FORUM:
            sb.append( 1 + r / boxesAcross * boxesAcross + c / boxesDown );
            break ;
        }
        return sb ;
    }

    public static StringBuilder appendBox( StringBuilder sb , int boxesAcross , int boxesDown , int r , int c ){
        return appendBox( sb , boxesAcross , boxesDown , r , c , defaultReasoningFormat );
    }
    
    public static String valueToString( int value , ValueFormat format ){
        if( value >= 0 ){
            switch( format ){
            case NUMERIC:
                return Integer.toString( 1 + value );
            case ALPHANUMERIC_0:
                if( value >= 10 ){
                    return Character.toString((char)( 'A' + value - 10 ) );
                }
                return Integer.toString( value );                
            case ALPHANUMERIC_1:
                if( value >= 9 ){
                    return Character.toString((char)( 'A' + value - 9 ) );
                }
                return Integer.toString( 1 + value );                
            case TEXT:
                return Character.toString( text[value] );
            default:
                assert false ;
            }
        }
        return new String();
    }
    
    public static String valueToString( int value ){
        return valueToString( value , defaultValueFormat );
    }
    
    public static StringBuilder appendValue( StringBuilder sb , int v , ValueFormat format ){
        sb.append( valueToString( v , format ) );
        return sb ;
    }
    
    public static StringBuilder appendValue( StringBuilder sb , int v ){
        sb.append( valueToString( v ) );
        return sb ;
    }
    
    public static StringBuilder appendMove( StringBuilder sb , 
                                            int r , 
                                            int c , 
                                            int value , 
                                            ValueFormat valueFormat ,
                                            ReasoningFormat reasoningFormat ){
        appendCell( sb , r , c , reasoningFormat );
        sb.append(":=");
        appendValue( sb , value , valueFormat );
        return sb ;
    }
    
    public static StringBuilder appendMove( StringBuilder sb , int r , int c , int value ){
        return appendMove( sb , r , c , value , defaultValueFormat , defaultReasoningFormat );
    }
}
