package ch.ethz.karto.map3d;

import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

/**
 * Mouse event support for a Map3DViewer. Rotate and zoom for a 3D model.
 * @author Benrhard Jenny, Institute of Cartography, ETH Zurich.
 */
public final class Map3DMouseHandler extends MouseInputAdapter implements MouseWheelListener {

    /**
     * Divide the horizontal mouse movemement by this factor to convert to a
     * rotation angle around the z axis.
     */
    private static final float Z_MOVEMENT_SCALE = 20;
    /**
     * Divide the vertical mouse movemement by this factor to convert to a
     * rotation angle around the x axis.
     */
    private static final float X_MOVEMENT_SCALE = 10;
    /**
     * Divide the number of ticks the scroll wheel was turned by this factor to
     * convert to the zoom factor of the map.
     */
    private static final float SCROLL_WHEEL_SCALE = 20;
    /** 
     * Scale the size of the shear effect produced when manually shearing. 
     */    
    private static final float SHEAR_COEFFICIENT = 1f;
    /**
     * Animation rate in (frames/second).
     */
    private static final int ANIMATION_FRAMERATE = 30;
    /**
     * Used by spring to control shearing. Fraction of velocity maintained each step.
     */
    private static final float FRICTION_CONSTANT = 0.75f;
    /**
     * Used by spring to control shearing. Fraction of distance between points to impart as velocity.
     */
    private static final float SPRING_CONSTANT = 0.05f;
    /**
     * Used to control the rate of speed when animating zoom (z-per-second).
     */
    private static final float ZOOM_SPEED = 2f;
    /**
     * The map being controlled.
     */
    private Map3DViewer map3DViewer;
    /**
     * Remembers the position of the last mouse event.
     */
    private Point lastPoint = null;
    /**
     * Remembers the position of the mouse at the beginning of the last press. 
     */
    private Point startPoint = null;
    /**
     * Remembers the elevation (in model z) at the beginning of the last press. 
     */
    private float startZ;
    /**
     * Remembers the X and Y shift of the map at the beginning of a the last press.
     */
    private float startShiftX;
    private float startShiftY;
    /**
     * Remembers the current velocity in X and Y when animating a shear/pan.
     */
    private float currentVelocityX = 0;
    private float currentVelocityY = 0;
    private float currentAnimatedShiftX = 0;
    private float currentAnimatedShiftY = 0;
    /**
     * Remember the state, start and end distances when animating zoom;
     */
    private boolean zoomIsAnimating = false;
    private float zoomEndDistance = 0;
    
