/*
 * VectorSymbolPanel.java
 *
 * Created on July 6, 2006, 8:36 AM
 */
package ika.gui;

import ika.geo.VectorSymbol;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class VectorSymbolPanel extends javax.swing.JPanel {

    private VectorSymbol vs;
    private boolean updating = false;
    private AbstractFormatterFactory strokeWidthFormatter;

    /**
     * Creates new form VectorSymbolPanel
     */
    public VectorSymbolPanel() {
        javax.swing.text.NumberFormatter nf = new javax.swing.text.NumberFormatter();
        nf.setMinimum(0f);
        strokeWidthFormatter = new javax.swing.text.DefaultFormatterFactory(nf);
        initComponents();
    }

    public void setVectorSymbol(VectorSymbol vs) {
        this.vs = vs;
        this.write();
    }

    public VectorSymbol getVectorSymbol() {
        return this.vs;
    }

    private void enableTransparency() {
        // transparency
        boolean enableTransparency = vs.isFilled();
        transparencyLabel.setEnabled(enableTransparency);
        transparencySlider.setEnabled(enableTransparency);
        transparencyField.setEnabled(enableTransparency);
    }

    public void write() {
        if (vs == null) {
            // stroke
            strokeLabel.setEnabled(false);
            strokeWidthField.setEnabled(false);
            strokeCheckBox.setEnabled(false);
            strokeColorButton.setEnabled(false);
            pxLabel.setEnabled(false);

            // fill
            fillLabel.setEnabled(false);
            fillCheckBox.setEnabled(false);
            fillColorButton.setEnabled(false);

            // transparency
            transparencyLabel.setEnabled(false);
            transparencySlider.setEnabled(false);
            transparencyField.setEnabled(false);
            return;
        }

        // stroke
        strokeLabel.setEnabled(true);
        strokeWidthField.setEnabled(true);
        strokeCheckBox.setEnabled(true);
        strokeColorButton.setEnabled(true);
        pxLabel.setEnabled(true);

        // fill
        fillLabel.setEnabled(true);
        fillCheckBox.setEnabled(true);
        fillColorButton.setEnabled(true);

        enableTransparency();

        try {
            updating = true;

            // stroke
            strokeWidthField.setValue(new Float(vs.getStrokeWidth()));
            strokeCheckBox.setSelected(vs.isStroked());
            strokeColorButton.setColor(vs.getStrokeColor());

            // fill
            fillCheckBox.setSelected(vs.isFilled());
            java.awt.Color fillColor = vs.getFillColor();
            fillColorButton.setColor(fillColor);
            int alpha = 255 - fillColor.getAlpha();
            transparencySlider.setValue(alpha);
            transparencyField.setValue(alpha);
        } finally {
            updating = false;
        }
    }

    public void read() {
        if (updating) {
            return;
        }

        if (vs == null) {
            return;
        }

        VectorSymbol old_vs = (VectorSymbol) vs.clone();

        vs.setStrokeWidth(((Number) strokeWidthField.getValue()).floatValue());
        vs.setStroked(strokeCheckBox.isSelected());
        vs.setStrokeColor(strokeColorButton.getColor());

        // fill
        vs.setFilled(fillCheckBox.isSelected());
        java.awt.Color fillColor = fillColorButton.getColor();

        final int alpha = 255 - transparencySlider.getValue();
        fillColor = new java.awt.Color(fillColor.getRed(), fillColor.getGreen(),
                fillColor.getBlue(), alpha);
        vs.setFillColor(fillColor);

        this.firePropertyChange("vectorsymbol", old_vs, this.vs);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        strokeLabel = new javax.swing.JLabel();
        strokeCheckBox = new javax.swing.JCheckBox();
        strokeColorButton = new ika.gui.ColorButton();
        fillLabel = new javax.swing.JLabel();
        fillCheckBox = new javax.swing.JCheckBox();
        fillColorButton = new ika.gui.ColorButton();
        transparencySlider = new javax.swing.JSlider();
        transparencyLabel = new javax.swing.JLabel();
        strokeWidthField = new javax.swing.JFormattedTextField();
        pxLabel = new javax.swing.JLabel();
        transparencyField = new javax.swing.JFormattedTextField();

        setLayout(new java.awt.GridBagLayout());

        strokeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        strokeLabel.setText("Stroke");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        add(strokeLabel, gridBagConstraints);

        strokeCheckBox.setSelected(true);
        strokeCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        strokeCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        strokeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeCheckBoxgeometryActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(strokeCheckBox, gridBagConstraints);

        strokeColorButton.setColorChooserTitle("Stroke Color");
        strokeColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strokeColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(strokeColorButton, gridBagConstraints);

        fillLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        fillLabel.setText("Fill");
        fillLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        add(fillLabel, gridBagConstraints);

        fillCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        fillCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        fillCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillCheckBoxgeometryActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(fillCheckBox, gridBagConstraints);

        fillColorButton.setColorChooserTitle("Fill Color");
        fillColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        add(fillColorButton, gridBagConstraints);

        transparencySlider.setMaximum(255);
        transparencySlider.setValue(0);
        transparencySlider.setPreferredSize(new java.awt.Dimension(150, 29));
        transparencySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                transparencySlidercolorSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(transparencySlider, gridBagConstraints);

        transparencyLabel.setText("Transparency");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(transparencyLabel, gridBagConstraints);

        strokeWidthField.setColumns(8);
        strokeWidthField.setFormatterFactory(strokeWidthFormatter);
        strokeWidthField.setMinimumSize(new java.awt.Dimension(70, 28));
        strokeWidthField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                strokeWidthFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        add(strokeWidthField, gridBagConstraints);

        pxLabel.setText("px");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        add(pxLabel, gridBagConstraints);

        transparencyField.setPreferredSize(new java.awt.Dimension(55, 28));
        javax.swing.text.NumberFormatter nf2 = new javax.swing.text.NumberFormatter();
        nf2.setMinimum(0);
        nf2.setMaximum(255);
        transparencyField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nf2));
        transparencyField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                transparencyFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 8;
        add(transparencyField, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void transparencySlidercolorSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_transparencySlidercolorSliderStateChanged
        if (this.updating) {
            return;
        }
        try {
            this.updating = true;
            int alpha = this.transparencySlider.getValue();
            this.transparencyField.setValue(alpha);
        } finally {
            this.updating = false;
        }
        if (transparencySlider.getValueIsAdjusting() == false) {
            this.read();
        }
    }//GEN-LAST:event_transparencySlidercolorSliderStateChanged

    private void fillColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fillColorButtonActionPerformed
        this.fillCheckBox.setSelected(true);
        this.read();
        enableTransparency();
    }//GEN-LAST:event_fillColorButtonActionPerformed

    private void fillCheckBoxgeometryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fillCheckBoxgeometryActionPerformed
        // first read then update GUI for fill transparency
        read();
        enableTransparency();
    }//GEN-LAST:event_fillCheckBoxgeometryActionPerformed

    private void strokeColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeColorButtonActionPerformed
        try {
            updating = true;
            this.strokeCheckBox.setSelected(true);
        } finally {
            updating = false;
        }        
        this.read();
    }//GEN-LAST:event_strokeColorButtonActionPerformed

    private void strokeCheckBoxgeometryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeCheckBoxgeometryActionPerformed
        this.read();
    }//GEN-LAST:event_strokeCheckBoxgeometryActionPerformed

    private void strokeWidthFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_strokeWidthFieldPropertyChange
        if (this.updating) {
            return;
        }
        if ("value".equals(evt.getPropertyName()) && strokeWidthField.getValue() != null) {
            read();
        }
    }//GEN-LAST:event_strokeWidthFieldPropertyChange

    private void transparencyFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_transparencyFieldPropertyChange
        if (this.updating) {
            return;
        }
        if ("value".equals(evt.getPropertyName()) && transparencyField.getValue() != null) {
            int alpha = ((Number) transparencyField.getValue()).intValue();
            try {
                updating = true;
                transparencySlider.setValue(alpha);
            } finally {
                updating = false;
            }
            read();
        }
    }//GEN-LAST:event_transparencyFieldPropertyChange
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox fillCheckBox;
    private ika.gui.ColorButton fillColorButton;
    private javax.swing.JLabel fillLabel;
    private javax.swing.JLabel pxLabel;
    private javax.swing.JCheckBox strokeCheckBox;
    private ika.gui.ColorButton strokeColorButton;
    private javax.swing.JLabel strokeLabel;
    private javax.swing.JFormattedTextField strokeWidthField;
    private javax.swing.JFormattedTextField transparencyField;
    private javax.swing.JLabel transparencyLabel;
    private javax.swing.JSlider transparencySlider;
    // End of variables declaration//GEN-END:variables
}
