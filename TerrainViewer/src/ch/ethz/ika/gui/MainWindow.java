/*
 * ImageFrame.java
 *
 * Created on June 1, 2008, 4:28 PM
 */
package ch.ethz.ika.gui;

import ch.ethz.ika.terrainviewer.ShearPanel;
import ch.ethz.karto.map3d.Map3DGLCapabilities;
import ch.ethz.karto.map3d.Map3DTexture;
import ch.ethz.karto.map3d.gui.Map3DOptionsPanel;
import ch.ethz.karto.map3d.Map3DViewer;
import ch.ethz.karto.map3d.Map3DViewer.Camera;
import ch.ethz.karto.map3d.Map3DTexture1DMapper;
import ch.ethz.karto.swa.atlas.SystemInfoGPU;
import com.bric.swing.ColorPicker;
import com.bric.swing.MultiThumbSlider;
import com.fizzysoft.sdu.RecentDocumentsManager;
import ika.geo.GeoGrid;
import ika.geo.GeoImage;
import ika.geo.GeoObject;
import ika.geo.grid.GaussianPyramid;
import ika.geo.grid.GridGaussLowPassOperator;
import ika.geo.grid.GridScaleOperator;
import ika.geoimport.EsriASCIIGridReader;
import ika.geoimport.GeoImporter;
import ika.geoimport.ImageImporter;
import ika.geoimport.SynchroneDataReceiver;
import ika.gui.SwingWorkerWithProgressIndicator;
import ika.gui.TransparentMacPanel;
import ika.utils.CatmullRomSpline;
import ika.utils.ErrorDialog;
import ika.utils.FileUtils;
import ika.utils.ImageUtils;
import ika.utils.MathUtils;
import ika.utils.PropertiesLoader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import net.roydesign.mac.MRJAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.media.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Main window.
 * @author  Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class MainWindow extends javax.swing.JFrame implements
        PropertyChangeListener, ComponentListener {

    /**
     * smallest side length of the grid used for previewing
     */
    private static final int MIN_GRID_CELLS = 64 * 64;
    private static final int MAX_GRID_SIDE = 6000;
    private static final int DEFAULT_GRID_SIDE = 1024;
    /**
     * maximum side length of a texture image
     */
    private static final int MAX_TEXTURE_SIDE = 2048;

    private static final boolean VERBOSE = false;

    /**
     * OpenGL handler
     */
    private Map3DViewer map3DViewer;
    /**
     * information string for the full resolution grid
     */
    private String originalInfoText;
    /**
     * A pyramid of grid levels.
     */
    private GaussianPyramid gridPyramid;

    /**
     * dimension of the window when minimized.
     */
    private java.awt.Dimension packedSize = null;
    private boolean updatingGUI = false;

    private RecentDocumentsManager rdm;

    /**
     * multiplication sign surrounded by small spaces
     */
    private static final String MULT_SIGN = "\u2006\u00D7\u2006";

    private static final String APPNAME;
    static {
        java.util.Properties props = PropertiesLoader.loadProperties("ika.app.Application");
        APPNAME = props.getProperty("ApplicationName");
    }

   
    public MainWindow() throws IOException {
        initRecentDocumentsMenu(this);
        initComponents();
        initMenus();
        
        // setup 3D map
        
               
        this.map3DViewer = new Map3DViewer(Map3DViewer.GLComponentType.GL_AWT, GLProfile.getDefault());
        this.map3DViewer.setDefaultCamera(75, 0, Map3DViewer.MAX_DISTANCE, 50, 0, 0);
        this.map3DViewer.resetToDefaultCamera();

        // setup GUI controls of 3D map
        /*this.map3DOptionsPanel.setCameraVisible(Map3DViewer.Camera.orthogonal, true);
        this.map3DOptionsPanel.setCameraVisible(Map3DViewer.Camera.parallelOblique, true);
        this.map3DOptionsPanel.setCameraVisible(Map3DViewer.Camera.planOblique, true);
        this.map3DOptionsPanel.setCameraVisible(Map3DViewer.Camera.cylindrical, true);
        */
        //this.map3DOptionsPanel.setAntialiasingPanelVisible(false);

        // add 3D map to GUI
        Component map3DComponent = map3DViewer.getComponent();
        map3DComponent.setPreferredSize(new Dimension(400, 400));
        this.container3D.add(map3DComponent, BorderLayout.CENTER);
        map3DComponent.addPropertyChangeListener(this);
        this.map3DOptionsPanel.setMap3DViewer(this.map3DViewer);

        this.map3DOptionsPanel.addPanel("Level of Detail", lodPanel);
        this.map3DOptionsPanel.setCameraVisible(Map3DViewer.Camera.cylindrical, true);

        // pack the window (= bring it to its smallest possible size)
        // and store this minimum size
        pack();
        packedSize = getSize();

        // register this as a ComponentListener to get resize events.
        // the resize-event-handler makes sure the window is not gettting too small.
        this.addComponentListener(this);
               
    }

    private void initRecentDocumentsMenu(final Component parent) {
        rdm = new RecentDocumentsManager() {

            private Preferences getPreferences() {
                return Preferences.userNodeForPackage(MainWindow.class);
            }

            @Override
            protected byte[] readRecentDocs() {
                return getPreferences().getByteArray("RecentDocuments"+APPNAME, null);
            }

            @Override
            protected void writeRecentDocs(byte[] data) {
                getPreferences().putByteArray("RecentDocuments"+APPNAME, data);
            }

            @Override
            protected void openFile(File file, ActionEvent event) {
                try {
                    if (file != null) {
                        openDEM(file.getCanonicalPath());
                    }
                } catch (IOException ex) {
                    String msg = "Could not open the terrain model.";
                    String title = APPNAME + " Error";
                    ErrorDialog.showErrorDialog(msg, title, ex, parent);
                }
            }
        };
    }

    private GeoGrid getFullResolutionGrid() {
        if (gridPyramid == null) {
            return null;
        }
        return gridPyramid.getFullResolutionLevel();
    }

    /**
     * Returns the grid that should be displayed in the 3D view
     * @return the displayGrid
     */
    private GeoGrid getDisplayGrid() {
        if (gridPyramid == null) {
            return null;
        }
        return gridPyramid.getLevel(this.getDisplayLevel());
    }

    /**
     * Returns the pyramid level that should be displayed as selected in the GUI.
     * @return
     */
    private int getDisplayLevel() {
        int buttonCount = lodButtonPanel.getComponentCount();
        for (int i = 0; i < buttonCount; i++) {
            JRadioButton b = (JRadioButton)lodButtonPanel.getComponent(i);
            if (b.isSelected()) {
                return gridPyramid.getLevelsCount() - i - 1;
            }
        }
        return 0;
    }

    /**
     * Returns the pyramid level that is initially displayed.
     * @return
     */
    private int getDefaultDisplayLevel() {
        int lastLevel = gridPyramid.getLevelsCount() - 1;
        for (int i = lastLevel; i >= 0; i--) {
            GeoGrid grid = gridPyramid.getLevel(i);
            int cols = grid.getCols();
            int rows = grid.getRows();
            if (cols * rows >= DEFAULT_GRID_SIDE * DEFAULT_GRID_SIDE) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Setup the panel for selecting the level of details.
     */
    private void initLevelOfDetailGUI() {

        // find firstLevel, i.e. the ID of the largest pyramid level that can be
        // reasonable displayed
        int firstLevel, lastLevel = gridPyramid.getLevelsCount() - 1;
        for (firstLevel = 0; firstLevel <= lastLevel; firstLevel++) {
            GeoGrid grid = gridPyramid.getLevel(firstLevel);
            int cols = grid.getCols();
            int rows = grid.getRows();
            if (cols * rows <= MAX_GRID_SIDE * MAX_GRID_SIDE) {
                break;
            }
        }

        // find the default pyramid level to display
        int displayLevel = getDefaultDisplayLevel();
        
        // add radio buttons to GUI
        lodButtonPanel.removeAll();
        DecimalFormat df = new DecimalFormat("#,##0");
        for (int i = lastLevel; i >= firstLevel; i--) {
            JRadioButton b = new JRadioButton();

            // generate label for radio button
            GeoGrid grid = gridPyramid.getLevel(i);
            StringBuilder sb = new StringBuilder();
            sb.append(df.format(grid.getCols()));
            sb.append(MULT_SIGN);
            sb.append(df.format(grid.getRows()));

            // generate tooltip for radio button
            String tooltip = "Preview with " + sb.toString() + " pixels.";
            if (lastLevel - firstLevel == 0) {
                tooltip += " Disabled because the model is small.";
            }
            b.setToolTipText(tooltip);

            // finish label
            if (i == 0) {
                sb.append(" (full resolution)");
            } else if (i == lastLevel) {
                sb.append(" (fastest)");
            }
            b.setText(sb.toString());

            // add event listener
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (((JRadioButton)e.getSource()).isSelected()) {
                        modelChanged();
                    }
                }
            });
            
            lodButtonGroup.add(b);
            lodButtonPanel.add(b);

            // select the button of the currend display level (after adding it
            // to the button group)
            b.setSelected(displayLevel == i);

            // disable radio button if there is only one button
            b.setEnabled(lastLevel - firstLevel >= 1);
        }

        if (firstLevel != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Full size: ");
            GeoGrid bigGrid = getFullResolutionGrid();
            sb.append(df.format(bigGrid.getCols()));
            sb.append(MULT_SIGN);
            sb.append(df.format(bigGrid.getRows()));
            JLabel label = new JLabel(sb.toString());
            label.setFont(label.getFont().deriveFont(11f));
            lodButtonPanel.add(label);
        }
        lodButtonPanel.validate();

    }

    
    private void openDEM(final String filePath) {

        if (!EsriASCIIGridReader.canRead(filePath)) {
            String msg = "The selected file cannot be read.";
            String title = "Error";
            ErrorDialog.showErrorDialog(msg, title, null, this);
            return;
        }

        // release previous grid to free memory
        gridPyramid = null;
       

        SwingWorkerWithProgressIndicator worker;
        worker = new SwingWorkerWithProgressIndicator<GaussianPyramid>(
                this, APPNAME + " Data Import", "", true) {

            @Override
            public void done() {
                try {
                    updatingGUI = true;
                    
                    gridPyramid = get();
                    originalInfoText = getFullResolutionGrid().toStringWithStatistics("<br>");

                    // set title of window
                    String name = getFullResolutionGrid().getName();
                    if (name != null && name.trim().length() > 0) {
                        setTitle(name.trim());
                    }

                    initLevelOfDetailGUI();
                  
                    rdm.addDocument(new File(filePath), null);
                    updatingGUI = false;
                    modelChanged();
                } catch (Exception ex) {
                    String exmsg = ex.getMessage();
                    if (exmsg != null && exmsg.contains("user canceled")) {
                        return;
                    }
                    ex.printStackTrace();

                    String msg = "An error occured";
                    String title = APPNAME + " Error";
                    ika.utils.ErrorDialog.showErrorDialog(msg, title, ex, MainWindow.this);
                    return;
                } finally {
                    updatingGUI = false;
                    this.completeProgress();
                }
            }

            @Override
            protected GaussianPyramid doInBackground() throws Exception {

                this.start();

                // read sourceGrid from file
                GeoGrid newGrid = ika.geoimport.EsriASCIIGridReader.read(filePath, this);
                if (this.isAborted()) {
                    throw new IllegalStateException("user canceled");
                }

                this.setIndeterminate(true);
                this.setMessage("Downsampling terrain model for display");
                return new GaussianPyramid(newGrid, 9999, MIN_GRID_CELLS);
            }
        };

        worker.setMaxTimeWithoutDialog(1);
        worker.setMessage("Reading terrain model \"" + FileUtils.getFileName(filePath) + "\"");
        worker.execute();
    }

    /**
     * Load and init GUI with DEM
     */
    public void openDEM() {

        String inputGridPath = FileUtils.askFile(null, "Select an ESRI ASCII Grid", true);
        if (inputGridPath == null) {
            return; // user canceled
        }
        openDEM(inputGridPath);
    }

    private void openTexture(final String filePath) {

        try {
            java.net.URL url = ika.utils.URLUtils.filePathToURL(filePath);
            Dimension dim = ImageImporter.getDimensions(url);

            // test for non power of two support
            if (!Map3DGLCapabilities.hasNonPowerOfTwoTextures()
                    && (!MathUtils.isPower2(dim.width)
                    || !MathUtils.isPower2(dim.height))) {
                String msg = "Your graphics card only supports texture images with\n"
                        + "power of two dimensions (e.g. 1024 x 1024 pixels).\n"
                        + "The selected image has " + dim.width
                        + " x " + dim.height + " pixels\n"
                        + "and therefore cannot be used.";
                String title = "";
                JOptionPane.showMessageDialog(this, msg, title,
                        JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            // make sure the image is not too large
            int maxSize = Map3DTexture.getMaxTextureSize();
            if (dim.width > maxSize || dim.height > maxSize) {
                String msg = "<html>The selected texture image is too large " +
                        "and cannot be displayed. <br>The maximum texture size is " +
                        maxSize + " x " + maxSize + " pixels.</html>";
                String title = "Large Texture Image";
                JOptionPane.showMessageDialog(this, msg, title,
                        JOptionPane.ERROR_MESSAGE, null);
                return;
            }
        } catch (Exception exc) {
            String msg = "The texture image could not be opened.";
            String title = APPNAME + " Error";
            ErrorDialog.showErrorDialog(msg, title, exc, this);
            exc.printStackTrace();
            return;
        }

        final JFrame frame = this;
        SwingWorkerWithProgressIndicator worker;
        worker = new SwingWorkerWithProgressIndicator<BufferedImage>(
                this, APPNAME + " Texture Import", "", true) {

            @Override
            public void done() {
                try {

                    try {
                        BufferedImage t = get(); // tests for exceptions
                        map3DViewer.setTextureImage(t);
                    } catch (Exception ex) {
                        String exmsg = ex.getMessage();
                        if (exmsg != null && exmsg.contains("user canceled")) {
                            return;
                        }

                        String msg = "The texture image could not be opened.";
                        String title = APPNAME + " Error";
                        ika.utils.ErrorDialog.showErrorDialog(msg, title, ex, frame);
                        return;
                    }


                } finally {
                    modelChanged();
                    this.completeProgress();
                }
            }

            @Override
            protected BufferedImage doInBackground() throws Exception {

                this.setProgress(0);
                ImageImporter importer = new ImageImporter();
                importer.setProgressIndicator(this);
                importer.setOptimizeForDisplay(false);
                java.net.URL url = ika.utils.URLUtils.filePathToURL(filePath);
                SynchroneDataReceiver dataReceiver = new SynchroneDataReceiver();
                dataReceiver.setShowMessageOnError(false);
                importer.read(url, dataReceiver, GeoImporter.SAME_THREAD);

                // test whether the image has been successfully read
                if (dataReceiver.hasReceivedError()) {
                    throw new IOException("Could not read image file at " + filePath);
                }

                // retrieve the image
                GeoImage geoImage = (GeoImage) dataReceiver.getImportedData();
                if (geoImage == null) {
                    return null; // user canceled
                }

                BufferedImage t = geoImage.getBufferedImage();
                if (this.isAborted()) {
                    throw new IllegalStateException("user canceled");
                }

                this.setIndeterminate(true);

                while (t.getWidth() * t.getHeight() > MAX_TEXTURE_SIDE * MAX_TEXTURE_SIDE) {
                    int w = t.getWidth() / 2;
                    int h = t.getHeight() / 2;
                    Object hint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                    int imageType = BufferedImage.TYPE_INT_ARGB;
                    t = ImageUtils.getFasterScaledInstance(t, w, h, hint, imageType, false);
                    if (this.isAborted()) {
                        throw new IllegalStateException("user canceled");
                    }
                }

                return t;
            }
        };

        worker.setMaxTimeWithoutDialog(1);
        worker.setMessage("Reading Texture \"" + FileUtils.getFileName(filePath) + "\"");
        worker.execute();
    }

    /** 
     * Mac OS X and Windows specific initialization of the menus
     */
    private void initMenus() {
        if (ika.utils.Sys.isMacOSX()) {

            // remove exit menu item on Mac OS X
            this.fileMenu.remove(this.exitMenuSeparator);
            this.fileMenu.remove(this.exitMenuItem);
            this.fileMenu.validate();

            // remove window info menu item on Mac OS X
            this.menuBar.remove(this.winHelpMenu);

            /*
            // remove preferences menu item on Mac OS X
            this.editMenu.remove(this.preferencesSeparator);
            this.editMenu.remove(this.preferencesMenuItem);
            this.editMenu.validate();
             */

            // setup about command in apple menu
            final JFrame owner = this;
            MRJAdapter.addAboutListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    ika.gui.ProgramInfoPanel.showApplicationInfo(owner);
                }
            });

            // setup quit command in apple menu
            MRJAdapter.addQuitApplicationListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    askToCloseWindowAndExit();
                }
            });
        } else if (ika.utils.Sys.isWindows()) {
            this.menuBar.remove(macHelpMenu);
        }

        this.menuBar.validate();
    }

    private void noTerrainErrorMessage(JFrame frame) {
        String msg = "<html>There is no terrain model loaded.<br>" +
                "Please first open one.";
        String title = APPNAME + " Error";
        JOptionPane.showMessageDialog(frame, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a dialog with information about the original sourceGrid and the
     * current bending.
     */
    public void showGridInfo() {

        try {
            
            if (originalInfoText == null || getDisplayGrid() == null) {
                noTerrainErrorMessage(this);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<html> <b>");
            sb.append("Terrain Model");
            sb.append("</b><br><br>");

            sb.append(this.originalInfoText);

            String title = "Terrain Model Info";
            JOptionPane.showMessageDialog(this, sb.toString(), title, JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception exc) {
            String msg = "An error occured.";
            String title = APPNAME + " Error";
            ErrorDialog.showErrorDialog(msg, title, exc, this);
        }
    }

    /**
     * Returns whether the grid seems to be in geographical coordinates, i.e.
     * cell size is in degrees and not in meters.
     * @return
     */
    private boolean isGeographicCoordinateSystem() {
        GeoGrid grid = this.getFullResolutionGrid();
        return grid == null ? false : grid.getCellSize() < 0.1;
    }

    /**
     * Returns a cell size. Computes a projected cell size if the grid is not
     * projected.
     * @param resampledGrid
     * @return
     */
    private double getProjectedCellSize(GeoGrid resampledGrid) {

        double downsampledCellSize = resampledGrid.getCellSize();

        // the following test of the cell size must be done with the original
        // sourceGrid. Very large geographic grids that have been resampled to a small
        // size for display might end up with relatively large cell sizes.
        if (isGeographicCoordinateSystem()) {
            double lat = (getFullResolutionGrid().getNorth() + getFullResolutionGrid().getSouth()) / 2;
            lat = Math.toRadians(lat);
            double R = 6371000;
            downsampledCellSize = R * Math.toRadians(downsampledCellSize) * Math.cos(lat);
        }
        return downsampledCellSize;
    }

    /**
     * Converts to model to a string that can be written to a file for storing.
     * @return
     */
    @Override
    public String toString() {
        DecimalFormat formatter = new DecimalFormat("##0.####");
        DecimalFormatSymbols dfs = formatter.getDecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        formatter.setDecimalFormatSymbols(dfs);
        String lineSep = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append("-");
        sb.append(lineSep);
        // FIXME add the model here

        return sb.toString();
    }

    /**
     * Restores the settings from a string that has been generated by toString
     * @param string
     * @throws java.io.IOException
     */
    public void fromString(String string) throws IOException {

        try {
            updatingGUI = true;

            // make sure we have the right file format
            if (!string.startsWith("-")) {
                throw new IOException("Not a supported format");
            }

            StringTokenizer tokenizer = new StringTokenizer(string, "\n\r");

            // overread -
            tokenizer.nextToken();
            // FIXME read the model here

        } finally {
            updatingGUI = false;
        }
    }

    /**
     * Called whenever the model changes.
     */
    protected void modelChanged() {

        try {
            if (getDisplayGrid() == null || updatingGUI) {
                return;
            }

            getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);

            long modelStartTime = System.currentTimeMillis();

            // FIXME change grid according to model
            GeoGrid grid = getDisplayGrid();

            long modelEndTime = System.currentTimeMillis();

            long openGLStartTime = System.currentTimeMillis();
            if (this.map3DViewer != null) {
                double cellSize = getProjectedCellSize(grid);
                Map3DTexture1DMapper mapper = new Map3DTexture1DMapper();               
                map3DViewer.setModel(grid.getGrid(), (float) cellSize, mapper);
            }

            if (VERBOSE) {
                long endTime = System.currentTimeMillis();
                if (grid != null) {
                    System.out.println("Model Changed (" + grid.getCols() + " x " + grid.getRows() + ")");
                }
                System.out.println("Bending:  " + (modelEndTime - modelStartTime) + " ms");
                System.out.println("OpenGL: " + (endTime - openGLStartTime) + " ms");
                System.out.println("Total:  " + (endTime - modelStartTime) + " ms");
                System.out.println();
            }

        } catch (Exception exc) {
            String msg = "An error occured.";
            String title = APPNAME + " Error";
            ErrorDialog.showErrorDialog(msg, title, exc, this);
        }
    }
            
    /**
     * Displays a dialog that asks the user whether the application should
     * be closed and does so if the user confirms.
     */
    private void askToCloseWindowAndExit() {
        if (this.getFullResolutionGrid() == null) {
            System.exit(0);
        }

        Map3DOptionsPanel.hide3DOptions();
        int res = JOptionPane.showConfirmDialog(this,
                "<html>This will quit " + APPNAME + ". <br>" +
                "Unsaved settings will be lost.</html>",
                "Close " + APPNAME,
                JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            this.dispose();
            System.exit(0);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lodPanel = new TransparentMacPanel();
        lodPanel_ = new TransparentMacPanel();
        jLabel6 = new javax.swing.JLabel();
        lodInfoTextArea = new javax.swing.JTextArea();
        lodButtonPanel = new TransparentMacPanel();
        lodButtonGroup = new javax.swing.ButtonGroup();
        scaleTerrainPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        scaleTerrainFormattedTextField = new javax.swing.JFormattedTextField();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        cameraPositionPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        cameraXFormattedTextField = new javax.swing.JFormattedTextField();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        cameraYFormattedTextField = new javax.swing.JFormattedTextField();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        eastLabel = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        southLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        northLabel = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        westLabel = new javax.swing.JLabel();
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
        container3D = new javax.swing.JPanel();
        eastPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        map3DOptionsPanel = new ch.ethz.karto.map3d.gui.Map3DOptionsPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openDEMMenuItem = new javax.swing.JMenuItem();
        openRecentMenu = rdm.createOpenRecentMenu();
        saveDEMMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        openTextureMenuItem = new javax.swing.JMenuItem();
        clearTextureMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        saveSettingsMenuItem = new javax.swing.JMenuItem();
        openSettingsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        export3DViewMenuItem = new javax.swing.JMenuItem();
        exitMenuSeparator = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        gridInfoMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        scaleTerrainMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        cameraPositionMenuItem = new javax.swing.JMenuItem();
        extrasMenu = new javax.swing.JMenu();
        highResMenuItem = new javax.swing.JCheckBoxMenuItem();
        bindLightDirectionMenuItem = new javax.swing.JCheckBoxMenuItem();
        backgroundColorMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        animationMenuItem = new javax.swing.JMenuItem();
        macHelpMenu = new javax.swing.JMenu();
        systemInfoMenuItem = new javax.swing.JMenuItem();
        winHelpMenu = new javax.swing.JMenu();
        infoMenuItem = new javax.swing.JMenuItem();
        winSystemInfoMenuItem = new javax.swing.JMenuItem();

        lodPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 12));

        lodPanel_.setLayout(new java.awt.GridBagLayout());

        jLabel6.setText("Level of Detail for Preview");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        lodPanel_.add(jLabel6, gridBagConstraints);

        lodInfoTextArea.setColumns(20);
        lodInfoTextArea.setEditable(false);
        lodInfoTextArea.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        lodInfoTextArea.setLineWrap(true);
        lodInfoTextArea.setRows(2);
        lodInfoTextArea.setText("For export the full resolution is used.");
        lodInfoTextArea.setWrapStyleWord(true);
        lodInfoTextArea.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        lodPanel_.add(lodInfoTextArea, gridBagConstraints);

        lodButtonPanel.setLayout(new javax.swing.BoxLayout(lodButtonPanel, javax.swing.BoxLayout.Y_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        lodPanel_.add(lodButtonPanel, gridBagConstraints);

        lodPanel.add(lodPanel_);

        scaleTerrainPanel.setLayout(new java.awt.GridBagLayout());

        jLabel8.setText("Scale Factor:");
        scaleTerrainPanel.add(jLabel8, new java.awt.GridBagConstraints());

        scaleTerrainFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        scaleTerrainFormattedTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        scaleTerrainFormattedTextField.setValue(new Float(1));
        scaleTerrainPanel.add(scaleTerrainFormattedTextField, new java.awt.GridBagConstraints());

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-2f));
        jLabel9.setText("All values in the terrain grid are scaled by this factor.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        scaleTerrainPanel.add(jLabel9, gridBagConstraints);

        cameraPositionPanel.setLayout(new java.awt.GridBagLayout());

        jLabel10.setText("Horizontal");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPositionPanel.add(jLabel10, gridBagConstraints);

        cameraXFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        cameraXFormattedTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        cameraXFormattedTextField.setValue(new Float(1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cameraPositionPanel.add(cameraXFormattedTextField, gridBagConstraints);

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getSize()-2f));
        jLabel11.setText("<html>Position the cylindrical camera. Coordinates are in meters.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        cameraPositionPanel.add(jLabel11, gridBagConstraints);

        jLabel12.setText("Vertical");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        cameraPositionPanel.add(jLabel12, gridBagConstraints);

        cameraYFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        cameraYFormattedTextField.setPreferredSize(new java.awt.Dimension(200, 28));
        cameraYFormattedTextField.setValue(new Float(1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cameraPositionPanel.add(cameraYFormattedTextField, gridBagConstraints);

        jLabel13.setFont(jLabel13.getFont().deriveFont(jLabel13.getFont().getSize()-2f));
        jLabel13.setText("Grid extension:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cameraPositionPanel.add(jLabel13, gridBagConstraints);

        eastLabel.setFont(eastLabel.getFont().deriveFont(eastLabel.getFont().getSize()-2f));
        eastLabel.setText("jLabel14");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(eastLabel, gridBagConstraints);

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getSize()-2f));
        jLabel15.setText("East");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(jLabel15, gridBagConstraints);

        jLabel16.setFont(jLabel16.getFont().deriveFont(jLabel16.getFont().getSize()-2f));
        jLabel16.setText("South");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(jLabel16, gridBagConstraints);

        southLabel.setFont(southLabel.getFont().deriveFont(southLabel.getFont().getSize()-2f));
        southLabel.setText("jLabel14");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(southLabel, gridBagConstraints);

        jLabel18.setFont(jLabel18.getFont().deriveFont(jLabel18.getFont().getSize()-2f));
        jLabel18.setText("North");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(jLabel18, gridBagConstraints);

        northLabel.setFont(northLabel.getFont().deriveFont(northLabel.getFont().getSize()-2f));
        northLabel.setText("jLabel14");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(northLabel, gridBagConstraints);

        jLabel20.setFont(jLabel20.getFont().deriveFont(jLabel20.getFont().getSize()-2f));
        jLabel20.setText("West");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(jLabel20, gridBagConstraints);

        westLabel.setFont(westLabel.getFont().deriveFont(westLabel.getFont().getSize()-2f));
        westLabel.setText("jLabel14");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        cameraPositionPanel.add(westLabel, gridBagConstraints);

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

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(APPNAME);
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        container3D.setOpaque(false);
        container3D.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                container3DComponentResized(evt);
            }
        });
        container3D.setLayout(new java.awt.BorderLayout());
        getContentPane().add(container3D, java.awt.BorderLayout.CENTER);

        eastPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel1.add(map3DOptionsPanel, gridBagConstraints);

        eastPanel.add(jPanel1, java.awt.BorderLayout.NORTH);

        getContentPane().add(eastPanel, java.awt.BorderLayout.EAST);

        fileMenu.setText("File");

        openDEMMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O,
            java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    openDEMMenuItem.setText("Open Terrain Model…");
    openDEMMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            openDEMMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(openDEMMenuItem);

    openRecentMenu.setText("Open Recent Terrain Model");
    fileMenu.add(openRecentMenu);

    saveDEMMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
saveDEMMenuItem.setText("Save Terrain Model…");
saveDEMMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveDEMMenuItemActionPerformed(evt);
    }
    });
    fileMenu.add(saveDEMMenuItem);
    fileMenu.add(jSeparator5);

    openTextureMenuItem.setText("Open Texture Image…");
    openTextureMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            openTextureMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(openTextureMenuItem);

    clearTextureMenuItem.setText("Remove Texture or Hypsometric Tints");
    clearTextureMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            clearTextureMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(clearTextureMenuItem);
    fileMenu.add(jSeparator1);

    saveSettingsMenuItem.setText("Save Settings…");
    saveSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveSettingsMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(saveSettingsMenuItem);

    openSettingsMenuItem.setText("Open Settings…");
    openSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            openSettingsMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(openSettingsMenuItem);
    fileMenu.add(jSeparator2);

    export3DViewMenuItem.setText("Export Rendering…");
    export3DViewMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            export3DViewMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(export3DViewMenuItem);
    fileMenu.add(exitMenuSeparator);

    exitMenuItem.setText("Exit");
    exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            exitMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    editMenu.setText("Edit");

    cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
