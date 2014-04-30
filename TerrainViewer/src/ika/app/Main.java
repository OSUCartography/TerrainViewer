package ika.app;

import ch.ethz.ika.gui.MainWindow;
import ch.ethz.karto.map3d.Map3DGLCapabilities;
import ch.ethz.karto.map3d.gui.Map3DOpenGLErrorMessage;
import ika.utils.IconUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Main {

    private static boolean displayAssertionInfo() {
        //JOptionPane.showMessageDialog(null, "Special Version with Error Testing Enabled");
        return true;
    }

    public static void main(String[] args) {

        java.util.Properties props = ika.utils.PropertiesLoader.loadProperties("ika.app.Application");
        final String appName = props.getProperty("ApplicationName");

        // use the standard look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        // on Mac OS X: take the menu bar out of the window and put it on top
        // of the main screen.
        if (ika.utils.Sys.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        // set icon for JOptionPane dialogs. This is done automatically on Mac 10.5.
        if (!ika.utils.Sys.isMacOSX_10_5_orHigherWithJava5()) {
            IconUtils.setOptionPaneIcons(props.getProperty("ApplicationIcon"));
        }

        // Use this for heavy weight components with swing
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    assert displayAssertionInfo();

                    // test for OpenGL
                    if (!Map3DGLCapabilities.hasOpenGL()) {
                        Map3DOpenGLErrorMessage.noOpenGLMessage(appName, true);
                        System.exit(0);
                    }

                    // test for OpenGL version 1.1 or higher
                    if (!Map3DGLCapabilities.hasOpenGLVersion(1, 1)) {
                        Map3DOpenGLErrorMessage.noOpenGL1_1(appName, true);
                        System.exit(0);
                    }

                    MainWindow window = null;
                    try {
                        window = new MainWindow();
                        window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
                        // Immediately layout the window after extending its size to
                        // the maximum and before making the window visible.
                        // Otherwise the panel on the right side would appear to
                        // jump around.
                        window.doLayout();
                        window.setVisible(true);
                    } catch (Throwable ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        Map3DOpenGLErrorMessage.fatalErrorMessage(ex, appName);
                        System.exit(0);
                    }

                    window.openDEM();
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    String msg = "An error occured.";
                    ika.utils.ErrorDialog.showErrorDialog(msg, appName, ex, null);
                }
            }
        });
    }
}
