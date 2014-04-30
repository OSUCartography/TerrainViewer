package ch.ethz.karto.map3d;

import com.jogamp.opengl.util.FPSAnimator;
import java.awt.event.KeyEvent;
import javax.media.opengl.GLAutoDrawable;

/**
 *
 * @author Institute of Cartography, ETH Zurich.
 */
public class Map3DRotationAnimation implements Map3DAnimation {

    private FPSAnimator animator;
    private GLAutoDrawable glAutoDrawable;
    
    /**
     * Increment for the xAngle when animating
     */
    protected float xDelta = 0.0f;
    /**
     * Increment for the zAngle when animating
     */
    private float zDelta = 0.0f;
    /**
     * Increment for the viewDistance when animating
     */
    private float dDelta = 0.0f;
    private int xLevel = 0;
    private int zLevel = 0;
    private int dLevel = 0;
    /**
     * Number of frames per second.
     */
    protected static final int FPS = 50;
    /**
     * Duration of full rotation in seconds.
     */
    private static final float ROTATION_DURATION = 4.0f;
    /**
     * Duration of full elevation change in seconds.
     */
    private static final float ELEVATION_DURATION = 2.0f;
    /**
     * Duration of full elevation change in seconds.
     */
    private static final float DISTANCE_DURATION = 2.0f;
    /**
     * Rotation steps.
     */
    private static final int ROTATION_STEPS = 16;
    /**
     * Rotation steps.
     */
    private static final int ELEVATION_STEPS = 8;
    /**
     * Distance steps.
     */
    private static final int DISTANCE_STEPS = 8;
    /**
     * Rotation units [in degrees].
     */
    private static final float ROTATION_UNITS = 360.0f / (ROTATION_DURATION * FPS * ROTATION_STEPS * ROTATION_STEPS);
    /**
     * Elevation units [in degrees].
     */
    private static final float ELEVATION_UNITS = 90.0f / (ELEVATION_DURATION * FPS * ELEVATION_STEPS * ELEVATION_STEPS);
    /**
     * Distance units.
     */
    private static final float DISTANCE_UNITS = (Map3DViewer.MAX_DISTANCE - Map3DViewer.MIN_DISTANCE) / (DISTANCE_DURATION * FPS * DISTANCE_STEPS * DISTANCE_STEPS);

    protected Map3DRotationAnimation(GLAutoDrawable glAutoDrawable) {
        this.glAutoDrawable = glAutoDrawable;
    }

    @Override
    public void update(Map3DViewer map3DViewer) {

        if (!isAnimating()) {
            return;
        }
        
        // this method is called from Map3DViewer.display(). Do not call methods
        // in Map3DView that again call display()!
        
        if (map3DViewer.xAngle + xDelta > Map3DViewer.MAX_X_ANGLE 
                || map3DViewer.xAngle + xDelta < Map3DViewer.MIN_X_ANGLE) {
            xDelta = -xDelta;
        }
        map3DViewer.xAngle += xDelta;
        /*if (xAngle > MAX_X_ANGLE || xAngle < MIN_X_ANGLE) {
        xLevel = 0;
        xDelta = 0.0f;
        xAngle = Math.max(Math.min(xAngle, MAX_X_ANGLE), MIN_X_ANGLE);
        }
        
        if (xAngle > MAX_X_ANGLE || xAngle < MIN_X_ANGLE) {
        xLevel = 0;
        xDelta = 0.0f;
        xAngle = Math.max(Math.min(xAngle, MAX_X_ANGLE), MIN_X_ANGLE);
        }*/

        map3DViewer.zAngle = Map3DViewer.normalizeZAngle(map3DViewer.zAngle - zDelta);

        map3DViewer.viewDistance += dDelta;
        if (map3DViewer.viewDistance > Map3DViewer.MAX_DISTANCE || map3DViewer.viewDistance < Map3DViewer.MIN_DISTANCE) {
            dLevel = 0;
            dDelta = 0.0f;
            map3DViewer.viewDistance = Math.max(Math.min(map3DViewer.viewDistance, Map3DViewer.MAX_DISTANCE), Map3DViewer.MIN_DISTANCE);
        }
        map3DViewer.getComponent().repaint();
        map3DViewer.getComponent().firePropertyChange("view", 0, 1);
    }