cutMenuItem.setText("Cut");
cutMenuItem.setEnabled(false);
editMenu.add(cutMenuItem);

copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,
    java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    copyMenuItem.setText("Copy");
    copyMenuItem.setEnabled(false);
    editMenu.add(copyMenuItem);

    pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
pasteMenuItem.setText("Paste");
pasteMenuItem.setEnabled(false);
editMenu.add(pasteMenuItem);

deleteMenuItem.setText("Delete");
deleteMenuItem.setEnabled(false);
editMenu.add(deleteMenuItem);
editMenu.add(jSeparator3);

gridInfoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I,
    java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    gridInfoMenuItem.setText("Terrain Model Info…");
    gridInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            gridInfoMenuItemActionPerformed(evt);
        }
    });
    editMenu.add(gridInfoMenuItem);
    editMenu.add(jSeparator7);

    scaleTerrainMenuItem.setText("Scale Terrain…");
    scaleTerrainMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            scaleTerrainMenuItemActionPerformed(evt);
        }
    });
    editMenu.add(scaleTerrainMenuItem);
    editMenu.add(jSeparator8);

    cameraPositionMenuItem.setText("Position Cylindrical Camera…");
    cameraPositionMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            cameraPositionMenuItemActionPerformed(evt);
        }
    });
    editMenu.add(cameraPositionMenuItem);

    menuBar.add(editMenu);

    extrasMenu.setText("Extras");

    highResMenuItem.setText("Use High Resolution Cylindrical Rendering");
    highResMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            highResMenuItemActionPerformed(evt);
        }
    });
    extrasMenu.add(highResMenuItem);

    bindLightDirectionMenuItem.setText("Bind Light Direction to View Direction");
    bindLightDirectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            bindLightDirectionMenuItemActionPerformed(evt);
        }
    });
    extrasMenu.add(bindLightDirectionMenuItem);

    backgroundColorMenuItem.setText("Background Color");
    backgroundColorMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            backgroundColorMenuItemActionPerformed(evt);
        }
    });
    extrasMenu.add(backgroundColorMenuItem);
    extrasMenu.add(jSeparator9);

    animationMenuItem.setText("Animation");
    animationMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            animationMenuItemActionPerformed(evt);
        }
    });
    extrasMenu.add(animationMenuItem);

    menuBar.add(extrasMenu);
    //menuBar.remove(extrasMenu);

    macHelpMenu.setText("Help");

    systemInfoMenuItem.setText("System Info…");
    systemInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            systemInfoMenuItemActionPerformed(evt);
        }
    });
    macHelpMenu.add(systemInfoMenuItem);

    menuBar.add(macHelpMenu);

    winHelpMenu.setText("?");

    infoMenuItem.setText("Info");
    infoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            infoMenuItemActionPerformed(evt);
        }
    });
    winHelpMenu.add(infoMenuItem);

    winSystemInfoMenuItem.setText("System Info…");
    winSystemInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            winSystemInfoMenuItemActionPerformed(evt);
        }
    });
    winHelpMenu.add(winSystemInfoMenuItem);

    menuBar.add(winHelpMenu);

    setJMenuBar(menuBar);

    pack();
    }// </editor-fold>//GEN-END:initComponents

