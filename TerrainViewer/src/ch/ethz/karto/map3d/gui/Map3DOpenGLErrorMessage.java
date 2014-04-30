package ch.ethz.karto.map3d.gui;

import ika.utils.Sys;
import ika.utils.TextWindow;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

/**
 * Error messages to inform the user about missing OpenGL or insufficient version.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich
 */
public class Map3DOpenGLErrorMessage {

    /**
     * Displays a dialog informing about the absence of OpenGL2. Suggests simple
     * system dependent measures to solve the problem.
     * @param appName The name of the application.
     * @param exit If true, the user is informed that the application will exit.
     */
    public static void noOpenGLMessage(String appName, boolean willExit) {

        final String newLine = System.getProperty("line.separator");
        final String errorTitle = appName == null ? "Error" : appName + " Error";

        StringBuilder sb = new StringBuilder();
        sb.append("OpenGL and JOGL are not accessible.");

        // DLLs must be in the same folder as the exe file.
        if (Sys.isWindows()) {
            sb.append(newLine);
            sb.append("Please make sure the four DLL files are in "
                    + "the same folder as ");
            if (appName == null) {
                sb.append("the exe file.");
            } else {
                sb.append(appName);
                sb.append(".exe.");
            }
        }

        // updating the driver for the graphics card should solve the problem.
        if (!Sys.isMacOSX()) {
            sb.append(newLine);
            updateDriverString(sb, appName);
        }

        // possibly mixed up 32 and 64 bit versions
        if (!Sys.isMacOSX()) {
            sb.append(newLine);
            sb.append("You are possibly trying to run a 32 bit version "
                    + "with 64 bit Java, or vice versa.");
        }

        // inform about immediate shut down
        if (willExit) {
            sb.append(System.getProperty("line.separator"));
            sb.append(appName == null ? "The application" : appName);
            sb.append(" will exit now.");
        }

        // don't use ErrorDialog, which would delay the display of
        // the dialog to make sure it is in the EDT, at which time
        // System.exit is already executed.
        JOptionPane.showMessageDialog(null, sb.toString(),
                errorTitle, JOptionPane.ERROR_MESSAGE);

    }

    /**
     * Displays a dialog informing about the absence of OpenGL version 1.1.
     * This should be called if OpenGL prior to version 1.1 is present.
     * @param appName
     * @param willExit
     */
    public static void noOpenGL1_1(String appName, boolean willExit) {

        final String newLine = System.getProperty("line.separator");
        final String errorTitle = appName == null ? "Error" : appName + " Error";

        StringBuilder sb = new StringBuilder();
        sb.append("OpenGL version 1.1 or higher is required.");

        // updating the driver for the graphics card should solve the problem.
        if (!Sys.isMacOSX()) {
            sb.append(newLine);
            updateDriverString(sb, appName);
        }

        // inform about immediate shut down
        if (willExit) {
            sb.append(newLine);
            sb.append(appName == null ? "The application" : appName);
            sb.append(" will exit now.");
        }

        // don't use ErrorDialog, which would delay the display of
        // the dialog to make sure it is in the EDT, at which time
        // System.exit is already executed.
        JOptionPane.showMessageDialog(null, sb.toString(),
                errorTitle, JOptionPane.ERROR_MESSAGE);

    }

    /**
     * Inform the user about a fatal error and an immediate shut down.
     * @param ex
     * @param appName
     */
    public static void fatalErrorMessage(Throwable ex, String appName) {

        final String newLine = System.getProperty("line.separator");
        final String errorTitle = appName + " Error";

        StringBuilder sb = new StringBuilder();
        sb.append("<html>An error occurred. ");
        sb.append(appName == null ? "The application" : appName);
        sb.append(" cannot run.<br>");

        if (ex != null && ex.getMessage() != null) {
            sb.append("Error: ");
            sb.append(ex.getMessage());
        }
        sb.append("</html>");

        // don't use ika.utils.ErrorDialog that would search for
        // a parent frame, which is potentially corrupt
        JOptionPane.showMessageDialog(null, sb.toString(), errorTitle,
                JOptionPane.ERROR_MESSAGE);

        // show a stack trace if the exception has no message
        if (ex != null && ex.getMessage() == null) {
            StringWriter sw = new StringWriter();
            sw.write(errorTitle);
            sw.write(newLine);
            sw.write("Please send this report to jenny@karto.baug.ethz.ch");
            sw.write(newLine);
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.flush();
            new TextWindow(null, true, true, sw.toString(), errorTitle);
        }
    }

    /**
     * Appends a message suggesting to update the driver for the graphcis card.
     * @param sb
     * @param appName
     */
    private static void updateDriverString(StringBuilder sb, String appName) {
        sb.append("Please update the driver for the graphics card and then try "
                + "running ");
        sb.append(appName == null ? "the application" : appName);
        sb.append(" again.");
    }
}