    /**
     * Is zoom currently in the process of animating.
     */
    private boolean shearIsAnimating = false;
    /** 
     * Controls whether the shear-and-pan animation is run.
     */
    private boolean shearAnimationOn = false;
    /** 
     * Controls whether zoom animation is run.
     */
    private boolean zoomAnimationOn = true;
    /**
     * Controls whether shear and shear-and-pan should be reversed (e.g. for low points).
     */
    private boolean shearReversed = false;
    
    
    /**
     * Construct a new Map3DMouseHandler.
     * @param map3DViewer The map to rotate and zoom.
     */
    public Map3DMouseHandler(Map3DViewer map3DViewer) {
        this.map3DViewer = map3DViewer;
        
        //FIXME: Animation routines should really live outside the handler.
        //Animate shearing
        ActionListener animationPolling = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {        
                if(shearIsAnimating) animateShearWithSpring();
                if(zoomAnimationOn && zoomIsAnimating) animateZoom();
            }
        };
        new Timer(1000/ANIMATION_FRAMERATE, animationPolling).start();
        
        
    }

    public boolean isDragging() {
        return lastPoint != null;
    }

    /**
     * Mouse movements pan by default.
     * Shift key causes rotation around the vertical z axis and around the horizontal x axis.
     * Meta key causes the shear to change.
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastPoint == null || map3DViewer == null) {
            return;
        }
        
        //Holding shift rotates the view
        if (e.isShiftDown()) {
            if (this.map3DViewer.is2D()) {
                rotate2D(e);
            } else {
                rotate3D(e);
            }
        } 
        //Holding alt forces a pan (already the default in most modes)
        else if (e.isAltDown()){
            pan(e);
        }
        //Holding down the meta (or executing a right click or two-finger press) shears in plan oblique
        else if (e.isMetaDown() && map3DViewer.getCamera() == Map3DViewer.Camera.planOblique) {
            shear(e);
        }
        //Otherwise, pan in all modes except plan oblique (where shearing animation is used instead)
        else {//if(map3DViewer.getCamera() != Map3DViewer.Camera.planOblique){
            pan(e);
        }
        
        this.lastPoint = e.getPoint();
    }
  
    /**
     * Remember the point where the dragging starts.
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e) {
        this.startPoint = e.getPoint();
        this.lastPoint = e.getPoint();
        this.shearIsAnimating = false;
        
        if(shearAnimationOn && 
            map3DViewer.getCamera() == Map3DViewer.Camera.planOblique && 
            !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown()){
            this.shearIsAnimating = true;
            this.currentVelocityX = 0;
            this.currentVelocityY = 0;
        }
        this.startShiftX = map3DViewer.getShiftX();
        this.startShiftY = map3DViewer.getShiftY();
        
        //FIXME: A few shearing experiments
        if(map3DViewer.getCamera() == Map3DViewer.Camera.planOblique && e.isMetaDown()){
            Point2D.Float startPosition = this.map3DViewer.mouseXYtoModelXY(lastPoint.x, lastPoint.y);
            startZ = map3DViewer.getModel().z(startPosition.x, startPosition.y);
            //(1) shearing centered around a set elevation
            //((Map3DModelVBOShader) map3DViewer.getModel()).setShearBaseline(startZ);

            //(2) shear reversal based on local max/min detector
            //shearReversed = isLocalMinMax(this.map3DViewer.getModel(), startPosition,100,0.6f) != -1;
            Rectangle2D.Float view = this.map3DViewer.getViewBounds();
            shearReversed = (localRelativeHeight(this.map3DViewer.getModel(), view, startPosition) <= 0);
            if(shearReversed)
                System.out.println("Using reverse shear.");
            else
                System.out.println("Using normal shear.");
        }
    }

    /**
     * Release the last position of the mouse.
     * @param e
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        boolean mouseMoved = (this.startPoint.x != this.lastPoint.x) || 
                             (this.startPoint.y != this.lastPoint.y);
        //this.startPoint = null;
        //this.lastPoint = null;

        // Reset shear if meta is down
        if (e.isMetaDown()) {
            resetShear();
        }
                
        // redraw with antialiasing enabled
        if (map3DViewer.isAntialiasing())
            this.map3DViewer.display();
    }

    /**
     * Zoom in and out.
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        float viewDist = this.map3DViewer.getViewDistance();
        float viewDistChange = e.getWheelRotation() / SCROLL_WHEEL_SCALE;
        if(zoomAnimationOn){
          if(!zoomIsAnimating){
            zoomEndDistance = viewDist + viewDistChange;
            zoomIsAnimating = true;
          }
          else zoomEndDistance += viewDistChange;
        }
        else this.map3DViewer.setViewDistance(viewDist + viewDistChange);
    }
    
      private void rotate2D(MouseEvent e) {
        int w = this.map3DViewer.getComponent().getWidth();
        int h = this.map3DViewer.getComponent().getHeight();
        float zAngle = this.map3DViewer.getZAngle();
        double lastAngle = Math.atan2(lastPoint.y - h / 2, lastPoint.x - w / 2);
        double newAngle = Math.atan2(e.getY() - h / 2, e.getX() - w / 2);
        zAngle += (float) Math.toDegrees(newAngle - lastAngle);
        this.map3DViewer.setZAngle(zAngle);
    }

    private void pan(MouseEvent e) {
        int h = this.map3DViewer.getComponent().getHeight();
        float dx = (float) (lastPoint.x - e.getX()) / h * map3DViewer.viewDistance;
        float dy = (float) (lastPoint.y - e.getY()) / h * map3DViewer.viewDistance;
        map3DViewer.setShift(map3DViewer.getShiftX() - dx, map3DViewer.getShiftY() - dy);
    }
    
    private void shear(MouseEvent e){
        
        Point2D.Float startPosition = this.map3DViewer.mouseXYtoModelXY(startPoint.x, startPoint.y);
        Point2D.Float currentPosition = this.map3DViewer.mouseXYtoModelXY(e.getX(),e.getY());
        float distanceX = currentPosition.x - startPosition.x;
        float distanceY = currentPosition.y - startPosition.y;
        
        //Shear in X and Y so the clicked point stays under the mouse.
        if(startZ > 0 && startZ < 1){
            if(!shearReversed){
              //shear in the positive direction for high points
              map3DViewer.setShearX(SHEAR_COEFFICIENT * distanceX / startZ);
              map3DViewer.setShearY(SHEAR_COEFFICIENT * distanceY / startZ);
            }
            else{
              //shear in the negative direction for other points
              map3DViewer.setShearX(-SHEAR_COEFFICIENT * distanceX / startZ);
              map3DViewer.setShearY(-SHEAR_COEFFICIENT * distanceY / startZ);
              map3DViewer.setShift(startShiftX + 2*distanceX, 
                                   startShiftY + 2*distanceY);
            }
        }
    }

    private void animateZoom(){
      float viewDist = this.map3DViewer.getViewDistance();
      float zoomAmt = ZOOM_SPEED / ANIMATION_FRAMERATE;
      if(Math.abs(this.zoomEndDistance - viewDist) <= zoomAmt){
        this.map3DViewer.setViewDistance(this.zoomEndDistance);
        zoomIsAnimating = false;
      }
      else{
        float dir = viewDist < zoomEndDistance ? 1f : -1f;
        this.map3DViewer.setViewDistance(viewDist + dir*zoomAmt);
      }
    }
    
    
    private void animateShearWithSpring(){
       
        int w = this.map3DViewer.getComponent().getWidth();
        int h = this.map3DViewer.getComponent().getHeight();
        
        if(startPoint == null) return;
        
        //Find distance between current mouse position and point being animated
        Point2D.Float startPosition = this.map3DViewer.mouseXYtoModelXY(startPoint.x, startPoint.y);
        Point2D.Float currentPosition = this.map3DViewer.mouseXYtoModelXY(lastPoint.x,lastPoint.y);
        Point2D.Float animatingPosition = new Point2D.Float(startPosition.x + currentAnimatedShiftX - startShiftX,
                                                            startPosition.y + currentAnimatedShiftY - startShiftY);
        float distanceX = currentPosition.x - animatingPosition.x;
        float distanceY = currentPosition.y - animatingPosition.y;
        
        //Compute velocities - modeling the interaction between the current 
        // mouse position and the start position as a simple spring with length 0
        // (elastic force between the two, but no resistive force, only friction).
        currentVelocityX *= FRICTION_CONSTANT;
        currentVelocityY *= FRICTION_CONSTANT;
        currentVelocityX += distanceX * SPRING_CONSTANT;
        currentVelocityY += distanceY * SPRING_CONSTANT;  
        if(Math.abs(currentVelocityX) < 0.0001) currentVelocityX = 0;
        if(Math.abs(currentVelocityY) < 0.0001) currentVelocityY = 0;
        
        //Shift the viewer by the new velocity
        currentAnimatedShiftX += currentVelocityX;
        currentAnimatedShiftY += currentVelocityY;
                
        //Recompute distances after the new velocity is applied.
        animatingPosition = new Point2D.Float(startPosition.x + currentAnimatedShiftX - startShiftX,
                                              startPosition.y + currentAnimatedShiftY - startShiftY);
        distanceX = currentPosition.x - animatingPosition.x;
        distanceY = currentPosition.y - animatingPosition.y;
        
        //Shear in X and Y so the clicked point stays under the mouse.
        if(startZ > 0 && startZ < 1){
          if(!shearReversed){
            map3DViewer.setShearX(distanceX / startZ);
            map3DViewer.setShearY(distanceY / startZ);
            map3DViewer.setShift(currentAnimatedShiftX, currentAnimatedShiftY);
          }
          else{
            map3DViewer.setShearX(-1 * distanceX / startZ);
            map3DViewer.setShearY(-1 * distanceY / startZ);
            map3DViewer.setShift(currentAnimatedShiftX + 2*distanceX, 
                                 currentAnimatedShiftY + 2*distanceY);
          }
        }
        else map3DViewer.setShift(currentAnimatedShiftX, currentAnimatedShiftY);
        
    }

    private void resetShear(){
        this.map3DViewer.setShearY(0);
        this.map3DViewer.setShearX(0);
        this.map3DViewer.setShiftX(this.startShiftX);
        this.map3DViewer.setShiftY(this.startShiftY);
    }
    
    private void rotate3D(MouseEvent e) {
        float zAngle = this.map3DViewer.getZAngle();
        float xAngle = this.map3DViewer.getXAngle();
        zAngle += (lastPoint.x - e.getX()) / Z_MOVEMENT_SCALE;
        xAngle += (lastPoint.y - e.getY()) / X_MOVEMENT_SCALE;
        this.map3DViewer.setZAngle(zAngle);
        this.map3DViewer.setXAngle(xAngle);
    }
    

    
    /**
     * FIXME: Hackish method for determining whether the specified point is
     * a local minimum or maximum. Samples z values for all points in a radius
     * around some position. If more than the fraction specified in 'threshold'
     * are greater than the z value at the point, return -1 (local min). If more
     * than that fraction are below the point, return 1 (local max). Otherwise
     * return 0;
     * @param position
     * @param radius
     * @param threshold
     * @return
     */
    public static int isLocalMinMax(Map3DModel model, Point2D.Float position, int radius, float threshold) {
     
      //FIXME: Naive first stab - ACTUALLY DOESN'T WORK
      float s = (Math.max(model.getCols(), model.getRows()) - 1);
      int col = (int)Math.round(position.x * s);
      int row = (int)Math.round(position.y * s);
      float positionZ = model.z(col, row);
      int sampledPoints = 0;
      int gtPoints = 0;
      int ltPoints = 0;
      
      for(int i=col - radius; i < col + radius; i++){
        for(int j=row - radius; j < row + radius; j++){
          float sampleZ = model.z(i,j);
          if(sampleZ == 0) continue; //typically out-of-bounds
          if(sampleZ > positionZ) gtPoints++;
          if(sampleZ < positionZ) ltPoints++;
          sampledPoints++;
        }
      }
      float gtFraction = (float) gtPoints / sampledPoints;
      float ltFraction = (float) ltPoints / sampledPoints;
      //System.out.println(gtFraction + " : " + ltFraction);
      if(ltFraction >= threshold && gtFraction < threshold) return 1;
      if(gtFraction >= threshold && ltFraction < threshold) return -1;
      else return 0;
    }
    
    
    public static float localRelativeHeight(Map3DModel model, Rectangle2D.Float view, Point2D.Float position) {
      
      //number of samples across the current view
      int effectiveResolution = 100;
      //radius around which to sample for current view
      int radius = 10;
      
      float stepSizeX = view.width / effectiveResolution;
      float stepSizeY = view.height / effectiveResolution;
      
      float positionZ = model.z(position.x, position.y);
      int sampledPoints = 0;
      float sampledDiff = 0;
      
      for(int i = -radius; i <= radius; i++){
        for(int j = -radius; j <= radius; j++){
          if(Math.sqrt(Math.pow(i,2) + Math.pow(j,2)) > radius) continue;
          float sampleZ = model.z(position.x + stepSizeX * i,
                                  position.y + stepSizeY * j);
          if(sampleZ == 0) continue; //typically out-of-bounds
          sampledDiff += (positionZ - sampleZ);
          sampledPoints++;
        }
      }
      return (float) sampledDiff / sampledPoints;
    }
    
    
    public static float localRelativeHeight(Map3DModel model, Point2D.Float position, int radius) {
      float s = (Math.max(model.getCols(), model.getRows()) - 1);
      int col = (int)Math.round(position.x * s);
      int row = (int)Math.round(position.y * s);
      float positionZ = model.z(col, row);
      int sampledPoints = 0;
      float sampledDiff = 0;
      int gtPoints = 0;
      int ltPoints = 0;
      
      for(int i=col - radius; i < col + radius; i++){
        for(int j=row - radius; j < row + radius; j++){
          if(Math.sqrt(Math.pow(col - i,2) + Math.pow(row - j,2)) > radius) continue;
          float sampleZ = model.z(i,j);
          if(sampleZ == 0) continue; //typically out-of-bounds
          if(sampleZ > positionZ) gtPoints++;
          if(sampleZ < positionZ) ltPoints++;
          sampledDiff += (positionZ - sampleZ);
          sampledPoints++;
        }
      }
      return (float) sampledDiff / sampledPoints;
    }
    
}