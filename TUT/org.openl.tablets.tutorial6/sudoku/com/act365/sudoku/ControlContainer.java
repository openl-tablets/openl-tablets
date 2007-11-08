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

import com.act365.sudoku.masks.MaskUtils ;

import java.awt.* ;
import java.awt.datatransfer.* ;
import java.awt.event.* ;
import java.text.DecimalFormat ;
import java.util.StringTokenizer ;

/**
 * A ControlContainer instance contains the various buttons and text
 * fields that sit underneath the Su Doku grid and control
 * its operation. 
 */

public class ControlContainer extends com.act365.awt.Container 
                              implements ActionListener ,
                                         ItemListener ,
                                         MouseListener ,
                                         ClipboardOwner {

    final static int defaultMinFilledCellsValue = 32 ;
     
	int boxesAcross ,
		boxesDown ,
        cellsInRow ,
		minFilledCellsValue ;
    
    boolean isApplet ;
    
    Class appClass ;
    
    GridContainer grid ;
        
	TextField across ,
			  down ,
			  minFilledCells ,
              text ;

    TextArea reasoningArea ;
                  
	Button solve ,
		   unsolve ,
		   reset ,
		   resize ,
		   evaluate ,
		   compose ,
           interrupt ,
           copy ,
           paste ,
           shuffle ;

    Label solns ,
          time ,
          lockedSectorCandidatesEliminations ,
          disjointSubsetsEliminations ,
          twoSectorDisjointSubsetsEliminations ,
          singleValuedStringsEliminations ,
          manyValuedStringsEliminations ,
          nishioEliminations ,
          permutateAdjacentSectorsEliminations ,
          nGuesses ;

    Choice format ,
           copyType ,
           maskType ,
           reasoningFormat ;
              
    Checkbox lockedSectorCandidates ,
             disjointSubsets ,
             twoSectorDisjointSubsets ,
             singleValuedStrings ,
             manyValuedStrings ,
             nishio ,
             permutateAdjacentSectors ,
             guess ;
             
    SuDokuClipboard clipboard ;
    
    /**
     * Creates a new ControlContainer to control the given
     * GridContainer.
     */
    
    public ControlContainer( GridContainer grid , Class appClass ){
        
        int i ;
        
        // Set state variables.
        this.grid = grid ;
        this.appClass = appClass ;
        try {
            Class suDokuApplet = Class.forName("com.act365.sudoku.SuDokuApplet");
            isApplet = appClass.equals( suDokuApplet );
        } catch ( ClassNotFoundException e ) {
            isApplet = false ;
        }
        if( isApplet ){
            clipboard = new SuDokuClipboard();    
        }
        boxesAcross = grid.getBoxesAcross();
        boxesDown = grid.getBoxesDown();
        cellsInRow = boxesAcross * boxesDown ;
        minFilledCellsValue = defaultMinFilledCellsValue ;
        
        // Add the Solve controls.
        solve = new Button("Solve");
        solve.addActionListener( this );
        unsolve = new Button("Unsolve");
        unsolve.addActionListener( this );
        reset = new Button("Reset");
        reset.addActionListener( this );
    
        // Add the Evaluate controls
        evaluate = new Button("Evaluate");
        evaluate.addActionListener( this );
        solns = new Label();
        time = new Label("0.0s");
        
        // Add the Shuffle and Format controls
        shuffle = new Button("Shuffle");
        shuffle.addActionListener( this );
        format = new Choice();
        i = 0 ;
        while( i < SuDokuUtils.ValueFormat.values().length ){
            format.add( SuDokuUtils.ValueFormat.values()[i++].toString() );
        }
        format.select( SuDokuUtils.defaultValueFormat.toString() );
        format.addItemListener( this );
        text = new TextField( 9 );
            
        // Add the Copy/Paste controls.
        copy = new Button("Copy");
        copy.addActionListener( this );
        paste = new Button("Paste");
        paste.addActionListener( this );
        copyType = new Choice();
        i = 0 ;
        while( i < SuDokuUtils.copyTypes.length ){
            copyType.add( SuDokuUtils.copyTypes[i++] );
        }
        copyType.addItemListener( this );
        
        // Add the Resize controls.
        resize = new Button("Resize");
        resize.addActionListener( this );
        across = new TextField(1);
        down = new TextField(1);
        
        // Add the Compose controls
        compose = new Button("Compose");
        compose.addActionListener( this );
        minFilledCells = new TextField( 2 );
        interrupt = new Button("Break");
        interrupt.addActionListener( this );
        maskType = new Choice();
        i = 0 ;
        while( i < MaskUtils.longLabels.length ){
            maskType.add( MaskUtils.longLabels[i++] );
        }
        maskType.addItemListener( this );

        // Add the Strategy controls.
        lockedSectorCandidates = new Checkbox("Locked Sector Candidates" , grid.getStrategy().useLockedSectorCandidates );
        lockedSectorCandidates.addItemListener( this );
        lockedSectorCandidatesEliminations = new Label("0");
        disjointSubsets = new Checkbox("Disjoint Subsets" , grid.getStrategy().useDisjointSubsets );
        disjointSubsets.addItemListener( this );
        disjointSubsetsEliminations = new Label("0");
        twoSectorDisjointSubsets = new Checkbox("Two-Sector Disjoint Subsets" , grid.getStrategy().useTwoSectorDisjointSubsets );
        twoSectorDisjointSubsets.addItemListener( this );
        twoSectorDisjointSubsetsEliminations = new Label("0");
        singleValuedStrings = new Checkbox("Single-Valued Chains" , grid.getStrategy().useSingleValuedChains );
        singleValuedStrings.addItemListener( this );
        singleValuedStringsEliminations = new Label("0");
        manyValuedStrings = new Checkbox("Many-Valued Chains" , grid.getStrategy().useManyValuedChains );
        manyValuedStrings.addItemListener( this );
        manyValuedStringsEliminations = new Label("0");
        nishio = new Checkbox("Nishio" , grid.getStrategy().useNishio );
        nishio.addItemListener( this );
        nishioEliminations = new Label("0");
        permutateAdjacentSectors = new Checkbox("Permutate Sectors" , grid.getStrategy().useAdjacentSectorPermutation );
        permutateAdjacentSectors.addItemListener( this );
        permutateAdjacentSectorsEliminations = new Label("0");
        guess = new Checkbox("Guess" , grid.getStrategy().useGuesses );
        guess.addItemListener( this );
        nGuesses = new Label("0");
        
        // Add the Reasoning area
        reasoningFormat = new Choice();
        i = 0 ;
        while( i < SuDokuUtils.ReasoningFormat.values().length ){
            reasoningFormat.add( SuDokuUtils.ReasoningFormat.values()[i++].toString() );
        }
        reasoningFormat.select( SuDokuUtils.defaultReasoningFormat.toString() );
        reasoningFormat.addItemListener( this );
        reasoningArea = new TextArea();
        reasoningArea.setEditable( false );
        reasoningArea.addMouseListener( this );
                    
        // Lay out the components.
		addComponent( solve , 0 , 0 , 3 , 1 , 1 , 0 );
		addComponent( unsolve , 4 , 0 , 3 , 1 , 1 , 0 );
		addComponent( reset , 8 , 0 , 3 , 1 , 1 , 0 );

        addComponent( evaluate , 0 , 1 , 3 , 1 , 1 , 0 );
        addComponent( new Label("Solutions") , 4 , 1 , 2 , 1 , 1 , 0 );
        addComponent( solns , 6 , 1 , 1 , 1 , 1 , 0 );
        addComponent( new Label("Time") , 7 , 1 , 2 , 1 , 1 , 0 );
        addComponent( time , 9 , 1 , 2 , 1 , 1 , 0 );
        
        addComponent( shuffle , 0 , 2 , 3 , 1 , 1 , 0 );
        addComponent( format , 3 , 2 , 3 , 1 , 1 , 0 );
        addComponent( new Label("Text") , 6 , 2 , 2 , 1 , 1 , 0 );
        addComponent( text , 8 , 2 , 3 , 1 , 1 , 0 );
        
        addComponent( copy , 0 , 3 , 3 , 1 , 1 , 0 );
        addComponent( paste , 4 , 3 , 3 , 1 , 1 , 0 );
        addComponent( copyType , 8 , 3 , 3 , 1 , 1 , 0 );
        
		addComponent( resize , 0 , 4 , 3 , 1 , 1 , 0 );
		addComponent( new Label("Across") , 4 , 4 , 2 , 1 , 1 , 0 );
		addComponent( across , 6 , 4 , 1 , 1 , 1 , 0 );
		addComponent( new Label("Down") , 8 , 4 , 2 , 1 , 1 , 0 );
		addComponent( down , 10 , 4 , 1 , 1 , 1 , 0 );	

        addComponent( compose , 0 , 5 , 3 , 1 , 1 , 0 );
        addComponent( new Label("Filled Cells") , 4 , 5 , 2 , 1 , 1 , 0 );
        addComponent( minFilledCells , 6 , 5 , 1 , 1 , 1 , 0 );
        addComponent( interrupt , 8 , 5 , 3 , 1 , 1 , 0 );
        addComponent( new Label("Symmetry") , 0 , 6 , 3 , 1 , 1 , 0 );
        addComponent( maskType , 4 , 6 , 3 , 1 , 1 , 0 );
                
        addComponent( new Label("Strategy:") , 0 , 7 , 2 , 1 , 1 , 0 );
        addComponent( lockedSectorCandidates , 4 , 7 , 3 , 1 , 1 , 0 );
        addComponent( lockedSectorCandidatesEliminations , 10 , 7 , 1 , 1 , 1 , 0 );
        addComponent( disjointSubsets , 4 , 8 , 3 , 1 , 1 , 0 );
        addComponent( disjointSubsetsEliminations , 10 , 8 , 1 , 1 , 1 , 0 );
        addComponent( twoSectorDisjointSubsets , 4 , 9 , 3 , 1 , 1 , 0 );
        addComponent( twoSectorDisjointSubsetsEliminations , 10 , 9 , 1 , 1 , 1 , 0 );
        addComponent( singleValuedStrings , 4 , 10 , 3 , 1 , 1 , 0 );
        addComponent( singleValuedStringsEliminations , 10 , 10 , 1 , 1 , 1 , 0 );
        addComponent( manyValuedStrings , 4 , 11 , 3 , 1 , 1 , 0 );
        addComponent( manyValuedStringsEliminations , 10 , 11 , 1 , 1 , 1 , 0 );
        addComponent( nishio , 4 , 12 , 3 , 1 , 1 , 0 );
        addComponent( nishioEliminations , 10 , 12 , 1 , 1 , 1 , 0 );
        addComponent( permutateAdjacentSectors , 4 , 13 , 3 , 1 , 1 , 0 );
        addComponent( permutateAdjacentSectorsEliminations , 10 , 13 , 1 , 1 , 1 , 0 );
        addComponent( guess , 4 , 14 , 3 , 1 , 1 , 0 );
        addComponent( nGuesses , 10 , 14 , 1 , 1 , 1 , 0 );
                
        addComponent( new Label("Format:") , 0 , 15 , 2 , 1 , 1 , 0 );
        addComponent( reasoningFormat , 2 , 15 , 3 , 1 , 1 , 0 );
        addComponent( reasoningArea , 0 , 16 , 11 , 5 , 1 , 1 );
          
		write();	
    }    

    /**
     * The size at which the ControlContainer looks its best.
     */
	
    @Override public Dimension getBestSize() {
		return new Dimension( 300 , 750 );
	}

	/**
	 * Reacts to button presses. 
	 */
    
	public void actionPerformed( ActionEvent evt ){
        
		if( evt.getSource() == solve ){
            read();
			grid.solve();
            if( grid.getStrategy().explainsReasoning() ){
                reasoningArea.setText( null );
                reasoningArea.append("START\n"); 
                int i = 0 ;
                while( i < grid.getStrategy().getThreadLength() ){
                    reasoningArea.append( ( 1 + i ) + ". " + grid.getStrategy().getReason( i ) );
                    ++ i ;
                }
            }
            time.setText( new DecimalFormat("#0.000").format( grid.getSolveTime() )+ "s");
            write();
		} else if( evt.getSource() == unsolve ) {
			grid.unsolve();
		} else if( evt.getSource() == reset ){
			grid.reset();
            reasoningArea.setText( null );
            time.setText("0.0s");
            write();
        } else if( evt.getSource() == shuffle ){
            grid.shuffle();
        } else if( evt.getSource() == copy ) {
            StringBuilder sb = new StringBuilder();
            read();
            grid.read();
            switch( SuDokuUtils.defaultCopyType ){
                case SuDokuUtils.PLAIN_TEXT:
                    sb.append( grid.toString() );
                    break;
                case SuDokuUtils.LIBRARY_BOOK:
                    sb.append( SuDokuUtils.libraryBookHeader( appClass.getName() , cellsInRow , boxesAcross , SuDokuUtils.featuredGrades ) );
                    sb.append( grid.toString() );
                    sb.append( SuDokuUtils.libraryBookFooter() );
                    break;
                default:
                    sb.append( grid.strategy.printState( SuDokuUtils.defaultCopyType ) );
                    break;
            }
            if( isApplet ){
                clipboard.setText( sb.toString() );
                clipboard.setVisible( true );
            } else {
                getToolkit().getSystemClipboard().setContents( new StringSelection( sb.toString() ) , this );
            }
        } else if( evt.getSource() == paste ){
            read();
            String pasteText = null ;
            if( isApplet ){
                if( clipboard != null ){
                    pasteText = clipboard.getText();
                }
            } else {
                Transferable transferable = getToolkit().getSystemClipboard().getContents( this );
                if( transferable != null ){
                    try {
                        pasteText = (String) transferable.getTransferData( DataFlavor.stringFlavor );
                    } catch( Exception e ) {
                    }
                }
            }
            if( pasteText != null ){
                try {
                    grid.paste( pasteText );
                    boxesAcross = grid.getBoxesAcross();
                    boxesDown = grid.getBoxesDown();
                    cellsInRow = boxesAcross * boxesDown ;
                    write();
                } catch ( Exception e ) { 
                    grid.reset();
                }
            }
		} else if( evt.getSource() == resize ){
			read();
            grid.setSize( boxesAcross , boxesDown );
			grid.setBoxes( boxesAcross , boxesDown );
			write();
		} else if( evt.getSource() == evaluate ){
            read();
			switch( grid.evaluate() ){
				case 0 :
				    solns.setText("None");
				    break;
				case 1 :
				    solns.setText("Unique");
				    break;
				case 2 :
				    solns.setText("Multiple");
				    break;
				default:
				    solns.setText("Error");
				    break;	    
			}
		} else if( evt.getSource() == compose ) {
			read();
			minFilledCellsValue = grid.startComposer( minFilledCellsValue );
			write();
        } else if( evt.getSource() == interrupt ) {
            grid.stopComposer();
		}
	}
	
    /**
     * 
     * When a mouse is clicked in the Reasoning Area, the puzzle will reset itself 
     * to the appropriate partial-solution. 
     */
    
    public void mouseClicked( MouseEvent evt ){
        int lastMove = 0 ,
            fullStopIndex ;
        String reasonText = reasoningArea.getText().substring( 0 , reasoningArea.getCaretPosition() ) ,
               reason = "";
        StringTokenizer st = new StringTokenizer( reasonText , "\n" );
        while( st.hasMoreTokens() ){
            try {
                reason = st.nextToken();
                if( ( fullStopIndex = reason.indexOf('.') ) >= 0 ){
                    lastMove = Integer.parseInt( reason.substring( 0 , fullStopIndex ) );
                }
            } catch( NumberFormatException e ) {
            }
        }
        grid.unsolve( lastMove );
        grid.strategy.unwind( lastMove , false , false );
    }
    
    /**
     * Mouse motion is ignored.
     */
    
    public void mouseEntered( MouseEvent evt ){
    }
    
    /**
     * Mouse motion is ignored.
     */
    
    public void mouseExited( MouseEvent evt ){
    }
    
    /**
     * Mouse presses are ignored.
     */
    
    public void mousePressed( MouseEvent evt ){
    }

    /**
     * Mouse releases are ignored.
     */    
    
    public void mouseReleased( MouseEvent evt ){
    }
    
    /**
     * Reacts to changes to the strategy list.
     */

    public void itemStateChanged( ItemEvent evt ){
        if( evt.getSource() == lockedSectorCandidates ){
            grid.getStrategy().useLockedSectorCandidates = lockedSectorCandidates.getState();
        } else if( evt.getSource() == disjointSubsets ){
            grid.getStrategy().useDisjointSubsets = disjointSubsets.getState();
        } else if( evt.getSource() == twoSectorDisjointSubsets ){
            grid.getStrategy().useTwoSectorDisjointSubsets = twoSectorDisjointSubsets.getState();
        } else if( evt.getSource() == singleValuedStrings ){
            grid.getStrategy().useSingleValuedChains = singleValuedStrings.getState();
        } else if( evt.getSource() == manyValuedStrings ){
            grid.getStrategy().useManyValuedChains = manyValuedStrings.getState();
        } else if( evt.getSource() == nishio ){
            grid.getStrategy().useNishio = nishio.getState();
        } else if( evt.getSource() == permutateAdjacentSectors ){
            grid.getStrategy().useAdjacentSectorPermutation = permutateAdjacentSectors.getState();
        } else if( evt.getSource() == guess ) {
            grid.getStrategy().useGuesses = guess.getState();
        } else if( evt.getSource() == format ) {
            SuDokuUtils.defaultValueFormat = SuDokuUtils.ValueFormat.values()[ format.getSelectedIndex() ];
        } else if( evt.getSource() == copyType ){
            SuDokuUtils.defaultCopyType = copyType.getSelectedIndex();
        } else if( evt.getSource() == maskType ){
            grid.maskType = maskType.getSelectedIndex();
        } else if( evt.getSource() == reasoningFormat ){
            SuDokuUtils.defaultReasoningFormat = SuDokuUtils.ReasoningFormat.values()[ reasoningFormat.getSelectedIndex() ];
        }
    }
    
	/**
	 * Reads data from the GUI to the state variables.
	 */
	
	void read(){
		try {
			boxesAcross = Integer.parseInt( across.getText() );
		} catch ( NumberFormatException e ) {        	
		}
		try {
			boxesDown = Integer.parseInt( down.getText() );
		} catch ( NumberFormatException e ) {        	
		}		
		try {
			minFilledCellsValue = Integer.parseInt( minFilledCells.getText() );
		} catch ( NumberFormatException e ) {        	
		}		
        cellsInRow = boxesAcross * boxesDown ;
        try {
            SuDokuUtils.setText( text.getText() , cellsInRow );
        } catch ( Exception e ) {
        }
	}
	
	/**
	 * Writes state variables to the GUI.
	 */
	
	void write(){
		across.setText( Integer.toString( boxesAcross ) );
		down.setText( Integer.toString( boxesDown ) );
		minFilledCells.setText( Integer.toString( minFilledCellsValue ));
        lockedSectorCandidatesEliminations.setText( Integer.toString( grid.getStrategy().lockedSectorCandidatesEliminations ) );
        disjointSubsetsEliminations.setText( Integer.toString( grid.getStrategy().disjointSubsetsEliminations ) );
        twoSectorDisjointSubsetsEliminations.setText( Integer.toString( grid.getStrategy().twoSectorDisjointSubsetsEliminations ) );
        singleValuedStringsEliminations.setText( Integer.toString( grid.getStrategy().singleValuedChainsEliminations ) );
        manyValuedStringsEliminations.setText( Integer.toString( grid.getStrategy().manyValuedChainsEliminations ) );
        nishioEliminations.setText( Integer.toString( grid.getStrategy().nishioEliminations ) );
        permutateAdjacentSectorsEliminations.setText( Integer.toString( grid.getStrategy().adjacentSectorPermutationEliminations ) );
        nGuesses.setText( Integer.toString( grid.getStrategy().nGuesses ) );
        if( SuDokuUtils.text.length != cellsInRow ){
            SuDokuUtils.setDefaultText( cellsInRow ); 
        }
        text.setText( new String( SuDokuUtils.text ) );
        minFilledCells.setText( Integer.toString( minFilledCellsValue ) );
	}
    
    /**
     * Nothing happens if ownership of the clipboard contents is lost.
     */
    
    public void lostOwnership( Clipboard clipboard , Transferable transferable ){
    }
}
