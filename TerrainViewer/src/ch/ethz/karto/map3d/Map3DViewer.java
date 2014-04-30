package ch.ethz.karto.map3d;

import ch.ethz.karto.map3d.gui.Map3DOptionsPanel;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import ika.gui.GUIUtil;
import ika.utils.ErrorDialog;
import ika.utils.TextWindow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.media.opengl.awt.GLJPanel;

import com.jogamp.opengl.util.awt.Screenshot;

/**
 * This class implements a Map3D viewer.
 */
public class Map3DViewer extends GLCanvas implements GLEventListener {

    private static final float MAX_CYLINDER_FOV = 165f;
    private boolean highResCylindricalRendering = false;

    public void setHighResCylindricalRendering(boolean useHighRes) {
        if (useHighRes != highResCylindricalRendering) {
            highResCylindricalRendering = useHighRes;
            if (this.camera == Camera.cylindrical) {
                this.updateView();
            }
        }
    }
    private boolean bindLightDirectionToViewDirection = false;

    public void setBindLightDirectionToViewDirection(boolean bind) {
        if (this.bindLightDirectionToViewDirection != bind) {
            this.bindLightDirectionToViewDirection = bind;
            if (this.camera == Camera.cylindrical) {
                this.updateView();
            }
        }
    }

    public enum Camera {

        perspective, parallelOblique, planOblique, orthogonal, cylindrical
    }

    public static String camera(Camera camera) {
        switch (camera) {
            case perspective:
                return "Perspective View";
            case parallelOblique:
                return "Parallel Oblique";
            case planOblique:
                return "Plan Oblique";
            case orthogonal:
                return "2D Orthogonal";
            case cylindrical:
                return "Cylindrical";
            default:
                return "";
        }
    }

    public static Camera camera(String name) {
        if ("Perspective View".equals(name)) {
            return Camera.perspective;
        }
        if ("Parallel Oblique".equals(name)) {
            return Camera.parallelOblique;
        }
        if ("Plan Oblique".equals(name)) {
            return Camera.planOblique;
        }
        if ("2D Orthogonal".equals(name)) {
            return Camera.orthogonal;
        }
        if ("Cylindrical".equals(name)) {
            return Camera.cylindrical;
        }
        return Camera.perspective;
    }
    /**
     * only use glu in callbacks
     */
    private GLU glu_callback_only;
    private Component component;
    private GLAutoDrawable drawable;
    private Map3DModel model;
    private Map3DTexture texture;
    private Map3DAnimation animation;
    private boolean antialiasing = true;
    private Map3DMouseHandler mouseHandler;
    /**
     * Default rotation around x-axis. Positive values between 0 and 90 degrees.
     * 0 degree corresponds to vertical view, 90 degrees to a horizontal view.
     */
    private float defaultXAngle = 55.0f;
    /**
     * Default rotation around z-axis. Positive values corresponds to a
     * clockwise rotation. Units in degrees.
     */
    private float defaultZAngle = 0.0f;
    /**
     * Default view angle. A value of 25 degrees corresponds roughly to an
     * object of 25 cm size held at an eye distance of 50 cm.
     */
    private float defaultViewAngle = 25.0f;
    private float defaultShiftX = 0f;
    private float defaultShiftY = 0f;
    /**
     * Minimum and maximum angle around x-axis.
     */
    protected static final float MIN_X_ANGLE = 1.0f, MAX_X_ANGLE = 90.0f;
    /**
     * Minimum, maximum, and default distance from model.
     */
    //private static final float MIN_DISTANCE = 1.2f,  MAX_DISTANCE = 3.6f,  DEFAULT_DISTANCE = 2.4f;
    public static final float MIN_DISTANCE = 0.2f, MAX_DISTANCE = 3f;
    private float defaultDistance = 2.4f;
    private float defaultCylindricalHeight = 0.2f;
    /**
     * Rotation around the x axis in degrees.
     */
    protected float xAngle = defaultXAngle;
    /**
     * Rotation around the z axis in degrees.
     */
    protected float zAngle = defaultZAngle;
    /**
     * Distance of camera from origin
     */
    protected float viewDistance = defaultDistance;
    /**
     * Height of cylindrical camera
     */
    private float cylindricalHeight = defaultCylindricalHeight;
    /**
     * Field of view of camera
     */
    protected float fov = defaultViewAngle;
    protected float shearX = 0;
    protected float shearY = 0;
    protected float shiftX = defaultShiftX;
    protected float shiftY = defaultShiftY;
    private float bgRed = 1.0f;
    private float bgGreen = 1.0f;
    private float bgBlue = 1.0f;
    protected boolean shadingEnabled = true;
    private Camera camera = Camera.planOblique;//Camera.perspective;
    protected static final float MIN_SHEAR_ANGLE = 0.0f, MAX_SHEAR_ANGLE = 180.0f;
    private static final float DEFAULT_LIGHT_AMBIENT = 0.4f;
    private static final float DEFAULT_LIGHT_DIFFUSE = 0.6f;
    private static final float DEFAULT_LIGHT_AZIMUTH = 315; //225;
    private static final float DEFAULT_LIGHT_ZENITH = 45;
    private float ambientLight = DEFAULT_LIGHT_AMBIENT;
    private float diffuseLight = DEFAULT_LIGHT_DIFFUSE;
    private float lightAzimuth = DEFAULT_LIGHT_AZIMUTH;
    private float lightZenith = DEFAULT_LIGHT_ZENITH;
    private static final float Z_NEAR = 0.001f;
    private static final float Z_FAR = 100.0f;
    private float aspectRatio_callback_only = -1;
    private static final int DEF_CYLINDER_IMAGES_COUNT = 8;
    private boolean fogEnabled = false;
    private float fogStart = 0f;
    private float fogEnd = 1f;
    private Color fogColor = Color.WHITE;

