package ch.ethz.karto.map3d;

import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * A 1D or 2D texture stored in a BufferedImage.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DTexture extends Map3DTextureAbstract {

    BufferedImage imageToLoad = null;
    boolean is2D = false;
    
    public Map3DTexture() {
    }

    /**
     * Returns true if the texture is two-dimensional, i.e. the texture image
     * consists of more than one row.
     * @return true if this is a two-dimensional texture.
     */
    @Override
    public boolean is2D() {
        return is2D;
    }

    /**
     * Returns true if the texture is one-dimensional, i.e. the texture image
     * consists of only a single row.
     * @return true if this is a one-dimensional texture.
     */
    @Override
    public boolean is1D() {
        return !is2D;
    }

    @Override
    public boolean constructTexture(GL gl) {
        
        conditionalRelease(gl);
        if (imageToLoad != null) {
            load(gl);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Passes the texture image to OpenGL2.
     * @param gl
     */
    private void load(GL gl1) {
 
    	GL2 gl = (GL2)gl1;
    	
        // generate a new texture name
        gl.glGenTextures(1, textureName, 0);

        // bind and load the texture
        gl.glBindTexture(getTexType(), textureName[0]);

        gl.glTexParameteri(getTexType(), GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(getTexType(), GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(getTexType(), GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(getTexType(), GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);

        AWTTextureData texData = new AWTTextureData(gl.getGLProfile(), 0, 0, false, imageToLoad);

        int w = texData.getWidth();
        int h = texData.getHeight();
        int f = texData.getPixelFormat();
        int t = texData.getPixelType();
        Buffer b = texData.getBuffer();
        int imgFormat = texData.getInternalFormat();
        if (is2D()) {
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, imgFormat, w, h, 0, f, t, b);
        } else {
            gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, imgFormat, w, 0, f, t, b);
        }

        imageToLoad = null;

    }

    /**
     * Returns whether a texture image has been specified using setTexture().
     * @return True if a texture image is available.
     */
    @Override
    public boolean hasTexture() {
        return imageToLoad != null || textureName[0] != 0;
    }

    /**
     * Set the texture image.
     * @param textureImage The new texture image.
     */
    public void setTexture(BufferedImage textureImage) {
        if (textureImage != null) {
            this.imageToLoad = textureImage;
            this.deleteTextureName = true;
            this.is2D = textureImage.getHeight() > 1;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Texture ");
        sb.append(hasTexture() ? "initialized " : "not initialized ");
        sb.append(is2D() ? "2D" : "1D");
        return sb.toString();
    }

}