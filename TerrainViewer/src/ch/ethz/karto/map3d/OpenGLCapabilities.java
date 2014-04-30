package ch.ethz.karto.map3d;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

/**
 * Provides information about the OpenGL systems that support pbuffers. Older
 * Windows systems do not support pbuffers, for these systems, use
 * the OpenGLCapabilitiesWindowsNoPBuffer class that overwrites pbuffer specific
 * methods.
 * All methods must be called from the OpenGL thread.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich
 */
public class OpenGLCapabilities {

    protected GLContext lastContext, newContext;
    private final GLPbuffer buffer;

    protected OpenGLCapabilities(GLProfile profile, AbstractGraphicsDevice graphicsDevice) {
        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(false);
        
        // createGLPbuffer throws a GLException if pbuffers are not supported.
        buffer = factory.createGLPbuffer(graphicsDevice, caps, null, 1, 1, null);
    }

    protected GL getGL() {
        try {
            switchToNewContext(buffer.getContext());
            return buffer.getGL();
        } catch (Throwable e) {
        }
        return null;
    }

    /**
     * Releases the current context, and makes the passed context the current one.
     * @param context
     */
    protected void switchToNewContext(final GLContext context) {
        lastContext = GLContext.getCurrent();
        if (context == lastContext) {
            newContext = null;
            return;
        }
        if (lastContext != null) {
            lastContext.release();
        }
        newContext = context;
        newContext.makeCurrent();
    }

    /**
     * Switch back to previous context: Reverts to the previously replaced
     * context and distroys the current one.
     */
    protected void restoreLastGLContext() {

        if (newContext != null) {
            newContext.release();
        }
        if (lastContext != null) {
            lastContext.makeCurrent();
        }
        if (newContext != null) {
            if (!isMacOS()) {
                newContext.destroy(); // causes flickering on Mac
            }
        }
        if (buffer != null) {
            if (!isMacOS()) {
                buffer.destroy(); // causes flickering on Mac
            }
        }

    }

    private boolean isMacOS() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Mac OS");
    }
}