private void saveDEMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDEMMenuItemActionPerformed

    if (getFullResolutionGrid() == null) {
        noTerrainErrorMessage(this);
        return;
    }

    String name = this.getTitle();
    name = FileUtils.cutFileExtension(name);
    if (name.isEmpty()) {
        name = "grid.asc";
    }
    String path = FileUtils.askFile(null, "Export ESRI ASCII Grid", name, false, "asc");
    if (path == null) {
        return;
    }
    throw new NotImplementedException(); // FIXME
}//GEN-LAST:event_saveDEMMenuItemActionPerformed

private void saveSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsMenuItemActionPerformed

    try {
        String filePath = FileUtils.askFile(this, "Save Settings", "Settings.txt", false, "txt");
        if (filePath == null) {
            return;
        }
        String serializedModel = this.toString();
        BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
        out.write(serializedModel);
        out.close();
    } catch (IOException e) {
        ika.utils.ErrorDialog.showErrorDialog("The settings could not be written.", e);
    }

}//GEN-LAST:event_saveSettingsMenuItemActionPerformed

private void openSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSettingsMenuItemActionPerformed
    try {
        String newFilePath = FileUtils.askFile(this, "Open Settings", true);
        if (newFilePath == null) {
            return; // user canceled
        }

        // import data from the file
        File file = new File(newFilePath);
        byte[] data = FileUtils.getBytesFromFile(file);
        this.fromString(new String(data));
        this.modelChanged();

    } catch (IOException e) {
        String msg = "The settings could not be read.";
        String title = APPNAME + " Error";
        ika.utils.ErrorDialog.showErrorDialog(msg, title, e, this);
    }
}//GEN-LAST:event_openSettingsMenuItemActionPerformed

