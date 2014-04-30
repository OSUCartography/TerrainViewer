package ch.ethz.karto.map3d;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

/**
 * 2D floating point texture.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DTextureFloat {

    private int[] textureName = new int[1];

    public Map3DTextureFloat(GL gl, FloatBuffer buffer, int width, int height) {
        load(gl, buffer, width, height);
    }

    /**
     * Passes the texture buffer to OpenGL2.
     * @param gl
     */
    private void load(GL gl, FloatBuffer buffer, int width, int height) {

        if (width > Map3DTexture.getMaxTextureSize()
                || height > Map3DTexture.getMaxTextureSize()) {
            throw new GLException("float texture too large");
        }
        
        buffer.rewind();

        // generate a new texture name
        gl.glGenTextures(1, textureName, 0);

        // bind and load the texture
        gl.glBindTexture(GL2.GL_TEXTURE_2D, textureName[0]);

        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, Buffers.SIZEOF_FLOAT);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE32F, width, height, 0, GL2.GL_LUMINANCE, GL2.GL_FLOAT, buffer);
    }

    public void release(GL gl) {
        gl.glDeleteTextures(textureName.length, textureName, 0);
    }

    public int getTextureName() {
        return textureName[0];
    }
    
}
