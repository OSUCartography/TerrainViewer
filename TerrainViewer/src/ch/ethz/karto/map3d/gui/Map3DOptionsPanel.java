/*
 * View3DPanel.java
 *
 * Created on October 1, 2008, 2:16 PM
 */
package ch.ethz.karto.map3d.gui;

import ch.ethz.karto.map3d.Map3DViewer;
import ch.ethz.karto.map3d.Map3DViewer.Camera;
import com.bric.swing.ColorPicker;
import com.bric.swing.MultiThumbSlider;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.prefs.Preferences;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DOptionsPanel extends javax.swing.JPanel implements PropertyChangeListener, KeyListener {

    private static final int SHEAR_SLIDER_MAX = 180;
    private static final int SHEAR_SLIDER_MIN = 0;
    private static final int CYL_HEIGHT_SLIDER_MAX = 100;
    private static final int CYL_HEIGHT_SLIDER_MIN = -100;
    private static final int INCLINATION_SLIDER_MAX = 90;
    private static final int INCLINATION_SLIDER_MIN = 0;
    private static final float MIN_VIEW_ANGLE = 0.5f;
    private static final String SHIFTX_TOOLTIP = "Move model left or right.";
    private static final String SHIFTY_TOOLTIP = "Move model up or down.";
    private static final String CYLINDER_POSX_TOOLTIP = "Center of cylindrical camera.";
    private static final String CYLINDER_POSY_TOOLTIP = "Center of cylindrical camera.";
    private Map3DViewer map3DViewer;
    private boolean updatingGUI = false;
    private static javax.swing.JDialog dialog = null;

    public static void show3DOptions(JFrame owner, Map3DViewer map3DViewer) {

        if (dialog == null) {
            dialog = new JDialog(owner, "Display Options", false);

            //frame = new javax.swing.JFrame("Display Options");
            dialog.getRootPane().putClientProperty("Window.style", "small");

            // setAlwaysOnTop() is not supported for applets
            try {
                dialog.setAlwaysOnTop(true);
            } catch (SecurityException se) {
            }

            dialog.add(new Map3DOptionsPanel(map3DViewer));
            dialog.pack();
            dialog.setLocation(15, 30);
            dialog.setResizable(false);
        }
        dialog.setVisible(true);

    }

    public static void hide3DOptions() {

        if (dialog != null) {
            dialog.setVisible(false);
        }

    }
    private static String prefsNodeName = "/ethz/karto/map3d/antialiasing";
    private static String prefsAntialiasingLevel = "antialiasinglevel";

    public Map3DOptionsPanel(Map3DViewer map3DViewer) {
        initComponents();
        this.setMap3DViewer(map3DViewer);
    }

    public Map3DOptionsPanel() {
        initComponents();
    }

    public void setMap3DViewer(Map3DViewer newMap3DViewer) {
        if (newMap3DViewer == null) {
            return;
        }
        if (this.map3DViewer != null) {
            this.map3DViewer.getComponent().removePropertyChangeListener(this);
        }

        this.map3DViewer = newMap3DViewer;
        this.writeModelToGUI();
        newMap3DViewer.getComponent().addPropertyChangeListener("view", this);
        if (newMap3DViewer.is2D()) {
            this.viewAngleSlider.setEnabled(false);
        }
    }

    public void addPanel(String title, Component panel) {
        optionsTabbedPane.addTab(title, panel);
    }

    public void setAntialiasingPanelVisible(boolean visible) {
        if (!visible) {
            this.optionsTabbedPane.remove(this.antialiasingPanel);
        } else {
            this.optionsTabbedPane.add(this.antialiasingPanel);
        }
    }

    public void setCameraVisible(Map3DViewer.Camera camera, boolean visible) {
        String cameraName = Map3DViewer.camera(camera);
        if (visible) {
            this.cameraComboBox.addItem(cameraName);
        } else {
            this.cameraComboBox.removeItem(cameraName);
        }
    }

    public void setInfoLabelVisible(boolean visible) {
        this.infoLabel.setVisible(visible);
    }

    public boolean isInfoLabelVisible() {
        return this.infoLabel.isVisible();
    }

    private void writeGUIToModel() {
        if (updatingGUI) {
            return;
        }
        updatingGUI = true;
        try {
            // projection
            map3DViewer.setZAngle(this.rotationSlider.getValue());

            // first slider is used for different parameters
            switch (map3DViewer.getCamera()) {
                case planOblique:
                    map3DViewer.setShearYAngle(this.inclinationSlider.getValue());
                    break;
                case perspective:
                case parallelOblique:
                    map3DViewer.setXAngle(this.inclinationSlider.getValue());
                    break;
                case cylindrical:
                    float v = (float) this.inclinationSlider.getValue() / (CYL_HEIGHT_SLIDER_MAX - CYL_HEIGHT_SLIDER_MIN);
                    map3DViewer.setCylindricalHeight(v);
                    break;
                case orthogonal:
                    break;
            }
            map3DViewer.setViewDistance(this.distanceSlider.getValue() / 100f);
            map3DViewer.setViewAngle(Math.max(MIN_VIEW_ANGLE, this.viewAngleSlider.getValue()));
            map3DViewer.setShiftX(shiftXSlider.getValue() / 100f);
            map3DViewer.setShiftY(shiftYSlider.getValue() / 100f);

            // shading
            float ambient = ambientSlider.getValue() / 100f;
            float diffuse = diffuseSlider.getValue() / 100f;
            map3DViewer.setLight(ambient, diffuse);
            float azimuth = azimuthSlider.getValue();
            float zenith = 90 - elevationSlider.getValue();
            map3DViewer.setLightDirection(azimuth, zenith);

            // anti-aliasing
            map3DViewer.setAntialiasing(antialiasingCheckBox.isSelected());

        } finally {
            updatingGUI = false;
        }
    }

    private void writeModelToGUI() {

        if (updatingGUI) {
            return;
        }
        updatingGUI = true;
        try {
            // projection

            rotationSlider.setValue((int) map3DViewer.getZAngle());
            Camera camera = map3DViewer.getCamera();
            cameraComboBox.setSelectedItem(Map3DViewer.camera(camera));
            switch (camera) {
                case perspective:
                case parallelOblique:
                    inclinationSlider.setValue((int) map3DViewer.getXAngle());
                    break;
                case planOblique:
                    inclinationSlider.setValue((int) map3DViewer.getShearYAngle());
                    break;
                case orthogonal:
                    inclinationSlider.setValue((int) map3DViewer.getXAngle());
                    break;
                case cylindrical:
                    int v = Math.round(map3DViewer.getCylindricalHeight() * (CYL_HEIGHT_SLIDER_MAX - CYL_HEIGHT_SLIDER_MIN));
                    inclinationSlider.setValue(v);
                    break;
            }
            this.adjustGUIToCamera();
            distanceSlider.setValue((int) (map3DViewer.getViewDistance() * 100f));
            viewAngleSlider.setValue((int) map3DViewer.getViewAngle());
            shiftXSlider.setValue((int) (map3DViewer.getShiftX() * 100f));
            shiftYSlider.setValue((int) (map3DViewer.getShiftY() * 100f));

            // shading
            ambientSlider.setValue((int) (map3DViewer.getAmbientLight() * 100f));
            diffuseSlider.setValue((int) (map3DViewer.getDiffuseLight() * 100f));
            azimuthSlider.setValue((int) (map3DViewer.getLightAzimuth()));
            elevationSlider.setValue(90 - (int) (map3DViewer.getLightZenith()));

            // anti-aliasing
            antialiasingCheckBox.setSelected(map3DViewer.isAntialiasing());
            int antialiasingLevel = Map3DOptionsPanel.getAntialiasingLevel();
            int menuIndex = 0;
            if (antialiasingLevel == 4) {
                menuIndex = 1;
            } else if (antialiasingLevel == 8) {
                menuIndex = 2;
            }
            antialiasingComboBox.setSelectedIndex(menuIndex);

        } finally {
            updatingGUI = false;
        }

    }

    private void initHypsoSlider() {
        float[] values = new float[]{0, 0.08f, 0.24f, 0.43f, 0.69f, 0.89f};
        Color[] colors = new Color[]{
            new Color(120, 181, 141),
            new Color(124, 172, 104),
            new Color(190, 194, 107),
            new Color(212, 218, 170),
            new Color(225, 246, 244),
            new Color(255, 255, 255)
        };
        hypsoSlider.setValues(values, colors);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        fogPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        fogStartSlider = new javax.swing.JSlider();
        fogDensitySlider = new javax.swing.JSlider();
        fogCheckBox = new javax.swing.JCheckBox();
        fogColorButton = new ika.gui.ColorButton();
        hypsoPanel = new javax.swing.JPanel();
        hypsoSlider = new com.bric.swing.GradientSlider();
        {
            hypsoSlider.putClientProperty("MultiThumbSlider.indicateSelectedThumb", "true");
            hypsoSlider.putClientProperty("MultiThumbSlider.indicateComponent", "false");
            hypsoSlider.putClientProperty("GradientSlider.useBevel", "true");
            hypsoSlider.putClientProperty("GradientSlider.colorPickerIncludesOpacity","false");

            //hypsoSlider.setOrientation(GradientSlider.VERTICAL);
            //hypsoSlider.setPaintTicks(false);

            initHypsoSlider();
            hypsoSlider.setEnabled(false);
        }
        hypsoCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel hypsoLabel = new javax.swing.JLabel();
        hypsoDefaultButton = new javax.swing.JButton();
        hypsoDefaultButton.putClientProperty("JButton.buttonType", "roundRect");
        optionsTabbedPane = new javax.swing.JTabbedPane();
        javax.swing.JPanel cameraPanel = new TransparentMacPanel();
        rotationSlider = new javax.swing.JSlider();
        inclinationSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        inclinationLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        distanceSlider = new javax.swing.JSlider();
        viewAngleSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JButton cameraDefaultButton = new javax.swing.JButton();
        cameraDefaultButton.putClientProperty("JButton.buttonType", "roundRect");
        infoLabel = new javax.swing.JLabel();
        shiftYSlider = new javax.swing.JSlider();
        shiftXLabel = new javax.swing.JLabel();
        shiftYLabel = new javax.swing.JLabel();
        shiftXSlider = new javax.swing.JSlider();
        cameraComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        shadingPanel = new TransparentMacPanel();
        javax.swing.JPanel shadingPanel_ = new TransparentMacPanel();
        diffuseSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        ambientSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        elevationSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        azimuthSlider = new javax.swing.JSlider();
        javax.swing.JButton shadingDefaultButton = new javax.swing.JButton();
        shadingDefaultButton.putClientProperty("JButton.buttonType", "roundRect");
        jPanel1 = new TransparentMacPanel();
        javax.swing.JButton fogButton = new javax.swing.JButton();
        fogButton.putClientProperty("JButton.buttonType", "roundRect");
        javax.swing.JButton hypsoButton = new javax.swing.JButton();
        hypsoButton.putClientProperty("JButton.buttonType", "roundRect");
        antialiasingPanel = new TransparentMacPanel();
        antialiasingPanel1 = new TransparentMacPanel();
        antialiasingCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        antialiasingComboBox = new javax.swing.JComboBox();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();

        fogPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Haze Start");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        fogPanel.add(jLabel5, gridBagConstraints);

        jLabel10.setText("Density");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        fogPanel.add(jLabel10, gridBagConstraints);

        fogStartSlider.setMajorTickSpacing(25);
        fogStartSlider.setMinorTickSpacing(5);
        fogStartSlider.setPaintLabels(true);
        fogStartSlider.setPaintTicks(true);
        fogStartSlider.setToolTipText("The position where haze starts, relative to the size of the terrain model.");
        fogStartSlider.setValue(0);
        fogStartSlider.setEnabled(false);
        fogStartSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                fogSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        fogPanel.add(fogStartSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = fogStartSlider.createStandardLabels(25);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            fogStartSlider.setLabelTable(labels);
        }

        fogDensitySlider.setMajorTickSpacing(299);
        fogDensitySlider.setMaximum(300);
        fogDensitySlider.setMinimum(1);
        fogDensitySlider.setMinorTickSpacing(99);
        fogDensitySlider.setPaintLabels(true);
        fogDensitySlider.setPaintTicks(true);
        fogDensitySlider.setToolTipText("Haze density");
        fogDensitySlider.setValue(100);
        fogDensitySlider.setEnabled(false);
        fogDensitySlider.setInverted(true);
        fogDensitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                fogSliderChanged(evt);
            }
        });
        {
            int max = fogDensitySlider.getMaximum();
            int min = fogDensitySlider.getMinimum();
            Hashtable labelTable = new Hashtable();
            labelTable.put( new Integer( min ), new JLabel("thick") );
            labelTable.put( new Integer( max ), new JLabel("thin") );
            fogDensitySlider.setLabelTable( labelTable );
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        fogPanel.add(fogDensitySlider, gridBagConstraints);
        // change the font for slider labels. Must be done after the slider is added
        // to a parent container.
        {
            Font font = fogStartSlider.getFont();
            Hashtable<Integer,JLabel> labels;
            labels = (Hashtable<Integer,JLabel>)fogDensitySlider.getLabelTable();
            int min = fogDensitySlider.getMinimum();
            int max = fogDensitySlider.getMaximum();
            JLabel label = labels.get( new Integer( min ) );
            label.setFont( font );             // Updates the font size
            label.setSize( label.getPreferredSize() );// Updates the label size and
            // slider layout

            label = labels.get( new Integer( max ) );
            label.setFont( font );             // Updates the font size
            label.setSize( label.getPreferredSize() );// Updates the label size and
            // slider layout

        }

        fogCheckBox.setText("Enable Haze");
        fogCheckBox.setToolTipText("Enable or disable haze.");
        fogCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fogCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        fogPanel.add(fogCheckBox, gridBagConstraints);

        fogColorButton.setToolTipText("Haze color");
        fogColorButton.setColor(new java.awt.Color(255, 255, 255));
        fogColorButton.setColorChooserTitle("Choose a Haze Color");
        fogColorButton.setEnabled(false);
        fogColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fogColorButtonActionPerformed(evt);
            }
        });
        fogColorButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                fogColorButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        fogPanel.add(fogColorButton, gridBagConstraints);

        hypsoPanel.setLayout(new java.awt.GridBagLayout());

        hypsoSlider.setOrientation(1);
        hypsoSlider.setPreferredSize(new java.awt.Dimension(40, 350));
        hypsoSlider.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                hypsoSliderPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        hypsoPanel.add(hypsoSlider, gridBagConstraints);

        hypsoCheckBox.setText("Enable");
        hypsoCheckBox.setToolTipText("Enable or disable hypsometric tinting.");
        hypsoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hypsoCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        hypsoPanel.add(hypsoCheckBox, gridBagConstraints);

        hypsoLabel.setFont(hypsoLabel.getFont().deriveFont(hypsoLabel.getFont().getSize()-2f));
        hypsoLabel.setText("<html>Click on the color gradient to add thumbs.<br>Double-click a thumb to change its color.<br><br>Hypsometric tints are not possible in <br>combination with a texture image.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        hypsoPanel.add(hypsoLabel, gridBagConstraints);

        hypsoDefaultButton.setText("Default");
        hypsoDefaultButton.setEnabled(false);
        hypsoDefaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hypsoDefaultButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        hypsoPanel.add(hypsoDefaultButton, gridBagConstraints);

        setLayout(new java.awt.GridBagLayout());

        cameraPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 1));
        cameraPanel.setLayout(new java.awt.GridBagLayout());

        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMaximum(180);
        rotationSlider.setMinimum(-180);
        rotationSlider.setMinorTickSpacing(15);
        rotationSlider.setPaintLabels(true);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setToolTipText("Rotate the model around the vertical axis");
        rotationSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        rotationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cameraPanel.add(rotationSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = rotationSlider.createStandardLabels(90);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            rotationSlider.setLabelTable(labels);
        }

        inclinationSlider.setMajorTickSpacing(45);
        inclinationSlider.setMaximum(INCLINATION_SLIDER_MAX);
        inclinationSlider.setMinimum(INCLINATION_SLIDER_MIN);
        inclinationSlider.setMinorTickSpacing(15);
        inclinationSlider.setPaintLabels(true);
        inclinationSlider.setPaintTicks(true);
        inclinationSlider.setName("Adjust plan oblique view angle"); // NOI18N
        inclinationSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        inclinationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cameraPanel.add(inclinationSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = inclinationSlider.createStandardLabels(45);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            inclinationSlider.setLabelTable(labels);
        }

        jLabel1.setText("Rotation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(jLabel1, gridBagConstraints);

        inclinationLabel.setText("Inclination");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(inclinationLabel, gridBagConstraints);

        jLabel3.setText("Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(jLabel3, gridBagConstraints);

        distanceSlider.setMaximum((int)(100 * Map3DViewer.MAX_DISTANCE));
        distanceSlider.setMinimum((int)(100 * Map3DViewer.MIN_DISTANCE));
        distanceSlider.setToolTipText("Zoom in or out");
        distanceSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        cameraPanel.add(distanceSlider, gridBagConstraints);

        viewAngleSlider.setMajorTickSpacing(45);
        viewAngleSlider.setMaximum(180);
        viewAngleSlider.setMinorTickSpacing(15);
        viewAngleSlider.setPaintLabels(true);
        viewAngleSlider.setPaintTicks(true);
        viewAngleSlider.setToolTipText("Telephoto or wide-angle lens.");
        viewAngleSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        viewAngleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cameraPanel.add(viewAngleSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = viewAngleSlider.createStandardLabels(45);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            viewAngleSlider.setLabelTable(labels);
        }

        jLabel4.setText("Field of View");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(jLabel4, gridBagConstraints);

        cameraDefaultButton.setText("Default");
        cameraDefaultButton.setToolTipText("Reset to default viewing parameters");
        cameraDefaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cameraDefaultButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(cameraDefaultButton, gridBagConstraints);

        infoLabel.setText("<html><small>Click and drag the mouse to rotate the view. <br>The model is moved when the shift key is <br>pressed while dragging. Use the scroll wheel<br>of the mouse to zoom.</small></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 0);
        cameraPanel.add(infoLabel, gridBagConstraints);

        shiftYSlider.setMajorTickSpacing(50);
        shiftYSlider.setMinimum(-100);
        shiftYSlider.setMinorTickSpacing(10);
        shiftYSlider.setPaintLabels(true);
        shiftYSlider.setPaintTicks(true);
        shiftYSlider.setToolTipText(SHIFTY_TOOLTIP);
        shiftYSlider.setValue(0);
        shiftYSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                shiftSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        cameraPanel.add(shiftYSlider, gridBagConstraints);

        shiftXLabel.setText("Shift X");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(shiftXLabel, gridBagConstraints);

        shiftYLabel.setText("Shift Y");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(shiftYLabel, gridBagConstraints);

        shiftXSlider.setMajorTickSpacing(50);
        shiftXSlider.setMinimum(-100);
        shiftXSlider.setMinorTickSpacing(10);
        shiftXSlider.setPaintLabels(true);
        shiftXSlider.setPaintTicks(true);
        shiftXSlider.setToolTipText(SHIFTX_TOOLTIP);
        shiftXSlider.setValue(0);
        shiftXSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                shiftSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        cameraPanel.add(shiftXSlider, gridBagConstraints);

        cameraComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Perspective View", "Parallel Oblique", "Plan Oblique", "2D Orthogonal" }));
        cameraComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cameraComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPanel.add(cameraComboBox, gridBagConstraints);

        jLabel2.setText("Camera");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPanel.add(jLabel2, gridBagConstraints);

        optionsTabbedPane.addTab("Camera", null, cameraPanel, "Projection of the 3D model");

        shadingPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        shadingPanel_.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 1));
        shadingPanel_.setLayout(new java.awt.GridBagLayout());

        diffuseSlider.setMajorTickSpacing(25);
        diffuseSlider.setMinorTickSpacing(5);
        diffuseSlider.setPaintLabels(true);
        diffuseSlider.setPaintTicks(true);
        diffuseSlider.setToolTipText("Brightness of diffuse reflection");
        diffuseSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        diffuseSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        shadingPanel_.add(diffuseSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = diffuseSlider.createStandardLabels(25);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            diffuseSlider.setLabelTable(labels);
        }

        jLabel6.setText("Diffuse Reflection");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        shadingPanel_.add(jLabel6, gridBagConstraints);

        ambientSlider.setMajorTickSpacing(25);
        ambientSlider.setMinorTickSpacing(5);
        ambientSlider.setPaintLabels(true);
        ambientSlider.setPaintTicks(true);
        ambientSlider.setToolTipText("Brightness of ambient illlumination");
        ambientSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        ambientSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        shadingPanel_.add(ambientSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = ambientSlider.createStandardLabels(25);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            ambientSlider.setLabelTable(labels);
        }

        jLabel7.setText("Ambient Light");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        shadingPanel_.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Elevation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        shadingPanel_.add(jLabel8, gridBagConstraints);

        elevationSlider.setMajorTickSpacing(45);
        elevationSlider.setMaximum(90);
        elevationSlider.setMinorTickSpacing(15);
        elevationSlider.setPaintLabels(true);
        elevationSlider.setPaintTicks(true);
        elevationSlider.setToolTipText("Elevation of illumination source above the horizon.");
        elevationSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        elevationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        shadingPanel_.add(elevationSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = elevationSlider.createStandardLabels(45);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            elevationSlider.setLabelTable(labels);
        }

        jLabel9.setText("Azimuth");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        shadingPanel_.add(jLabel9, gridBagConstraints);

        azimuthSlider.setMajorTickSpacing(90);
        azimuthSlider.setMaximum(360);
        azimuthSlider.setMinorTickSpacing(15);
        azimuthSlider.setPaintLabels(true);
        azimuthSlider.setPaintTicks(true);
        azimuthSlider.setToolTipText("Direction of illumination moving clockwise from north.");
        azimuthSlider.setPreferredSize(new java.awt.Dimension(250, 54));
        azimuthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        shadingPanel_.add(azimuthSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = azimuthSlider.createStandardLabels(90);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            azimuthSlider.setLabelTable(labels);
        }

        shadingDefaultButton.setText("Default Shading");
        shadingDefaultButton.setToolTipText("Reset to default shading parameters");
        shadingDefaultButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shadingDefaultButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        shadingPanel_.add(shadingDefaultButton, gridBagConstraints);

        fogButton.setText("Haze");
        fogButton.setToolTipText("Simulate haze in the background.");
        fogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fogButtonActionPerformed(evt);
            }
        });
        jPanel1.add(fogButton);

        hypsoButton.setText("Hypsometric Tints");
        hypsoButton.setToolTipText("Add colors depending on terrain altitude.");
        hypsoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hypsoButtonActionPerformed(evt);
            }
        });
        jPanel1.add(hypsoButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        shadingPanel_.add(jPanel1, gridBagConstraints);

        shadingPanel.add(shadingPanel_);

        optionsTabbedPane.addTab("Shading", shadingPanel);

        antialiasingPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 20));

        antialiasingPanel1.setLayout(new java.awt.GridBagLayout());

        antialiasingCheckBox.setText("Enable Anti-Aliasing");
        antialiasingCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                antialiasingCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        antialiasingPanel1.add(antialiasingCheckBox, gridBagConstraints);

        jLabel12.setText("Anti-Aliasing Quality:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        antialiasingPanel1.add(jLabel12, gridBagConstraints);

        antialiasingComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2x (Fast)", "4x", "8x (Slow)" }));
        antialiasingComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                antialiasingComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        antialiasingPanel1.add(antialiasingComboBox, gridBagConstraints);

        jLabel13.setText("<html><small>Changes to the quality setting will take effect the next time you <br>start this application.<br>Note: Not all graphics cards support high-quality anti-aliasing.</small></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        antialiasingPanel1.add(jLabel13, gridBagConstraints);

        antialiasingPanel.add(antialiasingPanel1);

        optionsTabbedPane.addTab("Anti-Aliasing", antialiasingPanel);

        add(optionsTabbedPane, new java.awt.GridBagConstraints());
    }// </editor-fold>//GEN-END:initComponents

    private void adjustGUIToCamera() {
        switch (map3DViewer.getCamera()) {
            case perspective:
                inclinationSlider.setMaximum(INCLINATION_SLIDER_MAX);
                inclinationSlider.setMinimum(INCLINATION_SLIDER_MIN);
                inclinationSlider.setEnabled(true);
                viewAngleSlider.setEnabled(true);
                inclinationSlider.setToolTipText("Tilt the model.");
                break;
            case parallelOblique:
                inclinationSlider.setMaximum(INCLINATION_SLIDER_MAX);
                inclinationSlider.setMinimum(INCLINATION_SLIDER_MIN);
                inclinationSlider.setEnabled(true);
                viewAngleSlider.setEnabled(false);
                inclinationSlider.setToolTipText("Foreshorten the view in vertical direction.");
                break;
            case planOblique:
                inclinationSlider.setMaximum(SHEAR_SLIDER_MAX);
                inclinationSlider.setMinimum(SHEAR_SLIDER_MIN);
                inclinationSlider.setEnabled(true);
                viewAngleSlider.setEnabled(false);
                inclinationSlider.setToolTipText("Adjust the plan oblique angle.");
                break;
            case orthogonal:
                inclinationSlider.setMaximum(INCLINATION_SLIDER_MAX);
                inclinationSlider.setMinimum(INCLINATION_SLIDER_MIN);
                inclinationSlider.setEnabled(false);
                viewAngleSlider.setEnabled(false);
                inclinationSlider.setToolTipText("");
                break;
            case cylindrical:
                inclinationSlider.setMaximum(CYL_HEIGHT_SLIDER_MAX);
                inclinationSlider.setMinimum(CYL_HEIGHT_SLIDER_MIN);
                inclinationSlider.setEnabled(true);
                viewAngleSlider.setEnabled(true);
                inclinationSlider.setToolTipText("Height of the cylindrical camera.");
                inclinationLabel.setText("Height");
                shiftXLabel.setText("Camera X");
                shiftYLabel.setText("Camera Y");
                shiftXSlider.setToolTipText(CYLINDER_POSX_TOOLTIP);
                shiftYSlider.setToolTipText(CYLINDER_POSY_TOOLTIP);
                break;
        }

        boolean cylindricalCamera = map3DViewer.getCamera() == Camera.cylindrical;
        inclinationSlider.setPaintLabels(!cylindricalCamera);
        inclinationSlider.setPaintTicks(!cylindricalCamera);
        if (!cylindricalCamera) {
            inclinationLabel.setText("Inclination");
            shiftXLabel.setText("Shift X");
            shiftYLabel.setText("Shift Y");
            shiftXSlider.setToolTipText(SHIFTX_TOOLTIP);
            shiftYSlider.setToolTipText(SHIFTY_TOOLTIP);
        }
    }

private void fogCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fogCheckBoxActionPerformed
    this.map3DViewer.setFogEnabled(fogCheckBox.isSelected());
    this.fogStartSlider.setEnabled(fogCheckBox.isSelected());
    this.fogDensitySlider.setEnabled(fogCheckBox.isSelected());
    this.fogColorButton.setEnabled(fogCheckBox.isSelected());
}//GEN-LAST:event_fogCheckBoxActionPerformed

