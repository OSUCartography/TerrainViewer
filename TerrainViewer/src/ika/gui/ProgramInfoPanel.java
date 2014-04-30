/*
 * ProgramInfoPanel.java
 *
 * Created on January 20, 2006, 1:56 PM
 */

package ika.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * An info or about dialog.
 * @author  Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */

public class ProgramInfoPanel extends javax.swing.JPanel {
    
    private static JDialog infoDialog = null;  
    
    private static Object invokeApplicationInfoMethod(String methodName) {
        try {
            Class applicationInfo = Class.forName("ika.app.ApplicationInfo");
            return applicationInfo.getMethod(methodName, (Class[])null).invoke(null, (Object[])null);
        } catch (Exception exc) {
            return null;
        }
    }
    
    private static void addEscapeKey() {
        JRootPane rootPane = infoDialog.getRootPane();
        InputMap iMap = rootPane.getInputMap(/*JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT*/);
        iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

        ActionMap aMap = rootPane.getActionMap();
        aMap.put("escape", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                hideApplicationInfo();
            }
        });
    }
    
    public static void showApplicationInfo() {
        ProgramInfoPanel.showApplicationInfo(null);
    }
    
    public static void showApplicationInfo(Frame owner) {
        
        if (infoDialog != null){
            infoDialog.setVisible(true);
            return;
        }
        
        ProgramInfoPanel infoPanel = new ProgramInfoPanel();
        if (owner == null) {
            owner = ika.gui.GUIUtil.getFrontMostFrame();
        }
        infoDialog = new JDialog(owner);
        infoDialog.getContentPane().add(infoPanel);
        addEscapeKey();
        
        // name of the application
        String appName = (String)invokeApplicationInfoMethod("getApplicationName");
        
        // window title (standard for about dialogs is not to show a title on Mac)
        if (!ika.utils.Sys.isMacOSX())
            infoDialog.setTitle("Info");
        
        // name
        infoPanel.nameLabel.setText(appName);
        if (appName == null)
            infoPanel.remove(infoPanel.nameLabel);
        
        // icon
        Icon icon = (Icon)invokeApplicationInfoMethod("getLargeApplicationIcon");
        if (icon == null)
            infoPanel.remove(infoPanel.iconLabel);
        infoPanel.iconLabel.setIcon(icon);
        
        // version
        String version = (String)invokeApplicationInfoMethod("getApplicationVersion");
        infoPanel.versionLabel.setText("Version " + version);
        if (version == null)
            infoPanel.remove(infoPanel.versionLabel);
        
        // copyright
        String copyright = (String)invokeApplicationInfoMethod("getCopyright");
        if (copyright == null)
            infoPanel.remove(infoPanel.copyrightLabel);
        infoPanel.copyrightLabel.setText(copyright);
        
        // information
        String information = (String)invokeApplicationInfoMethod("getInformation");
        if (information == null)
            infoPanel.remove(infoPanel.infoLabel);
        infoPanel.infoLabel.setText(information);
        
        // operating system
        String osString = "Operating System: " + System.getProperty("os.name") 
        + " " + System.getProperty("os.version");
        infoPanel.osInfoLabel.setText(osString);

        // java vm version
        String javaString = "Java VM " + System.getProperty("java.version");
        javaString += " by ";
        javaString += System.getProperty("java.vendor");
        infoPanel.javaInfoLabel.setText(javaString);
        
        infoDialog.pack();
        infoDialog.setResizable(false);
        
        // location
        java.awt.Dimension screen = infoDialog.getToolkit().getScreenSize();
        java.awt.Dimension dialog = infoDialog.getSize();
        infoDialog.setLocation((screen.width - dialog.width) / 2,
                (screen.height - dialog.height) / 2 );
        
        infoDialog.setVisible(true);
    }
    
    public static void hideApplicationInfo() {
        if (ProgramInfoPanel.infoDialog != null) {
            ProgramInfoPanel.infoDialog.setVisible(false);
        }            
    }
    
    /** Creates new form ProgramInfoPanel */
    public ProgramInfoPanel() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        iconLabel = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        infoLabel = new javax.swing.JLabel();
        copyrightLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        osInfoLabel = new javax.swing.JLabel();
        javaInfoLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 35, 20, 35));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        add(iconLabel, gridBagConstraints);

        nameLabel.setFont(new java.awt.Font("Lucida Grande", 1, 14));
        nameLabel.setText("Name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(25, 14, 14, 14);
        add(nameLabel, gridBagConstraints);

        infoLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        infoLabel.setText("Information");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 20, 14);
        add(infoLabel, gridBagConstraints);

        copyrightLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        copyrightLabel.setText("Copyright");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 20, 14);
        add(copyrightLabel, gridBagConstraints);

        jSeparator1.setPreferredSize(new java.awt.Dimension(200, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        add(jSeparator1, gridBagConstraints);

        osInfoLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        osInfoLabel.setText("Operating System");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(14, 0, 0, 0);
        add(osInfoLabel, gridBagConstraints);

        javaInfoLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        javaInfoLabel.setText("Java Version");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        add(javaInfoLabel, gridBagConstraints);

        versionLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        versionLabel.setText("Version");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 14, 14, 14);
        add(versionLabel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel copyrightLabel;
    private javax.swing.JLabel iconLabel;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel javaInfoLabel;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel osInfoLabel;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
    
}
