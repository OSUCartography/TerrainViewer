package ch.ethz.karto.map3d;

import java.awt.event.KeyListener;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public interface Map3DAnimation extends KeyListener {

    /**
     * Starts the animation.
     */
    void startAnimation();
    
    /**
     * Stops the animation.
     */
    void stopAnimation();

    /**
     * Update the viewing parameters of the passed Map3DViewer
     * @param map3DViewer
     */
    void update(Map3DViewer map3DViewer);

}
