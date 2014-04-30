/*
 * @(#)GradientSliderDemo.java
 *
 * $Date$
 *
 * Copyright (c) 2009 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.blogspot.com/
 * 
 * And the latest version should be available here:
 * https://javagraphics.dev.java.net/
 */
package com.bric.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JFrame;

/** A simple demo of the <code>GradientSlider</code> class.
 * 
 * @see http://javagraphics.blogspot.com/2008/05/gradients-gui-widget-to-design-them.html
 */
public class GradientSliderDemo extends JApplet {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {

		JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new GradientSliderDemo());
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public GradientSliderDemo() {
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1; c.gridy = 0; c.weightx = 1; c.weighty = 1;
		c.fill = GridBagConstraints.BOTH; c.insets = new Insets(3,3,3,3);
		GradientSlider slider = new GradientSlider();
		slider.setValues(new float[] {0, 1}, new Color[] { Color.red, Color.black});
		slider.setPaintTicks(true);
		slider.putClientProperty("MultiThumbSlider.indicateSelectedThumb", "false");
		getContentPane().add(slider,c);

		c.gridx = 0; c.gridy = 1;
		slider = new GradientSlider(GradientSlider.VERTICAL);
		slider.setValues(new float[] {0, 1}, new Color[] { new Color(255,0,255), Color.black});
		slider.setPaintTicks(true);
		slider.putClientProperty("MultiThumbSlider.indicateComponent", "false");
		slider.putClientProperty("GradientSlider.useBevel", "true");
		getContentPane().add(slider,c);

		c.gridx = 2; c.gridy = 1;
		slider = new GradientSlider(GradientSlider.VERTICAL);
		slider.setValues(new float[] {0, 1}, new Color[] { Color.yellow, Color.white});
		slider.setInverted(true);
		slider.putClientProperty("MultiThumbSlider.indicateComponent", "false");
		getContentPane().add(slider,c);

		c.gridx = 1; c.gridy = 2;
		slider = new GradientSlider();
		slider.setValues(new float[] {0, 1}, new Color[] { Color.green, Color.white});
		slider.setInverted(true);
		slider.putClientProperty("GradientSlider.useBevel", "true");
		getContentPane().add(slider,c);
		
		getContentPane().setBackground(Color.white);
		if(getContentPane() instanceof JComponent)
			((JComponent)getContentPane()).setOpaque(true);
	}
}
