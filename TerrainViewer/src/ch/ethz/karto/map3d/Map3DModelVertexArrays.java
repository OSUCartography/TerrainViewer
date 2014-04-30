package ch.ethz.karto.map3d;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * This vertex array model uses three large buffers to load vertices, normals,
 * and texture coordinates. The buffers contain regular grids. The grids are
 * assembled into a index buffer referencing the grids.
 * This model is not as fast as VBOs in Map3DModelVBO, which should be
 * preferred. Vertex arrays could be accelerated by compiling them into a display
 * list, but this would increase the memory occupied on the GPU, as the
 * triangle strips are converted to triangles.
 *
 * For vertex arrays, see:
 * http://www.java-tips.org/other-api-tips/jogl/vertex-buffer-objects-nehe-tutorial-jogl-port-2.html
 * @author jenny
 */
public class Map3DModelVertexArrays extends Map3DModel {

    private FloatBuffer verticesBuffer = null;
    private FloatBuffer normalsBuffer = null;
    private FloatBuffer textureBuffer = null;
    private Map3DTexture texture;

    public Map3DModelVertexArrays() {
    }

    private void copyRowToBuffer(FloatBuffer verticesBuffer,
            FloatBuffer normalsBuffer,
            FloatBuffer textureBuffer,
            int row) {

        if (this.grid == null) {
            return;
        }

        final int rows = this.grid.length;
        final int cols = this.grid[0].length;
        final int cols_1 = cols - 1;

        final float s = 1.0f / (Math.max(cols, rows) - 1);
        final float zScale = 1.0f / this.cellSize;

        final int r1 = row;
        final int r2 = Math.min(r1 + 1, rows - 1);

        float[] row1 = this.grid[r1];
        float[] row2 = this.grid[r2];

        final float texY = (float) (row - 1) / rows;
        final float widthInv = 1f / (cols - 1);

        for (int c0 = 0; c0 < cols; ++c0) {

            final int c1 = Math.min(c0 + 1, cols_1);
            final float v10 = zScale * (row1[c0] - this.minValue);
            final float v20 = zScale * (row2[c0] - this.minValue);
            final float v11 = zScale * (row1[c1] - this.minValue);

            // compute normal with the point to the right and the point below
            final float nx = v10 - v11;
            final float ny = v10 - v20;
            final float len_inv = (float) (1. / Math.sqrt(nx * nx + ny * ny + 1.0f));
            normalsBuffer.put(nx * len_inv);
            normalsBuffer.put(ny * len_inv);
            normalsBuffer.put(len_inv);

            final float x = c0 * s;
            final float y = r1 * s;
            verticesBuffer.put(x);
            verticesBuffer.put(y);
            verticesBuffer.put(v10 * s);

            if (textureBuffer != null) {
                if (texture.is1D()) {
                    final float t = texture1DMapper.get1DTextureCoordinate(c0, r1);
                    textureBuffer.put(t);
                } else {
                    textureBuffer.put(c0 * widthInv);
                    textureBuffer.put(texY);
                }
            }
        }
    }

    @Override
    public void loadModel(GL gl1, Map3DTexture texture) {
        if (modelInitialized || grid == null) {
            return;
        }

        GL2 gl = (GL2)gl1;
        this.texture = texture;
        texture.updateEnabledState(gl);

        final int rows = this.getRows();
        final int vertexCount = getCols() * rows;

        gl.glTranslatef(0, 0, ZOFFSET);

        if (verticesBuffer == null || verticesBuffer.capacity() != vertexCount * 3) {
            verticesBuffer = Buffers.newDirectFloatBuffer(vertexCount * 3);
        } else {
            verticesBuffer.rewind();
        }

        if (normalsBuffer == null || normalsBuffer.capacity() != vertexCount * 3) {
            normalsBuffer = Buffers.newDirectFloatBuffer(vertexCount * 3);
        } else {
            normalsBuffer.rewind();
        }

        if (texture.hasTexture()) {
            int texCount = texture.getDim() * vertexCount;
            if ((textureBuffer == null || textureBuffer.capacity() != texCount)) {
                textureBuffer = Buffers.newDirectFloatBuffer(texCount);
            } else {
                textureBuffer.rewind();
            }
        } else {
            textureBuffer = null;
        }

        // copy the rows
        for (int r = 0; r < rows; r++) {
            copyRowToBuffer(verticesBuffer, normalsBuffer, textureBuffer, r);
        }

        verticesBuffer.rewind();
        normalsBuffer.rewind();
        if (texture.hasTexture()) {
            textureBuffer.rewind();
        }

        this.modelInitialized = true;
    }

