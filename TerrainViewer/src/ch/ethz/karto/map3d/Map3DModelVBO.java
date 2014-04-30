package ch.ethz.karto.map3d;

import com.jogamp.common.nio.Buffers;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * http://www.java-tips.org/other-api-tips/jogl/vertex-buffer-objects-nehe-tutorial-jogl-port-2.html
 * Use ARB VBO functions to support drivers with OpenGL older than 1.5 with the
 * VBO extension. Old Intel hardware claims to support VBO, but only supports the ARB
 * variants ( http://www.gamedev.net/community/forums/topic.asp?topic_id=501651 ).
 * @author jenny
 */
public class Map3DModelVBO extends Map3DModel {

    /**
     * vertex buffer names
     */
    private int vertexBufs[] = null;
    /**
     * normal buffer names
     */
    private int normalBufs[] = null;
    /**
     * texture buffer names for non-linear one-dimensional height texture
     */
    private int textureBufs[] = null;

    /**
     * index buffer name
     */
    private int indexBuffer[] = null;
    /**
     * height of a patch in rows.
     */
    private int patchHeight;
    private Map3DTexture texture;

    protected Map3DModelVBO() {
    }

    @Override
    public void setModel(float grid[][], float cellSize, Map3DTexture1DMapper t) {
        super.setModel(grid, cellSize, t);

        // compute patch height such that a single vertex buffer is about 5MB
        // large. The index buffer will be about twice that large.
        int vboSize = 1024 * 1024 * 5; // VBO target size is 5MB
        patchHeight = vboSize / 3 // 3 values in vertex buffer
                / 4 // bytes per vertex
                / getCols();

        if (patchHeight < 2) {
            throw new IllegalArgumentException("Grid has too many columns");
        }
        if (patchHeight > getRows()) {
            patchHeight = getRows();
        }
    }

    /**
     * Copies one row of the grid to the vertex, normal and texture buffer.
     * @param verticesBuffer The buffer for the vertices.
     * @param normalsBuffer The buffer for the normals.
     * @param textureBuffer The buffer for texture coordinates. Can be null.
     * @param row The row to copy.
     */
    private void copyRowToBuffer(FloatBuffer verticesBuffer,
            FloatBuffer normalsBuffer, FloatBuffer textureBuffer,
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

        boolean use1DTextureBuffer = texture.hasTexture()
                && texture.is1D()
                && !texture1DMapper.isLinearHeightMapping()
                && textureBuffer != null;

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

            if (use1DTextureBuffer) {
                final float t = texture1DMapper.get1DTextureCoordinate(c0, r1);
                textureBuffer.put(t);
            }
        }
    }

    private boolean use1DTextureBuffer() {
        return  texture.hasTexture()
                && texture.is1D()
                && !texture1DMapper.isLinearHeightMapping();
    }

    /**
     * Loads the vertex, normals and texture buffers to the GPU.
     * @param gl
     * @param vBuf vertex buffer
     * @param nBuf normals buffer
     * @param tBuf one-dimensional textureBuffer
     * @param nBytes
     * @param bufID
     */
    private void loadToGPU(GL gl, Buffer vBuf, Buffer nBuf, Buffer tBuf,
            int nVertices, int bufID) {

        int nbytes = nVertices * 3 * 4;
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufs[bufID]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, nbytes, vBuf, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, normalBufs[bufID]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, nbytes, nBuf, GL2.GL_STATIC_DRAW);
        if (use1DTextureBuffer()) {
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, textureBufs[bufID]);
            nbytes = nVertices * 4;
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, nbytes, tBuf, GL2.GL_STATIC_DRAW);
        }

    }

    /**
     * Returns the number of patches, including the last one, which may by
     * smaller than the others.
     * @return The number of patches.
     */
    private int nPatches() {
        int n = (getRows() - 1) / (patchHeight - 1);
        return n + ((getRows() - 1) % (patchHeight - 1) > 0 ? 1 : 0);
    }

    /**
     * Returns the number of entire patches. This number includes the last patch
     * if it is as large as the others.
     * @return The number of entire patches.
     */
    private int nUnbrokenPatches() {
        return (getRows() - 1) / (patchHeight - 1);
    }

    /**
     * Returns whether the last patch is smaller than a normal patch.
     * @return True if the last patch has less rows than a normal patch.
     */
    private boolean hasBrokenPatch() {
        return nPatches() > nUnbrokenPatches();
    }

    /**
     * Returns the height of the last patch, which can be equal to patchHeight
     * or less.
     * @return The number of rows of the last patch.
     */
    private int lastPatchHeight() {
        if (hasBrokenPatch()) {
            return getRows() - nUnbrokenPatches() * (patchHeight - 1);
        } else {
            return patchHeight;
        }
    }

    /**
     * Returns the number of indices used to construct a triangle strip from the
     * regular grids of vertices, normals and texture coordinates.
     * @return The  number of indices in the indexBuf.
     */
    private int indexBufferSize() {
        return getCols() * 2 * (patchHeight - 1) + patchHeight - 2;
    }

    /**
     * Returns the number of indices used to construct a triangle strip for the
     * last patch from the regular grids of vertices, normals and texture coordinates.
     * @return The  number of indices for the last patch.
     */
    private int lastIndexBufferSize() {
        int h = lastPatchHeight();
        return getCols() * 2 * (h - 1) + h - 2;
    }

    @Override
    public void loadModel(GL gl1, Map3DTexture texture) {

        if (modelInitialized || grid == null) {
            return;
        }

        GL2 gl = (GL2)gl1;

        this.texture = texture;
        texture.updateEnabledState(gl);

        gl.glTranslatef(0, 0, ZOFFSET);

        // create new vertex and normal buffers
        final int vertexCount = getCols() * patchHeight;
        FloatBuffer vBuf = Buffers.newDirectFloatBuffer(vertexCount * 3);
        FloatBuffer nBuf = Buffers.newDirectFloatBuffer(vertexCount * 3);
        FloatBuffer tBuf = null;
        if (use1DTextureBuffer()) {
            tBuf = Buffers.newDirectFloatBuffer(vertexCount);
        }

        // release old buffers to free space on GPU
        releaseModel(gl);

        // create buffer names
        final int nPatches = nPatches();
        final int nUnbrokenPatches = nUnbrokenPatches();
        vertexBufs = new int[nPatches];
        normalBufs = new int[nPatches];
        gl.glGenBuffers(nPatches, vertexBufs, 0);
        gl.glGenBuffers(nPatches, normalBufs, 0);

        if (use1DTextureBuffer()) {
            textureBufs = new int[nPatches];
            gl.glGenBuffers(nPatches, textureBufs, 0);
        } else {
            textureBufs = null;
        }


        // load entire patches to the GPU
        for (int i = 0; i < nUnbrokenPatches; i++) {
            // fill the vertex, normal, and texture buffers
            int lastRow = (patchHeight - 1) * (i + 1);
            int firstRow = (patchHeight - 1) * i;
            loadBuffer(gl, vBuf, nBuf, tBuf, firstRow, lastRow, i);
        }

        // load last partial patch to the GPU
        if (hasBrokenPatch()) {
            int firstRow = (patchHeight - 1) * nUnbrokenPatches;
            final int bufID = normalBufs.length - 1;
            loadBuffer(gl, vBuf, nBuf, tBuf, firstRow, getRows() - 1, bufID);
        }

        buildIndexBuffer(gl);

        this.modelInitialized = true;
    }

    private void loadBuffer(GL gl,
            FloatBuffer vBuf, FloatBuffer nBuf, FloatBuffer tBuf,
            int firstRow, int lastRow, int bufID) {

        vBuf.rewind();
        nBuf.rewind();
        if (tBuf != null) {
            tBuf.rewind();
        }

        // fill the vertex, normal, and texture buffers
        for (int r = firstRow; r <= lastRow; r++) {
            copyRowToBuffer(vBuf, nBuf, tBuf, r);
        }

        vBuf.rewind();
        nBuf.rewind();
        if (tBuf != null) {
            tBuf.rewind();
        }
        int nVertices = (lastRow - firstRow + 1) * getCols();
        loadToGPU(gl, vBuf, nBuf, tBuf, nVertices, bufID);
    }

    @Override
    public void releaseModel(GL gl1) {
        // release data buffered on the GPU
        GL2 gl = (GL2)gl1;
        if (vertexBufs != null) {
            gl.glDeleteBuffers(vertexBufs.length, vertexBufs, 0);
        }
        if (normalBufs != null) {
            gl.glDeleteBuffers(normalBufs.length, normalBufs, 0);
        }
        if (indexBuffer != null) {
            gl.glDeleteBuffers(1, indexBuffer, 0);
        }
         if (textureBufs != null) {
            gl.glDeleteBuffers(textureBufs.length, textureBufs, 0);
        }
    }

    @Override
    public boolean canRun() {
        // VBO are available with OpenGL 1.5 or earlier with an extension
        // glTexGen is deprecatd with OpenGL 3.0 and not available in 3.1
        // use the ARB calls, non-ARB calls are not defined prior to 1.5 and
        // are not supported on Intel hardware, such as Intel 965
        return Map3DGLCapabilities.hasVBO()
                && !Map3DGLCapabilities.hasOpenGLVersion(3, 1);
    }

    /**
     * Draws the buffer identified by index i. First binds, then draws the buffers.
     * @param gl
     * @param i The index of the buffer to draw.
     * @param indexBufSize The size of the index buffer
     */
    private void drawBuffer(GL gl1, int i, int indexBufSize) {
        GL2 gl = (GL2)gl1;
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufs[i]);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, normalBufs[i]);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);

        if (use1DTextureBuffer()) {
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, textureBufs[i]);
            gl.glTexCoordPointer(1, GL2.GL_FLOAT, 0, 0);
        }

        gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, indexBufSize, GL2.GL_UNSIGNED_INT, 0);
    }

    @Override
    public void draw(GL gl1, boolean shading, boolean fog) {

        if (grid == null) {
            return;
        }

        GL2 gl = (GL2)gl1;
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        if (use1DTextureBuffer()) {
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }

        try {
            if (texture.hasTexture() && !use1DTextureBuffer()) {
                autoGenerateTextureCoordinates(gl);
            }

            // the index buffer only needs to be bound once
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);

            for (int i = 0; i < vertexBufs.length - 1; i++) {
                drawBuffer(gl, i, indexBufferSize());
            }

            // last patch is possibly smaller, only draw part of it
            drawBuffer(gl, vertexBufs.length - 1, lastIndexBufferSize());

        } finally {
            // do this in case the next time drawing is not done with VBOs
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
            gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

            // do this in case the next time none or another type of texture is used.
            gl.glDisable(GL2.GL_TEXTURE_GEN_S);
            gl.glDisable(GL2.GL_TEXTURE_GEN_T);
        }
    }

    /**
     * 2D textures are automatically derived from the vertex coordinates by
     * glTexGen. Vertex coordinates need to be scaled.
     * @param gl
     */
    private void autoGenerateTextureCoordinates(GL gl1) {
        int cols = getCols();
        int rows = getRows();
        GL2 gl = (GL2)gl1;
        if (texture.getDim() == 1) {
            // map z value of vertices to s coordinate for 1D texture
            gl.glEnable(GL2.GL_TEXTURE_GEN_S);
            float zScale = 1.f / ((maxValue - minValue) / cellSize / (Math.max(cols, rows) - 1));
            gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
            gl.glTexGenfv(GL2.GL_S, GL2.GL_OBJECT_PLANE, new float[]{0, 0, zScale, 0}, 0);
        } else {
            // map x and y of vertices to s and t coordinates of 2D texture
            gl.glEnable(GL2.GL_TEXTURE_GEN_S);
            gl.glEnable(GL2.GL_TEXTURE_GEN_T);
            float xScale = cols >= rows ? 1 : (float) rows / cols;
            float yScale = rows >= cols ? 1 : (float) cols / rows;
            gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
            gl.glTexGenfv(GL2.GL_S, GL2.GL_OBJECT_PLANE, new float[]{xScale, 0, 0, 0}, 0);
            gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
            gl.glTexGenfv(GL2.GL_T, GL2.GL_OBJECT_PLANE, new float[]{0, yScale, 0, 0}, 0);
        }
    }
    
    /**
     * Allocates the index buffer and loads it to the GPU.
     * An integer index buffer (and not shorts) must be used for large grids,
     * otherwise rendering becomes extremely slow. The individual VBOs
     * are too small for efficient rendering with shorts.
     * The index buffer contains two indices per grid value for triangle strips
     * (excep for the last row) and one index for each row (except for
     * the topmost and the bottomost row).
     * @param gl
     */
    private void buildIndexBuffer(GL gl1) {

        int cols = getCols();
        GL2 gl = (GL2)gl1;

        // create index buffer
        IntBuffer indexBuf = Buffers.newDirectIntBuffer(indexBufferSize());
        for (int y = 0; y < patchHeight - 1; y += 2) {

            // even row from left to right
            for (int x = 0; x < cols; x++) {
                final int i = x + y * cols;
                indexBuf.put(i);
                indexBuf.put((i + cols));
            }

            // stop if this was the last row
            if (y + 1 >= patchHeight - 1) {
                break;
            }

            // add a degenerate triangle
            // http://www.gamedev.net/community/forums/topic.asp?topic_id=227553&whichpage=1&#1477073
            indexBuf.put(((cols - 1) + y * cols + cols));

            // odd rows from right to left
            for (int x = cols - 1; x >= 0; x--) {
                final int i = x + (y + 1) * cols;
                indexBuf.put(i);
                indexBuf.put((i + cols));
            }

            // add a degenerate triangle if this is not the last row
            // http://www.gamedev.net/community/forums/topic.asp?topic_id=227553&whichpage=1&#1477073
            if (y + 2 < patchHeight - 1) {
                indexBuf.put(((y + 1) * cols + cols));
            }
        }

        // create name for index buffer and load it to the GPU
        indexBuf.rewind();
        indexBuffer = new int[1];
        gl.glGenBuffers(1, indexBuffer, 0);
        indexBuf.rewind();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
        int bufBytes = indexBuf.capacity() * 4;
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, bufBytes, indexBuf, GL2.GL_STATIC_DRAW);
    }

    @Override
    public boolean canDisplay(GL gl, float[][] grid) {
        return grid != null && grid.length >= 2 && grid[0].length >= 2;
    }
}