    /**
     * @return the antialiasing
     */
    public boolean isAntialiasing() {
        return antialiasing;
    }

    /**
     * @param antialiasing the antialiasing to set
     */
    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    private int getCylindricalImagesCount() {
        if (this.highResCylindricalRendering) {
            return this.drawable.getContext().getGLDrawable().getWidth();
        } else {
            return DEF_CYLINDER_IMAGES_COUNT;
        }
    }

    public static enum GLComponentType {

        GL_AWT, GL_Swing
    };

    /**
     * Map3D viewer constructor
     *
     * @param glComponentType With GL_AWT a heavyweight AWT component GLCanvas
     * is created. With GL_Swing a lightweight GLJPanel is created. GLCanvas
     * offers faster rendering, but may cause problems when combined with other
     * Swing components with certain layout managers.
     */
    public Map3DViewer(GLComponentType glComponentType, GLProfile glProfile) {
        this(glComponentType, glProfile, null);
    }

    /**
     * Map3D viewer constructor
     *
     * @param glComponentType With GL_AWT a heavyweight AWT component GLCanvas
     * is created. With GL_Swing a lightweight GLJPanel is created. GLCanvas
     * offers faster rendering, but may cause problems when combined with other
     * Swing components with certain layout managers.
     */
    public Map3DViewer(GLComponentType glComponentType, GLProfile glProfile, Map3DModel model) {
        this.init(glComponentType, glProfile, model);
    }

    private void init(GLComponentType glComponentType, GLProfile profile, Map3DModel model) {
        GLCapabilities caps = new GLCapabilities(profile);
        // use sample buffers for antialiasing
        caps.setSampleBuffers(true);
        // set the number of supersampling for antialising
        caps.setNumSamples(Map3DOptionsPanel.getAntialiasingLevel());

        if (glComponentType == GLComponentType.GL_Swing) {
            this.component = new GLJPanel(caps);
        } else {
            this.component = new GLCanvas(caps);
        }
        this.drawable = (GLAutoDrawable)this.component;
        this.component.setSize(1024, 768);
        //((Component) this.component).setIgnoreRepaint(true);
        this.drawable.addGLEventListener(this);

        if (model == null) {
            model = new Map3DModelVBOShader();
            if (!model.canRun()) {
                model = new Map3DModelVBO();
            }
            if (!model.canRun()) {
                model = new Map3DModelVertexArrays();
            }
        }
        //model = new Map3DModelVertexArrays();
        this.model = model;

        this.texture = new Map3DTexture();
        this.setAnimation(new Map3DRotationAnimation(this.drawable));

        mouseHandler = new Map3DMouseHandler(this);
        this.component.addMouseMotionListener(mouseHandler);
        this.component.addMouseListener(mouseHandler);
        this.component.addMouseWheelListener(mouseHandler);
    }

    public BufferedImage getImage() {
        // Needs the "current context"!
        this.drawable.getContext().makeCurrent();
        int width = component.getWidth();
        int height = component.getHeight();
        return Screenshot.readToBufferedImage(width, height);
    }

