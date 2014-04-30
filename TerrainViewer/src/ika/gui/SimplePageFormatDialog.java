/*
 * PageFormatDialog.java
 *
 * Created on April 20, 2007, 1:27 PM
 */

package ika.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * A dialog that lets the user enter a width and height value in millimeters.
 * The values are stored in the GUI - not as it should properly be done!
 * Usage: Dimension2D dim = SimplePageFormatDialog.showDialog(null);
 * @author  Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class SimplePageFormatDialog extends javax.swing.JDialog {
    
    /**
     * Remember whether the user canceled.
     */
    private boolean abort = false;
    
    /** Creates new form PageFormatDialog */
    public SimplePageFormatDialog(java.awt.Frame parent, boolean modal, PageFormat pageFormat) {
        super(parent, modal);
        initComponents();
        this.simplePageFormatPanel.setPageFormat(pageFormat);
        this.initDialog();
    }
    
    /**
     * Shows a modal dialog and returns the dimension selected by the user
     * or null if the user cancels.
     */
    public PageFormat showDialog() {
        
        this.abort = false;
        this.setModal(true);
        this.setVisible(true);
        return this.abort ? null : this.simplePageFormatPanel.getPageFormat();
    }
    
    /**
     * Returns the last page format.
     */
    public PageFormat getPageFormat() {
        if (this.abort)
            return null;
        return this.simplePageFormatPanel.getPageFormat();
    }
    
    private void initDialog() {
        
        // location
        java.awt.Dimension screenDim = this.getToolkit().getScreenSize();
        java.awt.Dimension dialogDim = this.getSize();
        this.setLocation((screenDim.width - dialogDim.width) / 2,
                (screenDim.height - dialogDim.height) / 2 );
        
        // ok button reacts on return key, cancel button on escape key.
        this.getRootPane().setDefaultButton(this.okButton);
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                abort = true;
            }
        };
        this.cancelButton.registerKeyboardAction(l, "EscapeKey",
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,
                0 , true), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        simplePageFormatPanel = new ika.gui.SimplePageFormatPanel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Map Scale & Size");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));

        cancelButton.setText("Cancel");
        cancelButton.setDefaultCapable(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel1.add(cancelButton);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        jPanel1.add(okButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 20, 20);
        getContentPane().add(jPanel1, gridBagConstraints);

        simplePageFormatPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 30, 20, 30));
        getContentPane().add(simplePageFormatPanel, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.setVisible(false);
        this.abort = true;
    }//GEN-LAST:event_formWindowClosing
    
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
        this.abort = true;
    }//GEN-LAST:event_cancelButtonActionPerformed
    
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        this.setVisible(false);
        this.abort = false;
    }//GEN-LAST:event_okButtonActionPerformed
 
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okButton;
    private ika.gui.SimplePageFormatPanel simplePageFormatPanel;
    // End of variables declaration//GEN-END:variables
    
}