private void fogSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_fogSliderChanged
    float fogStart = fogStartSlider.getValue() / 100f;
    float density = fogDensitySlider.getValue() / 100f;
    float fogEnd = fogStart + density;
    this.map3DViewer.setFogStart(fogStart);
    this.map3DViewer.setFogEnd(fogEnd);
}//GEN-LAST:event_fogSliderChanged

private void fogColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fogColorButtonActionPerformed
    Color fogColor = fogColorButton.getColor();
    this.map3DViewer.setFogColor(fogColor);
}//GEN-LAST:event_fogColorButtonActionPerformed

private void fogColorButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_fogColorButtonPropertyChange
    if (ColorPicker.SELECTED_COLOR_PROPERTY.equals(evt.getPropertyName())) {
        this.map3DViewer.setFogColor((Color) evt.getNewValue());
    }
}//GEN-LAST:event_fogColorButtonPropertyChange

private void hypsoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypsoCheckBoxActionPerformed
    hypsoSlider.setEnabled(hypsoCheckBox.isSelected());
    hypsoDefaultButton.setEnabled(hypsoCheckBox.isSelected());
    adjustHypsometricTintingVisibility();
}//GEN-LAST:event_hypsoCheckBoxActionPerformed

    private void adjustHypsometricTintingVisibility() {
        if (hypsoCheckBox.isSelected()) {
            this.readHypsometricTints();
        } else {
            map3DViewer.clearTextureImage();
        }
    }

    private void readHypsometricTints() {
        BufferedImage image = new BufferedImage(256, 1, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 256; i++) {
            // GradientSlider returns null for 0 and 1. Rescale to get around this bug.
            // float t = i / 255f;
            float t = (1 + 253f / 255f * i) / 255f;
            Color color = (Color) hypsoSlider.getValue(t);
            image.setRGB(i, 0, color.getRGB());
        }
        /*
         System.out.println("colors");
         Color colors[] = hypsoSlider.getColors();
         for (int i = 0; i < colors.length; i++) {
         Color c = colors[i];
         System.out.println(c.getRed() + " " + c.getBlue() + " " + c.getGreen());
         }

         System.out.println("positions");
         float values[] = hypsoSlider.getThumbPositions();
         for (int i = 0; i < values.length; i++) {
         System.out.println(values[i]);
         }
         */
        map3DViewer.setTextureImage(image);
    }

    public void showHypsometricDialog() {
        hypsoCheckBox.setSelected(map3DViewer.hasTexture() && map3DViewer.isTexture1D());
        JOptionPane.showOptionDialog(this,
                hypsoPanel,
                "Hypsometric Tints",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, null, null);
        adjustHypsometricTintingVisibility();
    }