    @Override
    public void draw(GL gl1, boolean shading, boolean fog) {

        if (grid == null) {
            return;
        }
        
        GL2 gl = (GL2)gl1;

        final int rows = this.getRows();
        final int cols = this.getCols();

        // Enable pointers
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        if (texture.hasTexture()) {
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }

        // create index buffer for triangle strip consisting of a single row
        IntBuffer ib = Buffers.newDirectIntBuffer(cols * 2);
        for (int x = 0; x < cols; x++) {
            ib.put(x);
            ib.put(x + cols);
        }

        for (int r = 0; r < rows - 1; r++) {
            // set the vertex, normal and texture pointers to the data buffers
            gl.glVertexPointer(3, GL2.GL_FLOAT, 0, verticesBuffer.position(r * cols * 3));
            gl.glNormalPointer(GL2.GL_FLOAT, 0, normalsBuffer.position(r * cols * 3));
            if (texture.hasTexture()) {
                gl.glTexCoordPointer(texture.getDim(), GL2.GL_FLOAT, 0, 
                        textureBuffer.position(r * cols * texture.getDim()));
            }

            ib.rewind();
            gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, cols * 2, GL2.GL_UNSIGNED_INT, ib);
        }

        // Disable Pointers
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }

    @Override
    public void releaseModel(GL gl) {
    }

    @Override
    public boolean canRun() {
        return Map3DGLCapabilities.hasOpenGLVersion(1, 1);
    }
    /*
     * A variant that first computes two normals, one with the top-left neighbors and
     * one with the bottom-right neighbours and then computes the mean of these
     * two normals. This results in rounder mountain ridges with the black of
     * the backside of mountains shimmering through.
    @Override
    protected void constructSurface(GL gl, int textureID) {

    final int rows = this.grid.length;
    final int cols = this.grid[0].length;

    // Enable Pointers
    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    //gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

    float s = 1.0f / (Math.max(cols, rows) - 1);
    float zScale = 1.0f / this.cellSize;

    // normals
    float[] normal = new float[3];
    FloatBuffer normalsBuffer = Buffers.newDirectFloatBuffer(cols * rows * 3);
    for (int r = 0; r < rows; r++) {
    final int r2 = Math.min(r + 1, rows - 1);
    for (int c = 0; c < cols; ++c) {
    final int c2 = Math.min(c + 1, cols - 1);
    final float v10 = zScale * (this.grid[r][c] - this.minValue);
    final float v20 = zScale * (this.grid[r2][c] - this.minValue);
    final float v11 = zScale * (this.grid[r][c2] - this.minValue);
    computeNormal(normal, v10, v11, v20);
    normalsBuffer.put(normal);
    }
    }
    normalsBuffer.position(0);
    gl.glNormalPointer(GL2.GL_FLOAT, 0, normalsBuffer);

    // vertices
    FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(cols * rows * 3);
    for (int r = 0; r < rows; r++) {
    for (int c = 0; c < cols; c++) {
    final float x = c * s;
    final float y = r * s;
    verticesBuffer.put(x);
    verticesBuffer.put(y + s);
    verticesBuffer.put(zScale * (this.grid[r][c] - this.minValue) * s + ZOFFSET);
    }
    }
    verticesBuffer.position(0);
    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, verticesBuffer);

    for (int r = 0; r < rows - 1; r++) {
    indices.position(r * cols * 2);
    IntBuffer rowIndices = indices.slice();
    gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, cols * 2, GL2.GL_UNSIGNED_INT, rowIndices);
    }

    // Disable Pointers
    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    //gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }
     */
    /*
    @Override
    protected void constructSurface(GL gl, int textureID) {

    final int rows = this.grid.length;
    final int cols = this.grid[0].length;
    final int vertexCount = cols * 2;

    // buffers that will hold two rows, forming a triangle strip.
    FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(vertexCount * 3);
    FloatBuffer normalsBuffer = Buffers.newDirectFloatBuffer(vertexCount * 3);

    // Enable Pointers
    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);  // Enable Vertex Arrays
    gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
    //gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

    // copy the first row
    this.copyRowToBuffer(verticesBuffer, normalsBuffer, 0);

    // copy the following rows in alternating order
    for (int r = 1; r < rows; r++) {

    // jump over first vertex on odd rows
    verticesBuffer.position((r % 2) * 3);
    normalsBuffer.position((r % 2) * 3);
    this.copyRowToBuffer(verticesBuffer, normalsBuffer, r);

    verticesBuffer.position(0);
    normalsBuffer.position(0);

    // set the vertex, normal and texture pointers to our data buffers
    gl.glNormalPointer(GL2.GL_FLOAT, 0, normalsBuffer);
    gl.glVertexPointer(3, GL2.GL_FLOAT, 0, verticesBuffer);
    //gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, mesh.texCoords);

    // draw the row
    gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, vertexCount);
    }

    // Disable Pointers
    gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    //gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }*/
}
