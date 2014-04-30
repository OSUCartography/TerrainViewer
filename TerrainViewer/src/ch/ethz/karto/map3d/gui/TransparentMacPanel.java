package ch.ethz.karto.map3d.gui;

import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 * Makes a JPanel transparent when on Mac OS X or newer. This is useful for
 * panels in JTabbedPanes.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class TransparentMacPanel extends JPanel {

    private void conditionalTransparency() {
        if (TransparentMacPanel.isMacOSX_10_5_orHigherWithJava5()) {
            this.setOpaque(false);
        }
    }

    public TransparentMacPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        conditionalTransparency();
    }

    public TransparentMacPanel(LayoutManager layout) {
        super(layout);
        conditionalTransparency();
    }

    public TransparentMacPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        conditionalTransparency();
    }

    public TransparentMacPanel() {
        conditionalTransparency();
    }
    
    private static boolean isMacOSX_10_5_orHigherWithJava5() {
        
        if (!isMacOSX_10_5_orHigher())
            return false;
        
        // check the java version. 1.5.x is required
        String[] versionStrings = System.getProperty("java.version").split("\\.");
        if (versionStrings[0].equals("1")
        && Integer.parseInt(versionStrings[1]) >= 5)
            return true;
        return Integer.parseInt(versionStrings[0]) > 1;
    }
    
    private static boolean isMacOSX_10_5_orHigher() {
        
        String osname = System.getProperty("os.name");
        boolean ismacosx  = osname.toLowerCase().startsWith("mac os x");
        if (!ismacosx)
            return false;
        
        // get the Mac OS version
        String[] osVersionStrings = System.getProperty("os.version").split("\\.");
        
        // check for at least 10.5.x
        if (osVersionStrings[0].equals("10"))
            return Integer.parseInt(osVersionStrings[1]) >= 5;
        
        // maybe we have Mac OS 11 or higher
        return Integer.parseInt(osVersionStrings[0]) > 10;

    }
}
