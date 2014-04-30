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
public class Map3DShearAnimation extends Map3DRotationAnimation {

    /**
     * Duration of a full rotation in seconds.
     */
    private float cycleDuration = 2f;
    private float animationAmplitude = 1f;
    private boolean inverseDirection = false;
    private int nRosePetals = 3;

    public enum AnimationType {

        updown, leftright, circular, rose
    };
    private AnimationType animationType = AnimationType.updown;

    public Map3DShearAnimation(GLAutoDrawable glAutoDrawable) {
        super(glAutoDrawable);
    }

    @Override
    public void update(Map3DViewer map3DViewer) {

        // this method is called from Map3DViewer.display(). Do not call methods
        // in Map3DView that again call display()!

        super.update(map3DViewer);

        if (cycleDuration <= 0 || !this.isAnimating()) {
            return;
        }

        switch (this.animationType) {
            case circular:
                this.circularAnimation(map3DViewer);
                break;
            case leftright:
                this.leftRightAnimation(map3DViewer);
                break;
            case updown:
                this.updownAnimation(map3DViewer);
                break;
            case rose:
                this.roseAnimation(map3DViewer);
                break;
        }

    }

    private void circularAnimation(Map3DViewer map3DViewer) {

        float dirDelta = 360.f / (cycleDuration * FPS);
        double dir = Math.toDegrees(Math.atan2(map3DViewer.shearY, map3DViewer.shearX));
        dir += dirDelta;
        if (dir > 180) {
            dir -= 360;
        }
        dir = Math.toRadians(dir);
        map3DViewer.shearX = (float) (Math.cos(dir)) * animationAmplitude;
        map3DViewer.shearY = (float) (Math.sin(dir)) * animationAmplitude;

    }

    private void updownAnimation(Map3DViewer map3DViewer) {

        float d = 2f * animationAmplitude / (cycleDuration * FPS);
        if (inverseDirection) {
            d = -d;
        }
        float newShear = map3DViewer.shearY + d;
        if (newShear > animationAmplitude) {
            d = -d;
            map3DViewer.shearY = animationAmplitude;
            inverseDirection = !inverseDirection;
        }
        if (newShear < -animationAmplitude) {
            d = -d;
            map3DViewer.shearY = -animationAmplitude;
            inverseDirection = !inverseDirection;
        }
        map3DViewer.shearY += d;
        map3DViewer.shearX = 0;

    }

    private void leftRightAnimation(Map3DViewer map3DViewer) {

        float d = 2f * animationAmplitude / (cycleDuration * FPS);
        if (inverseDirection) {
            d = -d;
        }
        float newShear = map3DViewer.shearX + d;
        if (newShear > animationAmplitude || newShear < -animationAmplitude) {
            d = -d;
            inverseDirection = !inverseDirection;
        }
        map3DViewer.shearY = 0;
        map3DViewer.shearX += d;

    }
    double roseDir = 0;

    private void roseAnimation(Map3DViewer map3DViewer) {

        int n = nRosePetals % 2 == 1 ? nRosePetals : nRosePetals / 2;
        double dirDelta = Math.toRadians(360.f / (cycleDuration * FPS) / n);
        roseDir += dirDelta;
        double r = Math.cos(n * roseDir) * animationAmplitude;
        map3DViewer.shearX = (float) (Math.cos(roseDir) * r);
        map3DViewer.shearY = (float) (Math.sin(roseDir) * r);

    }

    public float getCycleDuration() {
        return cycleDuration;
    }

    public void setCycleDuration(float cycleDuration) {
        if (cycleDuration < 0) {
            throw new IllegalArgumentException();
        }
        this.cycleDuration = cycleDuration;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }

    public void setAnimationType(AnimationType animationType) {
        this.animationType = animationType;
    }

    public float getAnimationAmplitude() {
        return animationAmplitude;
    }

    public void setAnimationAmplitude(float animationAmplitude) {
        this.animationAmplitude = animationAmplitude;
    }

    public int getNRosePetals() {
        return nRosePetals;
    }

    public void setNRosePetals(int nRosePetals) {
        this.nRosePetals = nRosePetals;
    }
}