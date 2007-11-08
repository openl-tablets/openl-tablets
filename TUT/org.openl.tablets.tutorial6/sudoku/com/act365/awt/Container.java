/*
 * act365.com Abstract Windowing Toolkit
 * 
 * Copyright (C) act365.com November 2004
 * 
 * Web site: http://act365.com
 * E-mail: developers@act365.com
 * 
 * The act365.com Abstract Windowing Toolkit defines standard GUI classes
 * common to all act365.com apps.
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

package com.act365.awt;

import java.awt.*;

/**
 * The Container class is the base for all containers used in act365.com apps.
 * It provides a simplified interface to the GridBagLayout manager. 
 * 
 * @see java.awt.Container
 * @see java.awt.GridBagLayout
 */

public abstract class Container extends java.awt.Container {

    /**
     * Creates a new Container instance.
     */
    
    public Container() {
        setLayout( new GridBagLayout() );   
    }

    /**
     * Each concrete Container subclass has to 'know' its ideal display size.
     */

    protected abstract Dimension getBestSize();
    
    /**
     * Adds a component to the Container.
     */

    protected void addComponent( Component comp ,
                                 int x ,
                                 int y ,
                                 int w ,
                                 int h ,
                                 int weightx ,
                                 int weighty ,
                                 int inset ) {
        
        GridBagLayout gbl = (GridBagLayout) getLayout();
        GridBagConstraints gbc = new GridBagConstraints();
     
        gbc.fill = GridBagConstraints.BOTH ;
        gbc.gridx = x ;
        gbc.gridy = y ;
        gbc.gridwidth = w ;
        gbc.gridheight = h ;
        gbc.weightx = weightx ;
        gbc.weighty = weighty ;
        gbc.insets = new Insets( inset , inset , inset , inset );
        
        add( comp );
        gbl.setConstraints( comp , gbc );    
    }
    
    /**
     * Adds a component to the Container.
     */

    protected void addComponent( Component comp ,
                                 int x ,
                                 int y ,
                                 int w ,
                                 int h ,
                                 int weightx ,
                                 int weighty ) {
        addComponent( comp , x , y , w , h , weightx , weighty , 5 );
    }
}
