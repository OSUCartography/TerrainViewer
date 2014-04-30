package ch.ethz.karto.map3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * A 1D or 2D texture
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public abstract class Map3DTextureAbstract {

    /**
     * The OpenGL name of the texture. 0 means undefined.
     */
    protected int[] textureName = new int[1];

    /**
     * True if the textureName has to be released.
     */
    protected boolean deleteTextureName = false;

    /**
     * Returns an estimation of the maximum size of a texture.
     * @return The maximum dimension of a texture or -1 if an error occurs.
     */
    public static int getMaxTextureSize() {
        return Map3DGLCapabilities.getMaxTextureSize();
    }

    /**
     * Returns true if the texture is two-dimensional, i.e. the texture image
     * consists of more than one row.
     * @return true if this is a two-dimensional texture.
     */
    public abstract boolean is2D();

    /**
     * Returns true if the texture is one-dimensional, i.e. the texture image
     * consists of only a single row.
     * @return true if this is a one-dimensional texture.
     */
    public abstract boolean is1D();

    /**
     * Returns the OpenGL texture type.
     * @return GL2.GL_TEXTURE_1D or GL2.GL_TEXTURE_2D
     */
    protected int getTexType() {
        return is1D() ? GL2.GL_TEXTURE_1D : GL2.GL_TEXTURE_2D;
    }

    /**
     * Returns the dimension of the texture.
     * @return 1 or 2
     */
    public int getDim() {
        return is1D() ? 1 : 2;
    }

    /**
     * Enables or disables the use of textures for old fixed function pipeline.
     * This is not needed for shader programs.
     * If both 2D and a 1D texturing are enable, 2D texturing is used and the
     * 1D texture is ignored.
     * @param gl
     */
    protected void updateEnabledState(GL gl) {
        if (hasTexture()) {
            if (is1D()) {
                gl.glDisable(GL2.GL_TEXTURE_2D);
                gl.glEnable(GL2.GL_TEXTURE_1D);
            } else {
                gl.glDisable(GL2.GL_TEXTURE_1D);
                gl.glEnable(GL2.GL_TEXTURE_2D);
            }
        } else {
            gl.glDisable(GL2.GL_TEXTURE_1D);
            gl.glDisable(GL2.GL_TEXTURE_2D);
        }
    }

    /**
     * Passes the texture image to OpenGL if this has not been done yet.
     * @param gl
     * @return True if this texture has been passed to OpenGL, false otherwise.
     */
    public abstract boolean constructTexture(GL gl);

    /**
     * Remove the current texture. This sets a flag.
     */
    public void clearTexture() {
        this.deleteTextureName = true;
    }

    /**
     * Remove the current texture. This releases the memory on the GPU
     * if the flag deleteTextureName is set.
     * @param gl
     */
    protected void conditionalRelease(GL gl) {
        // test whether the current textureID needs to be released,
        // which is the case after a call to clearTexture().
        // glDeleteTextures silently ignores 0's and names that do not
        // correspond to existing textures
        if (this.deleteTextureName) {
            gl.glDeleteTextures(1, textureName, 0);
            this.textureName[0] = 0;
            this.deleteTextureName = false;
        }
    }

    /**
     * Returns whether a texture image has been specified using setTexture().
     * @return True if a texture image is available.
     */
    public abstract boolean hasTexture();

    /**
     * @return the textureName
     */
    public int getTextureName() {
        return textureName[0];
    }

}