private void hypsoSliderPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_hypsoSliderPropertyChange

    String propName = evt.getPropertyName();
    if (MultiThumbSlider.VALUES_PROPERTY.equals(propName) // thumb position
            || MultiThumbSlider.ADJUST_PROPERTY.equals(propName) // color
            ) {
        if (!hypsoSlider.isValueAdjusting()) {
            this.readHypsometricTints();
        }
    }

}//GEN-LAST:event_hypsoSliderPropertyChange

private void hypsoDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypsoDefaultButtonActionPerformed
    this.initHypsoSlider();
}//GEN-LAST:event_hypsoDefaultButtonActionPerformed

    private void antialiasingComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_antialiasingComboBoxItemStateChanged
        if (!updatingGUI && evt.getStateChange() == ItemEvent.SELECTED) {
            int level = this.antialiasingComboBox.getSelectedIndex();
            int antialiasingLevel = 2;
            for (int i = 0; i < level; i++) {
                antialiasingLevel *= 2;
            }
            Map3DOptionsPanel.setAntialiasingLevel(antialiasingLevel);
        }
    }//GEN-LAST:event_antialiasingComboBoxItemStateChanged

    /**
     * Switch between 2D and 3D viewing mode.
     *
     * @param evt
     */
    private void antialiasingCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_antialiasingCheckBoxActionPerformed
        if (this.updatingGUI) {
            return;
        }
        this.writeGUIToModel();
        map3DViewer.display();
    }//GEN-LAST:event_antialiasingCheckBoxActionPerformed

    private void hypsoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypsoButtonActionPerformed
        showHypsometricDialog();
    }//GEN-LAST:event_hypsoButtonActionPerformed

    private void fogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fogButtonActionPerformed
        fogCheckBox.setSelected(map3DViewer.isFogEnabled());
        float fogStart = map3DViewer.getFogStart();
        float fogEnd = map3DViewer.getFogEnd();
        float fogDensity = fogEnd - fogStart;
        fogStartSlider.setValue(Math.round(fogStart * 100));
        fogDensitySlider.setValue(Math.round(fogDensity * 100));
        fogColorButton.setColor(map3DViewer.getFogColor());

        JOptionPane.showOptionDialog(this,
            fogPanel,
            "Haze",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, null, null);
    }//GEN-LAST:event_fogButtonActionPerformed

    private void shadingDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shadingDefaultButtonActionPerformed
        map3DViewer.defaultShading();
        updatingGUI = true;
        try {
            ambientSlider.setValue((int) (map3DViewer.getAmbientLight() * 100));
            diffuseSlider.setValue((int) (map3DViewer.getDiffuseLight() * 100));
            azimuthSlider.setValue((int) map3DViewer.getLightAzimuth());
            elevationSlider.setValue(90 - (int) map3DViewer.getLightZenith());
        } finally {
            updatingGUI = false;
        }
        map3DViewer.display();
    }//GEN-LAST:event_shadingDefaultButtonActionPerformed

    private void sliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderChanged
        this.writeGUIToModel();
        map3DViewer.display();
    }//GEN-LAST:event_sliderChanged

    private void cameraComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cameraComboBoxItemStateChanged

        if (evt.getStateChange() != ItemEvent.SELECTED || this.updatingGUI) {
            return;
        }

        this.updatingGUI = true;
        try {
            String name = (String) cameraComboBox.getSelectedItem();
            this.map3DViewer.setCamera(Map3DViewer.camera(name));
            this.adjustGUIToCamera();
            this.map3DViewer.display();
        } finally {
            this.updatingGUI = false;
        }
    }//GEN-LAST:event_cameraComboBoxItemStateChanged

    private void shiftSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shiftSliderStateChanged

        // The grid must potentially be changed when the location of the
        // cylindrical camera changes. So only do the change once, when the slider
        // is released.
        boolean cylindricalCamera = map3DViewer.getCamera() == Camera.cylindrical;
        if (cylindricalCamera && ((JSlider) evt.getSource()).getValueIsAdjusting()) {
            return;
        }

        writeGUIToModel();
        map3DViewer.display();
    }//GEN-LAST:event_shiftSliderStateChanged

    private void cameraDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cameraDefaultButtonActionPerformed
        map3DViewer.resetToDefaultCamera();
        writeModelToGUI();
        map3DViewer.display();
    }//GEN-LAST:event_cameraDefaultButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider ambientSlider;
    private javax.swing.JCheckBox antialiasingCheckBox;
    private javax.swing.JComboBox antialiasingComboBox;
    private javax.swing.JPanel antialiasingPanel;
    private javax.swing.JPanel antialiasingPanel1;
    private javax.swing.JSlider azimuthSlider;
    private javax.swing.JComboBox cameraComboBox;
    private javax.swing.JSlider diffuseSlider;
    private javax.swing.JSlider distanceSlider;
    private javax.swing.JSlider elevationSlider;
    private javax.swing.JCheckBox fogCheckBox;
    private ika.gui.ColorButton fogColorButton;
    private javax.swing.JSlider fogDensitySlider;
    private javax.swing.JPanel fogPanel;
    private javax.swing.JSlider fogStartSlider;
    private javax.swing.JCheckBox hypsoCheckBox;
    private javax.swing.JButton hypsoDefaultButton;
    private javax.swing.JPanel hypsoPanel;
    private com.bric.swing.GradientSlider hypsoSlider;
    private javax.swing.JLabel inclinationLabel;
    private javax.swing.JSlider inclinationSlider;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane optionsTabbedPane;
    private javax.swing.JSlider rotationSlider;
    private javax.swing.JPanel shadingPanel;
    private javax.swing.JLabel shiftXLabel;
    private javax.swing.JSlider shiftXSlider;
    private javax.swing.JLabel shiftYLabel;
    private javax.swing.JSlider shiftYSlider;
    private javax.swing.JSlider viewAngleSlider;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.writeModelToGUI();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        this.map3DViewer.getAnimation().keyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        this.map3DViewer.getAnimation().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        this.map3DViewer.getAnimation().keyReleased(e);
    }

    public static void setAntialiasingLevel(int antialiasLevel) {
        Preferences prefs = Preferences.userRoot().node(prefsNodeName);
        prefs.putInt(prefsAntialiasingLevel, antialiasLevel);
    }

    public static int getAntialiasingLevel() {
        Preferences prefs = Preferences.userRoot().node(prefsNodeName);
        return prefs.getInt(prefsAntialiasingLevel, 2);
    }
}