private void export3DViewMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export3DViewMenuItemActionPerformed
    try {
        String path = FileUtils.askFile(null, "Save 3D View", "3d.png", false, "png");
        if (path != null) {
            ImageIO.write(map3DViewer.getImage(), "png", new File(path));
        }
    } catch (IOException ex) {
        String msg = "The image could not be saved.";
        String title = APPNAME + " Error";
        ika.utils.ErrorDialog.showErrorDialog(msg, title, ex, this);
    }
}//GEN-LAST:event_export3DViewMenuItemActionPerformed

private void container3DComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_container3DComponentResized
    if (map3DViewer != null) {
        this.map3DViewer.display();
    }
}//GEN-LAST:event_container3DComponentResized

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    askToCloseWindowAndExit();
}//GEN-LAST:event_formWindowClosing

private void infoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoMenuItemActionPerformed
    ika.gui.ProgramInfoPanel.showApplicationInfo();
}//GEN-LAST:event_infoMenuItemActionPerformed

private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
    this.askToCloseWindowAndExit();
}//GEN-LAST:event_exitMenuItemActionPerformed

private void openDEMMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDEMMenuItemActionPerformed
    this.openDEM();
}//GEN-LAST:event_openDEMMenuItemActionPerformed

private void openTextureMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openTextureMenuItemActionPerformed
    String path = FileUtils.askFile(this, "Load Texture", true);
    if (path == null) {
        return;
    }
    openTexture(path);
}//GEN-LAST:event_openTextureMenuItemActionPerformed

