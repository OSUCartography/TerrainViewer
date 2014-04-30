package ch.ethz.karto.map3d;

import ika.utils.Sys;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.nio.IntBuffer;
import java.util.StringTokenizer;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.Threading;

/**
 * Map3DGLCapabilities provides information about the present version of OpenGL2.
 * All information is gathered in a static initialization block. A GL context
 * is created and immediately released afterwards. This should avoid issues with
 * threading and too many OpenGL contextes allocated.
 * @author jenny
 */
public class Map3DGLCapabilities {

    private static boolean hasOpenGL = false;
    private static boolean hasNonPowerOfTwoTextures = false;
    private static boolean hasVBO = false;
    private static boolean hasFloatingPointTextures = false;
    private static String glVendor;
    private static String glRenderer;
    private static String glVersion;
    private static String glShadingLanguageVersion;
    private static int maxTextureSize;
    private static int maxDrawBuffers;
    private static int maxVertexTextureImageUnits;
    private static int maxTextureImageUnits;

    public static AbstractGraphicsDevice defaultGraphicsDevice;
    public static GLProfile defaultGLProfile;
    
    static {
        // initialize all static variables
//        Runnable r = new Runnable() {
//
//            @Override
//            public void run() {
                try {
                	GLProfile defaultGLProfile = GLProfile.getDefault();
                	defaultGraphicsDevice = GLProfile.getDefaultDevice();
                    OpenGLCapabilities capabilities = new OpenGLCapabilities(defaultGLProfile, defaultGraphicsDevice);
                    if (capabilities != null) {
                        try {
                            GL gl = capabilities.getGL();
                            init(gl);
                        } finally {
                            capabilities.restoreLastGLContext();
                        }
                    }
                } catch (Throwable e) {
                    hasOpenGL = false;
                }
//            }
//        };
//        maybeDoSingleThreadedWorkaround(r);

    }

    private Map3DGLCapabilities() {
    }

    /**
     * Executes a runnable on the OpenGL thread and blocks the calling thread
     * until the runnable has finished executing.
     * @param action
     */
    private static void maybeDoSingleThreadedWorkaround(Runnable action) {
        if (Threading.isSingleThreaded() && !Threading.isOpenGLThread()) {
            // invokeOnOpenGLThread blocks the calling thread, although this is
            // not documented
            // http://www.javagaming.org/index.php/topic,18442.0.html
            Threading.invokeOnOpenGLThread(true, action);
        } else {
            action.run();
        }
    }

    private static void init(GL gl) {
        hasOpenGL = gl != null;
        if (!hasOpenGL) {
            return;
        }

        hasNonPowerOfTwoTextures = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
        hasVBO = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
        hasFloatingPointTextures = gl.isExtensionAvailable("GL_ARB_texture_float");
        glVendor = gl.glGetString(GL2.GL_VENDOR);
        glRenderer = gl.glGetString(GL2.GL_RENDERER);
        glVersion = gl.glGetString(GL2.GL_VERSION);
        glShadingLanguageVersion = gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION);
        maxTextureSize = getInteger(gl, GL2.GL_MAX_TEXTURE_SIZE);
        maxDrawBuffers = getInteger(gl, GL2.GL_MAX_DRAW_BUFFERS);
        maxVertexTextureImageUnits = getInteger(gl, GL2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
        maxTextureImageUnits = getInteger(gl, GL2.GL_MAX_TEXTURE_IMAGE_UNITS);
    }

    /**
     * Returns whether OpenGL is accessible.
     * @return
     */
    public static boolean hasOpenGL() {
        return hasOpenGL;
    }

    /**
     * Returns whether a required OpenGL version is present.
     * @param reqMajorVersion
     * @param reqMinorVersion
     * @return
     */
    public static boolean hasOpenGLVersion(int reqMajorVersion, int reqMinorVersion) {

        int foundMajorVersion = 0;
        int foundMinorVersion = 0;
        if (!hasOpenGL()) {
            return false;
        }
        StringTokenizer tokens = new StringTokenizer(glVersion, ". ");
        if (!tokens.hasMoreElements()) {
            return false;
        }
        foundMajorVersion = Integer.parseInt(tokens.nextToken());
        if (foundMajorVersion > reqMajorVersion) {
            return true;
        }
        if (foundMajorVersion < reqMajorVersion) {
            return false;
        }
        if (!tokens.hasMoreElements()) {
            return false;
        }
        foundMinorVersion = Integer.parseInt(tokens.nextToken());

        return reqMinorVersion <= foundMinorVersion;

    }

    /**
     * Returns whether textures can have dimensions differing from
     * power of 2 values.
     * @return
     */
    public static boolean hasNonPowerOfTwoTextures() {
        return hasNonPowerOfTwoTextures;
    }

    /**
     * Returns whether vertex buffer objects are available.
     * VBO are available with OpenGL 1.5 or earlier with an extension.
     * @return
     */
    public static boolean hasVBO() {
        return hasVBO;
    }

    /**
     * Returns whether floating point textures are supported.
     * @return
     */
    public static boolean hasFloatingPointTextures() {
        return hasFloatingPointTextures;
    }

    public static String getGLVendor() {
        return glVendor;
    }

    public static String getGLRenderer() {
        return glRenderer;
    }

    public static String getGLVersion() {
        return glVersion;
    }

    public static String getGLShadingLanguageVersion() {
        return glShadingLanguageVersion;
    }

    public static int getMaxTextureSize() {
        return maxTextureSize;
    }

    public static int getMaxDrawBuffers() {
        return maxDrawBuffers;
    }

    /**
     * Returns the maximum number of textures accessible in vertex shader
     * @return
     */
    public static int getMaxVertexTextureImageUnits() {
        return maxVertexTextureImageUnits;
    }

    /**
     * Returns the maximum number of textures accessible in a fragment shader
     * @return
     */
    public static int getMaxTextureImageUnits() {
        return maxTextureImageUnits;
    }

    /**
     * Returns whether PBuffers are available. pbuffers are not supported on
     * older graphics cards (pre 1.5)
     */
    public static boolean hasPBuffer() {
        GLDrawableFactory factory = GLDrawableFactory.getFactory(defaultGLProfile);
        return factory.canCreateGLPbuffer(defaultGraphicsDevice, defaultGLProfile);
    }

    private static int getInteger(GL gl, int id) {
        IntBuffer params = IntBuffer.allocate(1);
        gl.glGetIntegerv(id, params);
        return params.get(0);
    }
}
