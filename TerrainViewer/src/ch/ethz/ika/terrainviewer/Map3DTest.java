package ch.ethz.ika.terrainviewer;


import ch.ethz.karto.map3d.Map3DFrame;
import ch.ethz.karto.map3d.Map3DViewer;
import ch.ethz.karto.map3d.gui.Map3DOptionsPanel;
import ika.utils.FileUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 *
 * @author jenny
 */
public class Map3DTest {

    private static Map3DFrame frame;

    private static JMenuBar buildMenu() {

        JMenu testMenu = new javax.swing.JMenu();
        testMenu.setText("Tests");

        JMenuItem exportGridMenuItem = new javax.swing.JMenuItem();
        exportGridMenuItem.setText("Load Grid...");
        exportGridMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                
            }
        });
        testMenu.add(exportGridMenuItem);

        JMenuItem loadTextureMenuItem = new javax.swing.JMenuItem();
        loadTextureMenuItem.setText("Load Texture...");
        loadTextureMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {

                try {
                    String path = FileUtils.askFile(frame, "Load Texture", true);
                    if (path == null)
                        return;
                    BufferedImage textureImage = javax.imageio.ImageIO.read(new java.io.FileInputStream(path));
                    frame.setTextureImage(textureImage);
                } catch (IOException ex) {
                    Logger.getLogger(Map3DTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        testMenu.add(loadTextureMenuItem);
        
        JMenuItem clearTextureMenuItem = new javax.swing.JMenuItem();
        clearTextureMenuItem.setText("Clear Texture");
        clearTextureMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frame.clearTextureImage();
            }
        });
        testMenu.add(clearTextureMenuItem);
        
        JMenuItem backgroundColorMenuItem = new javax.swing.JMenuItem();
        backgroundColorMenuItem.setText("Background Color...");
        backgroundColorMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Color bcolor = JColorChooser.showDialog(frame, "Background Color", null);
                if (bcolor != null)
                    frame.setBackgroundColor(bcolor);
            }
        });
        testMenu.add(backgroundColorMenuItem);
        
        JMenuItem exportImageMenuItem = new javax.swing.JMenuItem();
        exportImageMenuItem.setText("Export 3D View...");
        exportImageMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {

                    String path = FileUtils.askFile(frame, "Save 3D View", "3d.png", false, "png");
                    if (path != null) {
                        ImageIO.write(frame.getImage(), "png", new File(path));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Map3DTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        testMenu.add(exportImageMenuItem);


        JMenuItem exportAnimationMenuItem = new javax.swing.JMenuItem();
        exportAnimationMenuItem.setText("Export Plan Oblique Animation...");/*
        exportAnimationMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    String basePath = FileUtils.askFile(frame, "Save Animation", "animation.png", false, "png");
                    if (basePath == null) {
                        return;
                    }
                    frame.getViewer().getComponent().setMinimumSize(new Dimension(1024, 768));
                    frame.getViewer().getComponent().setPreferredSize(new Dimension(1024, 768));
                    frame.pack();
                    basePath = FileUtils.cutFileExtension(basePath, 10);
                    frame.getViewer().setCamera(Map3DViewer.Camera.planOblique);

                    float[] minmax = grid.getMinMax();
                    double h = minmax[1] - minmax[0];
                    double d = Math.tan(Math.toRadians(60)) * h;
                    for (int i = 0; i < 100; i++) {
                        double di = d / 100 * i;
                        double angle = Math.atan2(di, h);
                        frame.getViewer().setShearYAngle(90 - (float)Math.toDegrees(angle));
                        frame.getViewer().getComponent().paint(frame.getViewer().getComponent().getGraphics());
                        String path = basePath + i + ".png";
                        ImageIO.write(frame.getImage(), "png", new File(path));
                        System.out.println(i);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Map3DTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });*/
        testMenu.add(exportAnimationMenuItem);


        testMenu.add(new JSeparator());
        JMenuItem optionsMenuItem = new javax.swing.JMenuItem();
        optionsMenuItem.setText("Camera & Shading...");
        optionsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Map3DOptionsPanel.show3DOptions(frame, frame.getViewer());
            }
        });
        testMenu.add(optionsMenuItem);
        
        JMenuItem shearMenuItem = new javax.swing.JMenuItem();
        shearMenuItem.setText("Shearing...");
        shearMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frame.getViewer().setCamera(Map3DViewer.Camera.orthogonal);
                ShearPanel.showPanel(frame, frame.getViewer());
            }
        });
        testMenu.add(shearMenuItem);
        
        JMenuBar menuBar = new javax.swing.JMenuBar();
        menuBar.add(testMenu);
        return menuBar;
    }

  

    public static void main(String[] args) {
        try {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Map3D Test");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {

                    float[][] grid = new float[100][100];
                    frame = new Map3DFrame(grid, 1);
                    frame.setJMenuBar(Map3DTest.buildMenu());
                    frame.validate();
                    frame.pack();
                    frame.setVisible(true);

                }
            });
        } catch (Exception ex) {
            Logger.getLogger(Map3DTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}