private void clearTextureMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearTextureMenuItemActionPerformed
    this.map3DViewer.clearTextureImage();
}//GEN-LAST:event_clearTextureMenuItemActionPerformed

private void highResMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highResMenuItemActionPerformed
    map3DViewer.setHighResCylindricalRendering(highResMenuItem.isSelected());
}//GEN-LAST:event_highResMenuItemActionPerformed

private void bindLightDirectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bindLightDirectionMenuItemActionPerformed
    map3DViewer.setBindLightDirectionToViewDirection(bindLightDirectionMenuItem.isSelected());
}//GEN-LAST:event_bindLightDirectionMenuItemActionPerformed

private void cameraPositionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cameraPositionMenuItemActionPerformed

    // get grid dimensions
    GeoGrid grid = this.getFullResolutionGrid();

    if (grid == null) {
        noTerrainErrorMessage(this);
        return;
    }

    double w = grid.getBounds2D(GeoObject.UNDEFINED_SCALE).getWidth();
    double h = grid.getBounds2D(GeoObject.UNDEFINED_SCALE).getHeight();
    double cx = grid.getCenterX(GeoObject.UNDEFINED_SCALE);
    double cy = grid.getCenterY(GeoObject.UNDEFINED_SCALE);

    // init extension labels
    DecimalFormat format = new DecimalFormat("#,##0.###");
    westLabel.setText(format.format(grid.getWest()));
    eastLabel.setText(format.format(grid.getEast()));
    southLabel.setText(format.format(grid.getSouth()));
    northLabel.setText(format.format(grid.getNorth()));

    // write current coordinates
    double xrel = this.map3DViewer.getShiftX();
    double yrel = this.map3DViewer.getShiftY();
    double x = xrel * w / 2 + cx;
    double y = yrel * h / 2 + cy;
    cameraXFormattedTextField.setValue(x);
    cameraYFormattedTextField.setValue(y);

    // show dialog
    int option = JOptionPane.showOptionDialog(this,
            cameraPositionPanel,
            "Position Cylindrical Camera",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, null, null);

    if (option != JOptionPane.OK_OPTION) {
        return;
    }

    // read new coordinates
    try {
        cameraXFormattedTextField.commitEdit();
        cameraYFormattedTextField.commitEdit();

        x = ((Number) (cameraXFormattedTextField.getValue())).doubleValue();
        y = ((Number) (cameraYFormattedTextField.getValue())).doubleValue();

        this.map3DViewer.setCamera(Camera.cylindrical);

        xrel = (x - cx) / w * 2;
        yrel = (y - cy) / h * 2;

        this.map3DViewer.setShiftX((float) xrel);
        this.map3DViewer.setShiftY((float) yrel);

    } catch (Exception exc) {
        ErrorDialog.showErrorDialog("An error occured while positioning the camera.",
                "Error", exc, this);
    }
}//GEN-LAST:event_cameraPositionMenuItemActionPerformed