    public Map3DModel getModel(){
        return this.model;
    }
    
    public void setModel(float grid[][], float cellSize) {
        Map3DTexture1DMapper mapper = new Map3DNonLinearTexture1DMapper(grid);
        this.setModel(grid, cellSize, mapper);
    }

     public void setModel(float[][] grid, float cellSize, Map3DTexture1DMapper mapper) {
        this.model.setModel(grid, cellSize, mapper);
        this.updateView();
    }
    
    public void setTextureImage(BufferedImage textureImage) {
        this.texture.setTexture(textureImage);
        this.updateView();
    }

    public void clearTextureImage() {
        this.texture.clearTexture();
        this.updateView();
    }

    public boolean hasTexture() {
        return this.texture.hasTexture();
    }

    public boolean isTexture1D() {
        return this.texture.is1D();
    }

    /**
     * Enables or disables light.
     *
     * @param enable turns light on or off
     */
    public void setShading(boolean enable) {
        this.shadingEnabled = enable;
        this.updateView();
    }

    public boolean isShading() {
        return shadingEnabled;
    }

    public void enableFog(GL gl1) {
        GL2 gl = (GL2)gl1;
        if (this.fogEnabled) {
            gl.glEnable(GL2.GL_FOG);
            float r = fogColor.getRed() / 255f;
            float g = fogColor.getGreen() / 255f;
            float b = fogColor.getBlue() / 255f;
            gl.glFogfv(GL2.GL_FOG_COLOR, new float[]{r, g, b}, 0);
            float start = fogStart;
            if (camera != Camera.cylindrical) {
                start += viewDistance - 0.5f;
            }
            gl.glFogf(GL2.GL_FOG_START, start);
            float end = fogEnd;
            if (camera != Camera.cylindrical) {
                end += viewDistance - 0.5f;
            }
            gl.glFogf(GL2.GL_FOG_END, end);
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
        } else {
            gl.glDisable(GL2.GL_FOG);
        }
    }

    /**
     * Get the OpenGL component.
     *
     * @return The OpenGL component
     */
    public Component getComponent() {
        return (Component) this.component;
    }

