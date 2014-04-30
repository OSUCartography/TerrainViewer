package ch.ethz.karto.map3d;

import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.media.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

public class Map3DFrame extends JFrame {

    private Map3DViewer viewer;

    public Map3DFrame(float[][] grid, float cellSize) {
        super("3D Map Viewer");
        
        // Use this if you use a heavy weight component with swing
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);

        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.viewer = new Map3DViewer(Map3DViewer.GLComponentType.GL_AWT, GLProfile.getDefault());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(this.viewer.getComponent(), BorderLayout.CENTER);
        
        setVisible(true);
        setBackgroundColor(0xff, 0xff, 0xff);
        enableLight(true);
               
        setModel(grid, cellSize);
    }

   
    public void setModel(float[][] grid, float cellSize) {
        this.viewer.setModel(grid, cellSize, new Map3DTexture1DMapper());
    }

    public void setTextureImage(BufferedImage textureImage) {
        this.viewer.setTextureImage(textureImage);
    }
    
    public void clearTextureImage() {
        this.viewer.clearTextureImage();
    }   

    public void setBackgroundColor(int red, int green, int blue) {
        this.viewer.setBackgroundColor(red, green, blue);
    }
    
    public void setBackgroundColor(Color bc) {
        this.setBackgroundColor(bc.getRed(), bc.getGreen(), bc.getBlue());
    }

    public void enableLight(boolean enable) {
        this.viewer.setShading(enable);
    }

    public Map3DViewer getViewer() {
        return viewer;
    }
    
    public BufferedImage getImage() {
        return this.viewer.getImage();
    }
   

}