private void scaleTerrainMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleTerrainMenuItemActionPerformed

    if (getFullResolutionGrid() == null) {
        noTerrainErrorMessage(this);
        return;
    }

    int option = JOptionPane.showOptionDialog(this,
            scaleTerrainPanel,
            "Scale Terrain",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, null, null);
    if (option != JOptionPane.OK_OPTION) {
        return;
    }

    try {
        scaleTerrainFormattedTextField.commitEdit();
        java.lang.Number f = (java.lang.Number) (scaleTerrainFormattedTextField.getValue());
        float scale = f.floatValue();
        GridScaleOperator op = new GridScaleOperator(scale);
        GeoGrid scaledGrid = op.operate(getFullResolutionGrid());
        gridPyramid = new GaussianPyramid(scaledGrid, 9999, MIN_GRID_CELLS);
        originalInfoText = getFullResolutionGrid().toStringWithStatistics("<br>");
        modelChanged();
    } catch (Exception exc) {
        ErrorDialog.showErrorDialog("An error occured while scaling the terrain.",
                "Error", exc, this);
    }
}//GEN-LAST:event_scaleTerrainMenuItemActionPerformed

private void gridInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gridInfoMenuItemActionPerformed
    this.showGridInfo();
}//GEN-LAST:event_gridInfoMenuItemActionPerformed