    protected void changeRotation(int d) {
        if ((d < 0 && this.zLevel > 0) || (d > 0 && this.zLevel < 0)) {
            this.zLevel = 0;
        } else {
            this.zLevel = Math.min(Math.max(this.zLevel + d, -ROTATION_STEPS), ROTATION_STEPS);
        }
        this.zDelta = (this.zLevel * this.zLevel) * ROTATION_UNITS;
        this.zDelta = this.zLevel < 0 ? -this.zDelta : this.zDelta;
    }

    protected void increaseRightRotation() {
        changeRotation(1);
    }

    protected void increaseLeftRotation() {
        changeRotation(-1);
    }

    protected void changeElevation(int d) {
        if ((d < 0 && this.xLevel > 0) || (d > 0 && this.xLevel < 0)) {
            this.xLevel = 0;
        } else {
            this.xLevel = Math.min(Math.max(this.xLevel + d, -ELEVATION_STEPS), ELEVATION_STEPS);
        }
        this.xDelta = (this.xLevel * this.xLevel) * ELEVATION_UNITS;
        this.xDelta = this.xLevel < 0 ? -this.xDelta : this.xDelta;
    }

    protected void increaseElevation() {
        changeElevation(1);
    }

    protected void decreaseElevation() {
        changeElevation(-1);
    }

    protected void changeZoom(int d) {
        if ((d < 0 && this.dLevel > 0) || (d > 0 && this.dLevel < 0)) {
            this.dLevel = 0;
        } else {
            this.dLevel = Math.min(Math.max(this.dLevel + d, -DISTANCE_STEPS), DISTANCE_STEPS);
        }
        this.dDelta = (this.dLevel * this.dLevel) * DISTANCE_UNITS;
        this.dDelta = this.dLevel < 0 ? -this.dDelta : this.dDelta;
    }

    protected void zoomIn() {
        changeZoom(-1);
    }

    protected void zoomOut() {
        changeZoom(1);
    }

    /**
     * Starts the animation.
     */
    @Override
    public void startAnimation() {

        if (this.animator == null) {
            this.animator = new FPSAnimator(glAutoDrawable, FPS);
        }
        if (!this.animator.isAnimating()) {
            this.animator.start();
        }
    }

    /**
     * Stops the animation.
     */
    @Override
    public void stopAnimation() {

        if (animator != null && this.animator.isAnimating()) {
            this.animator.stop();
        }
        this.xLevel = 0;
        this.xDelta = 0.0f;
        this.zLevel = 0;
        this.zDelta = 0.0f;
        this.dLevel = 0;
        this.dDelta = 0.0f;

        glAutoDrawable.display();
    }

    protected void toggleAnimation() {
        if (this.animator != null && this.animator.isAnimating()) {
            stopAnimation();
        } else {
            startAnimation();
        }
    }

    public boolean isAnimating(){
        return animator != null && this.animator.isAnimating();
    }
    
    /**
     * Treat plus and minus keys to zoom in and out.
     * Special treatment needed:
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4262044
     * @param key
     */
    @Override
    public void keyTyped(KeyEvent key) {

        switch (key.getKeyChar()) {
            case '+':
                this.startAnimation();
                this.zoomIn();
                break;
            case '-':
                this.startAnimation();
                this.zoomOut();
                break;
            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent key) {

        switch (key.getKeyCode()) {

            case KeyEvent.VK_UP:
                this.startAnimation();
                if (key.isShiftDown()) {
                    this.zoomIn();
                } else {
                    this.increaseElevation();
                }
                break;

            case KeyEvent.VK_DOWN:
                this.startAnimation();
                if (key.isShiftDown()) {
                    this.zoomOut();
                } else {
                    this.decreaseElevation();
                }
                break;

            case KeyEvent.VK_RIGHT:
                this.startAnimation();
                this.increaseRightRotation();
                break;

            case KeyEvent.VK_LEFT:
                this.startAnimation();
                this.increaseLeftRotation();
                break;

            case KeyEvent.VK_SPACE:
                this.toggleAnimation();
                break;

            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_DELETE:
                //resetView();
                break;

            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent key) {
    }

}
