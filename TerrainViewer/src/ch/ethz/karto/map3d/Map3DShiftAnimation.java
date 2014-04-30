/*
 * 
 * 
 */
package ch.ethz.karto.map3d;

import javax.media.opengl.GLAutoDrawable;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DShiftAnimation extends Map3DRotationAnimation {

    private static final float SHIFT_SPEED = 2;
    protected float targetShiftX;
    protected float targetShiftY;
    private int updatesSinceStart = 0;
    private int updatesSinceHalt = 0;
    private static final int NUPDATES = 10;
    private static final double SHEAR_ANIMATION_FRAMES = 30;

    protected Map3DShiftAnimation(Map3DViewer map3DViewer, GLAutoDrawable glAutoDrawable) {
        super(glAutoDrawable);
        targetShiftX = map3DViewer.shiftX;
        targetShiftY = map3DViewer.shiftY;
    }

    @Override
    public void startAnimation() {
        System.out.println("shift animation started");
        super.startAnimation();
        this.updatesSinceStart = 0;
    }
    
    public void animateShift(float targetShiftX, float targetShiftY) {
        this.targetShiftX = targetShiftX;
        this.targetShiftY = targetShiftY;
    }

    @Override
    public void update(Map3DViewer map3DViewer) {

        // this method is called from Map3DViewer.display(). Do not call methods
        // in Map3DView that again call display()!

        //super.update(map3DViewer);

        float dx = (this.targetShiftX - map3DViewer.shiftX) / NUPDATES;
        map3DViewer.shiftX += dx;

        float dy = (this.targetShiftY - map3DViewer.shiftY) / NUPDATES;
        map3DViewer.shiftY += dy;
        double dShift = Math.hypot(dx, dy);
        if (dShift < 0.0001) {
            ++updatesSinceHalt;
        } else {
            updatesSinceHalt = 0;
        }
        
        System.out.println(updatesSinceHalt);

        double shearAngle;
        if (updatesSinceHalt > 30) {
            shearAngle = 90. - 30 * Math.max((updatesSinceHalt - 30.) * -1./SHEAR_ANIMATION_FRAMES + 1, 0);
        } else {
            shearAngle = 90. - 30. * Math.min((double)updatesSinceStart / SHEAR_ANIMATION_FRAMES, 1);
        }
        
        System.out.println(shearAngle);
        double d = 1. / Math.tan(Math.toRadians(shearAngle));
        double dir = Math.atan2(dy, dx);
        map3DViewer.shearX = (float) (Math.cos(dir) * d);
        map3DViewer.shearY = -(float) (Math.sin(dir) * d);


        ++updatesSinceStart;
    /*
    if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
    this.stopAnimation();
    }
     */    }
}