private void backgroundColorMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundColorMenuItemActionPerformed

    Color bc = map3DViewer.getBackgroundColor();
    bc = ColorPicker.showDialog(this, "Background Color", bc, false);
    if (bc != null) {
        map3DViewer.setBackgroundColor(bc);
    }
}//GEN-LAST:event_backgroundColorMenuItemActionPerformed

private void systemInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemInfoMenuItemActionPerformed
    new SystemInfoGPU(this);
}//GEN-LAST:event_systemInfoMenuItemActionPerformed

private void winSystemInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_winSystemInfoMenuItemActionPerformed
    new SystemInfoGPU(this);
}//GEN-LAST:event_winSystemInfoMenuItemActionPerformed

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

private void hypsoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypsoCheckBoxActionPerformed
    hypsoSlider.setEnabled(hypsoCheckBox.isSelected());
    hypsoDefaultButton.setEnabled(hypsoCheckBox.isSelected());
    adjustHypsometricTintingVisibility();
}//GEN-LAST:event_hypsoCheckBoxActionPerformed

private void hypsoDefaultButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypsoDefaultButtonActionPerformed
    this.initHypsoSlider();
}//GEN-LAST:event_hypsoDefaultButtonActionPerformed
    
private void animationMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_animationMenuItemActionPerformed
    this.map3DViewer.setCamera(Map3DViewer.Camera.planOblique);
    ShearPanel.showPanel(this, map3DViewer);
}//GEN-LAST:event_animationMenuItemActionPerformed
                                         
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem animationMenuItem;
    private javax.swing.JMenuItem backgroundColorMenuItem;
    private javax.swing.JCheckBoxMenuItem bindLightDirectionMenuItem;
    private javax.swing.JMenuItem cameraPositionMenuItem;
    private javax.swing.JPanel cameraPositionPanel;
    private javax.swing.JFormattedTextField cameraXFormattedTextField;
    private javax.swing.JFormattedTextField cameraYFormattedTextField;
    private javax.swing.JMenuItem clearTextureMenuItem;
    private javax.swing.JPanel container3D;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JLabel eastLabel;
    private javax.swing.JPanel eastPanel;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JSeparator exitMenuSeparator;
    private javax.swing.JMenuItem export3DViewMenuItem;
    private javax.swing.JMenu extrasMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem gridInfoMenuItem;
    private javax.swing.JCheckBoxMenuItem highResMenuItem;
    private javax.swing.JCheckBox hypsoCheckBox;
    private javax.swing.JButton hypsoDefaultButton;
    private javax.swing.JPanel hypsoPanel;
    private com.bric.swing.GradientSlider hypsoSlider;
    private javax.swing.JMenuItem infoMenuItem;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.ButtonGroup lodButtonGroup;
    private javax.swing.JPanel lodButtonPanel;
    private javax.swing.JTextArea lodInfoTextArea;
    private javax.swing.JPanel lodPanel;
    private javax.swing.JPanel lodPanel_;
    private javax.swing.JMenu macHelpMenu;
    private ch.ethz.karto.map3d.gui.Map3DOptionsPanel map3DOptionsPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel northLabel;
    private javax.swing.JMenuItem openDEMMenuItem;
    private javax.swing.JMenu openRecentMenu;
    private javax.swing.JMenuItem openSettingsMenuItem;
    private javax.swing.JMenuItem openTextureMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem saveDEMMenuItem;
    private javax.swing.JMenuItem saveSettingsMenuItem;
    private javax.swing.JFormattedTextField scaleTerrainFormattedTextField;
    private javax.swing.JMenuItem scaleTerrainMenuItem;
    private javax.swing.JPanel scaleTerrainPanel;
    private javax.swing.JLabel southLabel;
    private javax.swing.JMenuItem systemInfoMenuItem;
    private javax.swing.JLabel westLabel;
    private javax.swing.JMenu winHelpMenu;
    private javax.swing.JMenuItem winSystemInfoMenuItem;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    }

    /**
     * Part of the ComponentListener interface.
     */
    @Override
    public void componentShown(ComponentEvent e) {
    }

    /**
     * Part of the ComponentListener interface.
     */
    @Override
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * Part of the ComponentListener interface.
     */
    @Override
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * Part of the ComponentListener interface. Make sure this window is not
     * getting too small.
     */
    @Override
    public void componentResized(ComponentEvent e) {

        if (this.packedSize == null) {
            return;
        }

        // Check if either the width or the height are below minimum
        // and reset size if necessary.
        // Note: this is not elegant, but SUN recommends doing it that way.
        int width = getWidth();
        int height = getHeight();
        boolean resize = false;

        if (width < this.packedSize.width) {
            resize = true;
            width = this.packedSize.width;
        }
        if (height < this.packedSize.height) {
            resize = true;
            height = this.packedSize.height;
        }
        if (resize) {
            setSize(width, height);
        }
    }

    private void initHypsoSlider() {
        float[] values = new float[] {0, 0.08f, 0.24f, 0.43f, 0.69f, 0.89f};
            Color[] colors = new Color[] {
                new Color(120, 181, 141),
                new Color(124, 172, 104),
                new Color(190, 194, 107),
                new Color(212, 218, 170),
                new Color(225, 246, 244),
                new Color(255, 255, 255)
            };
            hypsoSlider.setValues(values, colors);
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

        modelChanged();
        map3DViewer.display();
    }

    public void showHypsometricDialog() {
        //hypsoCheckBox.setSelected(map3DViewer.hasTexture() && map3DViewer.isTexture1D()); // FIXME
        JOptionPane.showOptionDialog(this,
                hypsoPanel,
                "Hypsometric Tints",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        adjustHypsometricTintingVisibility();
    }

    private void adjustHypsometricTintingVisibility() {
        if (hypsoCheckBox.isSelected()) {
            this.readHypsometricTints();
        } else {
            map3DViewer.display();
        }
    }
}