    private void setupLight(GL gl1, double lightAzimuthDeg, double lightZenithDeg) {

        GL2 gl = (GL2)gl1;

        if (this.shadingEnabled) {
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_LIGHT0);

            final double a = -Math.PI / 2 - Math.toRadians(lightAzimuthDeg);
            final double z = Math.toRadians(lightZenithDeg);
            final double sinz = Math.sin(z);

            final float lx = (float) (Math.cos(a) * sinz);
            final float ly = (float) (Math.sin(a) * sinz);
            final float lz = (float) Math.cos(z);

            float light_ambient[] = {ambientLight, ambientLight, ambientLight, 1.0f};
            float light_diffuse[] = {diffuseLight, diffuseLight, diffuseLight, 1.0f};
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light_ambient, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light_diffuse, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{-lx, ly, lz, 0}, 0);

        } else {
            // FIXME
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_LIGHT0);

            float lmodel_ambient[] = {0f, 0f, 0f, 1.0f};
            gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
        }
    }

    public void setLight(float ambient, float diffuse) {
        this.ambientLight = ambient;
        this.diffuseLight = diffuse;
        this.getComponent().firePropertyChange("view", 0, 1);
    }

    public float getAmbientLight() {
        return ambientLight;
    }

    public float getDiffuseLight() {
        return diffuseLight;
    }

    public void setLightDirection(float azimuth, float zenith) {
        this.lightAzimuth = azimuth;
        this.lightZenith = zenith;
        this.getComponent().firePropertyChange("view", 0, 1);
    }

    public float getLightAzimuth() {
        return this.lightAzimuth;
    }

    public float getLightZenith() {
        return this.lightZenith;
    }

    public void defaultShading() {
        this.ambientLight = DEFAULT_LIGHT_AMBIENT;
        this.diffuseLight = DEFAULT_LIGHT_DIFFUSE;
        this.lightAzimuth = DEFAULT_LIGHT_AZIMUTH;
        this.lightZenith = DEFAULT_LIGHT_ZENITH;
    }

    private boolean installDebugGL(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        if (gl == null) {
            return false;
        }
        drawable.setGL(new DebugGL2((GL2)gl));
        System.out.println("OpenGL debug mode: glError() called automatically "
                + "after each API call.");
        return true;
    }

    /**
     * GLEventListener Initialize material property and light source.
     */
    @Override
    public void init(GLAutoDrawable drawable) {

        // for debugging only
        assert installDebugGL(drawable);

        GL gl1 = drawable.getGL();
        GL2 gl = (GL2)gl1;

        glu_callback_only = new GLU();

        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(this.bgRed, this.bgGreen, this.bgBlue, 1.0f);
        gl.glEnable(GL2.GL_DEPTH_TEST); // depth test must be enabled
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        setupLight(gl, this.lightAzimuth, this.lightZenith);
        if (this.shadingEnabled) {
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT);
            gl.glColor3f(0.5f, 0.5f, 0.5f);
            gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_DIFFUSE);
            gl.glColor3f(1.0f, 1.0f, 1.0f);
        } else {
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
        }
    }

    /**
     * Computes the height of an image section for the cylindrical projection.
     * The cylindrical projection is approximated by CYLINDER_IMAGES_COUNT image
     * sections.
     *
     * @param sectionWidth The width of a section
     * @return The height of a tile
     */
    private int cylinderSectionHeight(int sectionWidth) {
        // the horizontal section of the full circle that this tile displays
        double a = Math.toRadians(360f / getCylindricalImagesCount());

        // the vertical viewing angle
        // avoid tangens of Pi/2
        double viewAngleCorr = Math.min(fov, MAX_CYLINDER_FOV);
        double b = Math.toRadians(viewAngleCorr);

        // the height of the tile
        double h = sectionWidth * Math.tan(b / 2) / Math.tan(a / 2);
        return (int) h;
    }

    /**
     * GLEventListener Called by the drawable to initiate OpenGL rendering by
     * the client.
     *
     * @param drawable
     */
    @Override
    public void display(GLAutoDrawable drawable) {

        try {
            GL gl = drawable.getGL();

            gl.glClearColor(this.bgRed, this.bgGreen, this.bgBlue, 1.0f);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_ACCUM_BUFFER_BIT);

            if (camera == Camera.cylindrical) {
                displayCylindricalProjection(drawable);
            } else {
                display_(drawable, this.zAngle);
            }

            testOpenGLError(gl);
        } catch (Throwable e) {

            assert displayExtendedErrorDialog(e);

            // FIXME try catch block
            model.releaseModel(drawable.getGL());
            if (model instanceof Map3DModelVBOShader) {
                Map3DModel model2 = new Map3DModelVBO();
                model2.setModel(model.grid, model.cellSize, model.texture1DMapper);
                model = model2;
            }

            String errorTitle = "Rendering Error";
            Frame parent = GUIUtil.getOwnerFrame(getComponent());
            ErrorDialog.showErrorDialog(e.getMessage(), errorTitle, e, parent);

        }
    }

    private boolean displayExtendedErrorDialog(Throwable e) {
        String errorTitle = "Rendering Error";
        StringWriter sw = new StringWriter();
        sw.write(errorTitle);
        sw.write(System.getProperty("line.separator"));
        sw.write("Model: ");
        sw.write(model.getClass().getSimpleName());
        sw.write(System.getProperty("line.separator"));
        if (e.getMessage() != null) {
            sw.write(e.getMessage());
            sw.write(System.getProperty("line.separator"));
        }
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        Frame parent = GUIUtil.getOwnerFrame(getComponent());
        new TextWindow(parent, true, true, sw.toString(), errorTitle);
        return true;
    }

    /**
     * Throws an exception if an OpenGL error occured since the last call to
     * <code>glGetError</code>.
     *
     * @param gl
     */
    private void testOpenGLError(GL gl) {
        int errCode = gl.glGetError();
        if (errCode != GL2.GL_NO_ERROR) {
            StringBuilder sb = new StringBuilder("An OpenGL error occured: Code ");
            sb.append(errCode);
            sb.append(" - ");
            sb.append(glu_callback_only.gluErrorString(errCode));
            throw new RuntimeException(sb.toString());
        }
    }

    /**
     * Renders multiple perspective images to simulate a cylindrical projection.
     *
     * @param drawable
     */
    private void displayCylindricalProjection(GLAutoDrawable drawable) {

        GL gl = drawable.getGL();

        // store the initial viewport
        int[] vp = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, vp, 0);

        try {
            int compH = this.drawable.getContext().getGLDrawable().getHeight();
            int compW = this.drawable.getContext().getGLDrawable().getWidth();

            float zoom = (viewDistance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);

            // width and height of a single image
            int w = (int) (compW / getCylindricalImagesCount() / zoom);
            int h = (int) (cylinderSectionHeight(w) / zoom);

            // vertical position of the image stripe
            int y = (int) ((compH - h) / 2);

            // angle covered by each image
            float rotAngle = 360f / getCylindricalImagesCount();

            // render getCylindricalImagesCount() views, each rotated by rotAngle
            for (int i = 0; i < getCylindricalImagesCount(); i++) {

                // adjust the viewport: draw to a section of the available space
                gl.glViewport(i * w, y, w, h);

                // increas the rotation around the z axis
                float zAngle_ = this.zAngle - i * rotAngle;
                zAngle_ = Map3DViewer.normalizeZAngle(zAngle_);

                // render image
                display_(drawable, zAngle_);
                if (this.highResCylindricalRendering) {
                    System.out.println("Column: " + i);
                }
            }
        } finally {
            // restore the initial viewport
            gl.glViewport(vp[0], vp[1], vp[2], vp[3]);
        }
    }

    private void display_(GLAutoDrawable drawable, float zAngle) {

        if (this.model == null) {
            return;
        }

        GL gl1 = drawable.getGL();
        GL2 gl = (GL2)gl1;

        // use antialiasing when mouse is not being dragged.
        if (!this.mouseHandler.isDragging() && antialiasing) {
            gl.glEnable(GL2.GL_MULTISAMPLE);
        } else {
            gl.glDisable(GL2.GL_MULTISAMPLE);
        }

        this.setupProjection(gl);

        this.enableFog(gl);

        // with standart orientation: up vector 0/1/0
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        if (camera == Camera.cylindrical) {
            glu_callback_only.gluLookAt(0, 0, 0, 0, 1, 0, 0, 0, -1);
        } else {
            // position the camera at 0/0/viewDistance,
            // looking at 0/0/0
            glu_callback_only.gluLookAt(0, 0, viewDistance, 0, 0, 0, 0, 1, 0);
        }

        // load the texture if necessary
        boolean textureChanged = texture.constructTexture(gl);
        if (textureChanged) {
            model.textureChanged();
        }

        // hack for shearing with vertex shader
        if (model instanceof Map3DModelVBOShader) {
            if (camera != Camera.planOblique) {
                ((Map3DModelVBOShader) model).setShearing(0, 0);
            } else {
                ((Map3DModelVBOShader) model).setShearing(shearX, shearY);
            }
        }

        // construct the geometry model if necessary
        model.loadModel(gl, this.texture);

        float modelWidth = this.model.getNormalizedModelWidth();
        float modelHeight = this.model.getNormalizedModelHeight();
        final float dz;
        switch (camera) {
            case perspective:
            case parallelOblique:
            case cylindrical:
                dz = -model.getCenterElevation();
                break;
            default:
                dz = 0;
        }

        // transform light without shearing
        {
            gl.glPushMatrix();
            // rotate
            switch (camera) {
                case perspective:
                case parallelOblique:
                    gl.glRotatef(xAngle, 1.0f, 0.0f, 0.0f);
            }

            gl.glRotatef(zAngle, 0.0f, 0.0f, 1.0f);
            gl.glTranslatef(-modelWidth / 2, -modelHeight / 2, dz);
            float azimuth = this.lightAzimuth;
            if (bindLightDirectionToViewDirection) {
                azimuth -= zAngle;
            }
            setupLight(gl, azimuth, this.lightZenith);
            gl.glPopMatrix();
        }

        gl.glPushMatrix();
        // vertical and horizontal shift to place model in viewport
        // FIXME
        // hack for compensating missing vertical shift in Map3DModelVBOShader.gridTexture()
        // gl.glTranslatef(0, 0, this.model.getNormalizedMinimumValue());
        if (camera != Camera.cylindrical) {
            gl.glTranslatef(shiftX, shiftY, 0);
        }

        /*
         // shear for plan oblique rendering
         if (camera == Camera.planOblique && (shearX != 0 || shearY != 0)) {
         gl.glEnable(GL2.GL_NORMALIZE);
         shearMatrix(gl, shearX, -shearY);
         gl.glTranslatef(0, 0, -Map3DModel.ZOFFSET);
         }
         */
        // rotate
        switch (camera) {
            case perspective:
            case parallelOblique:
                gl.glRotatef(xAngle, 1, 0, 0);
                break;
        }
        gl.glRotatef(zAngle, 0.0f, 0.0f, 1.0f);

        // center position of cylindrical camera on origin
        if (camera == Camera.cylindrical) {
            gl.glTranslatef(-shiftX / 2f, -shiftY / 2f, -cylindricalHeight);
        }

        // center model on origin by shifting by half width and size of model
        gl.glTranslatef(-modelWidth / 2, -modelHeight / 2, dz);

        if (camera == Camera.planOblique) {
            gl.glEnable(GL2.GL_NORMALIZE);
            // translate for XY shearing, which is proportional to Z
            gl.glTranslatef(0, 0, -Map3DModel.ZOFFSET);
        }
                
        model.draw(gl, shadingEnabled, fogEnabled);

        gl.glPopMatrix();

        gl.glFlush();

        animation.update(this);
    }

    private void setupProjection(GL gl1) {
        GL2 gl = (GL2)gl1;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        switch (camera) {
            case perspective:
                double top = Math.tan(Math.toRadians(fov * 0.5)) * Z_NEAR;
                double bottom = -top;
                double left = aspectRatio_callback_only * bottom;
                double right = aspectRatio_callback_only * top;
                gl.glFrustum(left, right, bottom, top, Z_NEAR, Z_FAR);
                //glu_callback_only.gluPerspective(fov, aspectRatio_callback_only, Z_NEAR, Z_FAR);
                break;
            case cylindrical: {
                int compW = this.drawable.getContext().getGLDrawable().getWidth();
                int w = compW / getCylindricalImagesCount();
                int h = cylinderSectionHeight(w);
                float aspect = (float) w / h;
                double va = Math.min(fov, MAX_CYLINDER_FOV);
                glu_callback_only.gluPerspective(va, aspect, Z_NEAR, Z_FAR);
                break;
            }
            default:
                float w = aspectRatio_callback_only * viewDistance;
                float h = viewDistance;
                gl.glOrtho(-w / 2, w / 2, -h / 2, h / 2, 0/*Z_NEAR*/, Z_FAR);
        }

        gl.glScalef(1.0f, -1.0f, 1.0f); // this inverts the y coordinates of the grid. A MESS! FIXME
    }

    /**
     * GLEventListener Called by the drawable during the first repaint after the
     * component has been resized.
     *
     * @param drawable
     * @param x
     * @param y
     * @param w
     * @param h
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {

        // avoid division by zero
        if (h == 0) {
            h = 1;
        }

        GL gl1 = drawable.getGL();
        GL2 gl = (GL2)gl1;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glViewport(x, y, w, h);

        this.aspectRatio_callback_only = (float) w / (float) h;

        this.setupProjection(gl); // is this needed ? FIXME

    }

    void shearMatrix(GL gl1, float shearX, float shearY) {
        GL2 gl = (GL2)gl1;
        float m[] = {
            1, 0, 0, 0, // col 1
            0, 1, 0, 0, // col 2
            shearX, shearY, 1, 0, // col 3
            0, 0, 0, 1 // col 4
        };
        gl.glMultMatrixf(m, 0);
    }

    /**
     * Renders the map.
     */
    public void display() {
        if (this.model.isInitialized()) {
            this.updateView();
        }
    }

    /**
     * Set the background color. The color components are expected to be between
     * 0 and 255.
     *
     * @param red the red color component
     * @param green the green color component
     * @param blue the blue color component
     */
    public void setBackgroundColor(int red, int green, int blue) {
        this.bgRed = red / 255.0f;
        this.bgGreen = green / 255.0f;
        this.bgBlue = blue / 255.0f;
        this.component.repaint();
    }

    public void setBackgroundColor(Color bc) {
        this.bgRed = bc.getRed() / 255.0f;
        this.bgGreen = bc.getGreen() / 255.0f;
        this.bgBlue = bc.getBlue() / 255.0f;
        this.component.repaint();
    }

    public Color getBackgroundColor() {
        return new Color((int) (bgRed * 255), (int) (bgGreen * 255), (int) (bgBlue * 255));
    }

    public void resetToDefaultCamera() {
        this.setXAngle(defaultXAngle);
        this.setZAngle(defaultZAngle);
        this.setViewDistance(defaultDistance);
        this.setViewAngle(defaultViewAngle);
        this.setShearX(0);
        this.setShearY(0);
        this.setShiftX(defaultShiftX);
        this.setShiftY(defaultShiftY);
        this.setCylindricalHeight(defaultCylindricalHeight);
        this.component.repaint();
    }

    public void setDefaultCamera(float defaultXAngle,
            float defaultZAngle,
            float defaultDistance,
            float defaultViewAngle,
            float defaultShiftX,
            float defaultShiftY) {
        this.defaultXAngle = defaultXAngle;
        this.defaultZAngle = defaultZAngle;
        this.defaultDistance = defaultDistance;
        this.defaultViewAngle = defaultViewAngle;
        this.defaultShiftX = defaultShiftX;
        this.defaultShiftY = defaultShiftY;
    }

    public void setDefaultCylindricalCameraHeight(float defaultCylindricalHeight) {
        this.defaultCylindricalHeight = defaultCylindricalHeight;
    }

    public Map3DAnimation getAnimation() {
        return animation;
    }

    public void setAnimation(Map3DAnimation animation) {
        if (animation == this.animation) {
            return;
        }
        if (this.animation != null) {
            this.animation.stopAnimation();
        }
        this.animation = animation;
        this.component.addKeyListener(animation);
    }

    /**
     * Rotation angle around x axis.
     *
     * @return Angle in degrees
     */
    public float getXAngle() {
        return xAngle;
    }

    /**
     *
     * @param xAngle in degrees
     */
    public void setXAngle(float xAngle) {
        xAngle = Math.max(Math.min(xAngle, MAX_X_ANGLE), MIN_X_ANGLE);
        if (xAngle != this.xAngle) {
            this.xAngle = xAngle;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    /**
     * Returns the rotation angle around the z axis.
     *
     * @return Angle in degrees.
     */
    public float getZAngle() {
        return zAngle;
    }

    /**
     *
     * @param zAngle Angle in degrees.
     * @return
     */
    public static float normalizeZAngle(float zAngle) {
        while (zAngle > 180) {
            zAngle -= 360;
        }
        while (zAngle < -180) {
            zAngle += 360;
        }
        return zAngle;
    }

    /**
     * Rotation around vertical z axis.
     *
     * @param zAngle Angle in degrees.
     */
    public void setZAngle(float zAngle) {
        zAngle = normalizeZAngle(zAngle);
        if (zAngle != this.zAngle) {
            this.zAngle = zAngle;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    public float getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(float viewDistance) {
        viewDistance = Math.min(Math.max(viewDistance, MIN_DISTANCE), MAX_DISTANCE);
        if (viewDistance != this.viewDistance) {
            this.viewDistance = viewDistance;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    public float getViewAngle() {
        return fov;
    }

    public void setViewAngle(float viewAngle) {
        if (viewAngle != this.fov) {
            this.fov = viewAngle;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    public float getShearXAngle() {
        float angle = (float) (Math.toDegrees(Math.atan2(1, shearX)));
        return Math.abs(angle) < 0.000001 ? 0f : angle;
    }

    public void setShearXAngle(float shearXAngle) {
        shearXAngle = Math.min(Math.max(shearXAngle, MIN_SHEAR_ANGLE), MAX_SHEAR_ANGLE);
        this.setShearX((float) (1. / Math.tan(shearXAngle)));
    }

    public float getShearX() {
        return shearX;
    }

    public void setShearX(float shearX) {
        if (this.shearX != shearX) {
            this.shearX = shearX;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    public float getShearYAngle() {
        float angle = (float) (Math.toDegrees(Math.atan2(1, shearY)));
        return Math.abs(angle) < 0.000001 ? 0f : angle;
    }

    public void setShearYAngle(float shearYAngle) {
        shearYAngle = Math.min(Math.max(shearYAngle, MIN_SHEAR_ANGLE), MAX_SHEAR_ANGLE);
        this.setShearY((float) (1. / Math.tan(Math.toRadians(shearYAngle))));
    }

    public float getShearY() {
        return shearY;
    }

    public void setShearY(float shearY) {
        if (this.shearY != shearY) {
            this.shearY = shearY;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    public float getShearDirection() {
        return (float) Math.toDegrees(Math.atan2(shearY, shearX));
    }

    public float getShearRadius() {
        return (float) Math.hypot(shearY, shearX);
    }

    public void setCamera(Camera camera) {
        if (this.camera != camera) {
            this.camera = camera;
            this.updateView();
            getComponent().firePropertyChange("camera changed", 0, 1);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean is2D() {
        return camera == Camera.orthogonal;
    }

    public float getShiftX() {
        return shiftX;
    }

    public void setShiftX(float shiftX) {
        if (this.shiftX != shiftX) {
            this.shiftX = shiftX;
            this.updateView();
            this.getComponent().firePropertyChange("shiftX", 0, 1);
        }
    }

    public float getShiftY() {
        return shiftY;
    }

    public void setShiftY(float shiftY) {
        if (this.shiftY != shiftY) {
            this.shiftY = shiftY;
            this.updateView();
            this.getComponent().firePropertyChange("shiftY", 0, 1);
        }
    }

    public void setShift(float shiftX, float shiftY) {
        if (this.shiftX != shiftX || this.shiftY != shiftY) {
            this.shiftX = shiftX;
            this.shiftY = shiftY;
            this.updateView();
            this.getComponent().firePropertyChange("view", 0, 1);
        }
    }

    /**
     * @return the cylindricalHeight
     */
    public float getCylindricalHeight() {
        return cylindricalHeight;
    }

    /**
     * @param cylindricalHeight the cylindricalHeight to set
     */
    public void setCylindricalHeight(float h) {
        if (this.cylindricalHeight != h) {
            this.cylindricalHeight = h;
            this.updateView();
            this.getComponent().firePropertyChange("height", 0, 1);
        }
    }

    private void updateView() {
        this.component.repaint();
    }

    /**
     * @return the fogEnabled
     */
    public boolean isFogEnabled() {
        return fogEnabled;
    }

    /**
     * @param fogEnabled the fogEnabled to set
     */
    public void setFogEnabled(boolean fogEnabled) {
        this.fogEnabled = fogEnabled;

        this.updateView();
        this.getComponent().firePropertyChange("fog", 0, 1);
    }

    /**
     * @return the fogStart
     */
    public float getFogStart() {
        return fogStart;
    }

    /**
     * @param fogStart the fogStart to set
     */
    public void setFogStart(float fogStart) {
        this.fogStart = fogStart;
        this.updateView();
        this.getComponent().firePropertyChange("fogStart", 0, 1);
    }

    /**
     * @return the fogEnd
     */
    public float getFogEnd() {
        return fogEnd;
    }

    /**
     * @param fogEnd the fogEnd to set
     */
    public void setFogEnd(float fogEnd) {
        this.fogEnd = fogEnd;
        this.updateView();
        this.getComponent().firePropertyChange("fogEnd", 0, 1);
    }

    /**
     * @return the fogColor
     */
    public Color getFogColor() {
        return fogColor;
    }

    /**
     * @param fogColor the fogColor to set
     */
    public void setFogColor(Color fogColor) {
        this.fogColor = fogColor;
        this.updateView();
        this.getComponent().firePropertyChange("fogColor", 0, 1);
    }

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
  /**
   * FIXME: Hackish method for taking a mouse position and mapping
   * it to a normalized location on the model.
   * (a Point2D.Float with x & y values between [0.0-1.0])
   * TODO: handle screen-to-map transformation when rotated    
   */
  protected Point2D.Float mouseXYtoModelXY(float x, float y){
      int componentWidth = getComponent().getWidth();
      int componentHeight = getComponent().getHeight();
      float normalizedX = x / componentWidth;
      float normalizedY = y / componentHeight;
      float whRatio = (float)componentWidth / componentHeight;
      float mapX = (normalizedX - 0.5f) * getViewDistance()*whRatio + 0.5f - getShiftX();
      float mapY = (normalizedY - 0.5f) * getViewDistance() + 0.5f - getShiftY();
      return new Point2D.Float(mapX, mapY);
  }
  
  /**
  * Find the model coordinates corresponding to current viewport.
  * FIXME: Depends on mouseXYtoModelXY, which only works with no rotations.
  */
  public Rectangle2D.Float getViewBounds(){
    int componentWidth = getComponent().getWidth();
    int componentHeight = getComponent().getHeight();
    Point2D.Float upperLeft = mouseXYtoModelXY(0,0);
    Point2D.Float lowerRight = mouseXYtoModelXY(componentWidth, componentHeight);
    
    return new Rectangle2D.Float(upperLeft.x, upperLeft.y, 
                                 lowerRight.x - upperLeft.x, lowerRight.y - upperLeft.y);
  }
}
