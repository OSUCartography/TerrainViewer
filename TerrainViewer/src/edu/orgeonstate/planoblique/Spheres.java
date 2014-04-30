package edu.orgeonstate.planoblique;

import ch.ethz.karto.map3d.Map3DModel;
import java.util.ArrayList;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class Spheres {

    /**
     * A dot on a 3D map with a color. Location is relative to top-left corner, 
     * x axis to the right, y axis downwards, coordinate range between 0 and 1.
     */
    private static class Location {
        protected final double x;
        protected final double y;
        protected final float r;
        protected final float g;
        protected final float b;
        
        protected Location (double x, double y, float r, float g, float b) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
    
    /**
     * An array of locations
     */
    private static final ArrayList<Location> locs = new ArrayList<Location>();

    
    /**
     * Radius of spheres drawn
     */
    private static final float R = 0.005f;
    
    /**
     * Longitudinal geometry resolution of sphere.
     */
    private static final int LONGITUDE_SLICES = 32;
    
    /**
     * Latitudinal geometry resolution of sphere.
     */
    private static final int LATITUDE_SLICES = 16;

    public Spheres(Map3DModel grid) {
        this.grid = grid;
    }
    
    /**
     * The elevation model with z values.
     */
    private final Map3DModel grid;

    
    public void addLocation(double x, double y, float r, float g, float b){
        locs.add(new Location(x,y,r,g,b));
    }
    
    public void clearLocations(){
        locs.clear();
    }
    
    
    /**
     * Draw all locations
     * @param gl
     * @param glu
     * @param shearX
     * @param shearY 
     */
    public void draw(GL gl1, GLU glu, double shearX, double shearY) {

        //System.out.println("\ndrawing spheres");

        GL2 gl = (GL2)gl1;
        gl.glInitNames();
        gl.glPushName(0);

        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);

        // store initial drawing color
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);

        for (int i = 0; i < locs.size(); i++) {
            Location loc = locs.get(i);
            gl.glLoadName(i);
            gl.glColor4f(loc.r, loc.g, loc.b, 1.0f);
            gl.glPushMatrix();
            try {
                double x = loc.x;
                double y = loc.y;
                double z = grid.z(x, y);
                x += shearX * z;
                y += shearY * z;
                //System.out.println("ID " + i + "\t" + x + "\t" + y + "\t" + z);
                gl.glTranslated(x, y, z);
                glu.gluSphere(quadric, R, LONGITUDE_SLICES, LATITUDE_SLICES);
            } catch (Exception exc) {
                // FIXME
                System.out.println("Could not draw sphere");                
            }
            gl.glPopMatrix();
        }

        // restore initial drawing color
        gl.glPopAttrib();

        glu.gluDeleteQuadric(quadric);
    }
}
