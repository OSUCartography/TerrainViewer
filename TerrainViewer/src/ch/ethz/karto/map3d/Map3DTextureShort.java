package ch.ethz.karto.map3d;

import java.nio.ShortBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * 2D short texture.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DTextureShort {

    private int[] textureName = new int[1];

    public Map3DTextureShort(GL gl, ShortBuffer buffer, int width, int height) {
        load(gl, buffer, width, height);
    }

    /**
     * Passes the texture buffer to OpenGL2.
     * @param gl
     */
    private void load(GL gl, ShortBuffer buffer, int width, int height) {

        buffer.rewind();

        // generate a new texture name
        gl.glGenTextures(1, textureName, 0);

        // bind and load the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, textureName[0]);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);

        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE, width, height,
                0, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_SHORT, buffer);
    }

    public void release(GL gl) {
        gl.glDeleteTextures(textureName.length, textureName, 0);
    }

    public int getTextureName() {
        return textureName[0];
    }
    
}
