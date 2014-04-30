package ch.ethz.karto.map3d;

import com.jogamp.common.nio.Buffers;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * http://www.java-tips.org/other-api-tips/jogl/vertex-buffer-objects-nehe-tutorial-jogl-port-2.html
 * @author jenny
 */
public class Map3DModelVBOShader extends Map3DModel {

    protected String fragmentShaderPath = "/ch/ethz/karto/map3d/vbo.frag";
    protected String vertexShaderPath = "/ch/ethz/karto/map3d/vbo.vert";

    /**
     * number of textures used in fragment shader
     */
    private static final int FRAG_SHADER_TEXTURE_COUNT = 3;
    
    /**
     * vertex buffer names
     */
    private int vertexBuffer[] = null;
    /**
     * index buffer name
     */
    private int indexBuffer[] = null;
    /**
     * height of a patch in rows.
     */
    private int patchHeight;
    /**
     * vertex and fragment shader.
     */
    private Map3DShader shaders;
    /**
     * A VBO buffer should be this large (but will be a bit smaller in reality)
     * In mega bytes.
     */
    private static final int VBO_SIZE_MB = 5;
    /**
     * Floating point texture that holds the height values.
     */
    private Map3DTextureFloat zTexture;
    private Map3DTexture texture;

    private static final int VERTEX_ATTRIB_ID = 0;
    private static final String VERTEX_ATTRIB_NAME = "v_vertex";
    private Map3DTextureByte nonLinearHypsoTexture;
    private float shearX = 0;
    private float shearY = 0;
    private float shearBaseline = 1f;
    
    protected Map3DModelVBOShader() {
    }

    @Override
    public void setModel(float grid[][], float cellSize, Map3DTexture1DMapper t) {

        super.setModel(grid, cellSize, t);

        // compute patch height such that a single vertex buffer is about VBO_SIZE_MB
        // large. The index buffer will be about twice that large.
        int vboSize = 1024 * 1024 * VBO_SIZE_MB; // VBO target size in bytes
        patchHeight = vboSize / 2 // number of vertex coordinates per vertex
                / Buffers.SIZEOF_SHORT // number of bytes per vertex coordinate
                / getCols(); // number of vertices per row

        if (patchHeight < 2) {
            throw new IllegalArgumentException("Grid has too many columns");
        }

        if (patchHeight > Map3DTexture.getMaxTextureSize()) {
            patchHeight = Map3DTexture.getMaxTextureSize();
        }

        if (patchHeight > getRows()) {
            patchHeight = getRows();
        }
    }

    @Override
    public void loadModel(GL gl1, Map3DTexture texture) {

        GL2 gl = (GL2)gl1;

        if (modelInitialized || grid == null || !canDisplay(gl, grid)) {
            return;
        }

        this.texture = texture;

        // first release old buffers to free memory on the GPU
        releaseModel(gl);

        gl.glTranslatef(0, 0, ZOFFSET);

        // load height texture
        zTexture = new Map3DTextureFloat(gl, gridTexture(), getCols(), getRows());

        if (useNonHeightProportional1DTexture()) {
            ByteBuffer buf = nonLinearHypsoTextureBuffer();
            nonLinearHypsoTexture = new Map3DTextureByte(gl, buf, getCols(), getRows());
        } else {
            nonLinearHypsoTexture = null;
        }

        // load buffers and shader programs
        loadVertexBuffer(gl);
        loadIndexBuffer(gl);
        loadShaderProgram(gl);

        this.modelInitialized = true;
    }

    @Override
    public void releaseModel(GL gl) {

        if (vertexBuffer != null) {
            gl.glDeleteBuffers(vertexBuffer.length, vertexBuffer, 0);
            vertexBuffer = null;
        }

        if (indexBuffer != null) {
            gl.glDeleteBuffers(indexBuffer.length, indexBuffer, 0);
            indexBuffer = null;
        }

        releaseShaderProgram(gl);

        if (zTexture != null) {
            zTexture.release(gl);
            zTexture = null;
        }

        if (nonLinearHypsoTexture != null) {
            nonLinearHypsoTexture.release(gl);
            nonLinearHypsoTexture = null;
        }

        this.modelInitialized = false;
    }

    protected void releaseShaderProgram(GL gl) {
        if (shaders != null) {
            shaders.disableShaders(gl);
            shaders = null;
        }
    }

    @Override
    public boolean canRun() {

        // number of textures accessible in vertex shader
        int vTex = Map3DGLCapabilities.getMaxVertexTextureImageUnits();

        // number of textures accessible in fragment shader
        int fTex = Map3DGLCapabilities.getMaxTextureImageUnits();

        // OpenGL 2.0 is required for shader programs and glVertexAttribPointer
        return Map3DGLCapabilities.hasOpenGLVersion(2, 0)
                // GL_ARB_texture_float extension is required for float textures
                && Map3DGLCapabilities.hasFloatingPointTextures()
                // height values are read from texture in vertex shader
                && vTex >= 1
                // three textures (1x1D and 2x2D) are declared in fragment shader
                && fTex >= FRAG_SHADER_TEXTURE_COUNT;
    }

    @Override
    public boolean canDisplay(GL gl, float[][] grid) {
        if (grid == null || grid.length < 2 || grid[0].length < 2) {
            return false;
        }

        // width of grid must be smaller than max texture size, as heights are
        // stored in a texture.
        if (grid[0].length > Map3DTexture.getMaxTextureSize()) {
            return false;
        }
        return true;
    }

    @Override
    public void draw(GL gl1, boolean shading, boolean fog) {
            
        if (grid == null) {
            return;
        }
        
        GL2 gl = (GL2)gl1;

        try {
            // set the vertex coordinates to use
            gl.glEnableVertexAttribArray(VERTEX_ATTRIB_ID);

            // set active texture unit to unit 0, bind it, and make it active
            // this is the texture with height values
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, zTexture.getTextureName());

            // enable optional texture on texture unit 1
            int textureType = 0; // 0: no texture; 1: 1D texture; 2: 2D texture
            if (texture.hasTexture()) {
                gl.glActiveTexture(GL2.GL_TEXTURE1);
                gl.glBindTexture(texture.getTexType(), texture.getTextureName());
                textureType = texture.is1D() ? 1 : 2;
            }
            
            // enable optional nonLinearHypsoTexture on unit 2
            if (useNonHeightProportional1DTexture()) {
                gl.glActiveTexture(GL2.GL_TEXTURE2);
                gl.glBindTexture(GL2.GL_TEXTURE_2D, nonLinearHypsoTexture.getTextureName());
                textureType = -1;
            }
            shaders.setUniform(gl, "textureType", textureType);

            // enable optional fog
            shaders.setUniform(gl, "applyFog", fog);
            
            shaders.setUniform(gl, "applyShading", shading);
            
            // shearing
            shaders.setUniform(gl, "shearXY", this.shearX, this.shearY);
            shaders.setUniform(gl, "shearBaseline", this.shearBaseline);

            // the index and vertex buffers only need to be bound once
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBuffer[0]);

            // use buffer with xy-vertex positions
            gl.glVertexAttribPointer(0, 2, GL2.GL_UNSIGNED_SHORT, false, 0, 0);

            int n = nPatches();
            shaders.setUniform(gl, "rowOffset", 0f);
            // better use glTranslatef to shift vertices with fixed function vertex buffer?
            // FIXME
            assert (shaders.validateProgram(gl));
            for (int i = 0; i < n - 1; i++) {
                gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, indexBufferSize(), GL2.GL_UNSIGNED_INT, 0);
                shaders.setUniform(gl, "rowOffset", (float) (i + 1) * (patchHeight - 1));
            }

            // last patch is possibly smaller, only draw part of it
            gl.glDrawElements(GL2.GL_TRIANGLE_STRIP, lastIndexBufferSize(), GL2.GL_UNSIGNED_INT, 0);

        } finally {
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glDisableVertexAttribArray(0);
        }

    }

    /**
     * Call textureChanged() after the texture for this terrain model has changed.
     */
    @Override
    public void textureChanged() {
        this.modelInitialized = false; // FIXME
        // should be changed such that the geometry is not reloaded when the texture changes.
    }

    private boolean useNonHeightProportional1DTexture() {
        boolean use1DTextureBuffer = texture != null
                && texture1DMapper != null
                && texture.hasTexture()
                && texture.is1D()
                && !texture1DMapper.isLinearHeightMapping();

        return use1DTextureBuffer;
    }

    private void loadVertexBuffer(GL gl) {

        if (getCols() > Short.MAX_VALUE || patchHeight > Short.MAX_VALUE) {
            throw new IllegalStateException("grid too large for rendering");
        }

        // create new vertex and normal buffers
        final int vertexCount = getCols() * patchHeight;
        ShortBuffer vBuf = Buffers.newDirectShortBuffer(vertexCount * 2);

        // create buffer name
        vertexBuffer = new int[1];
        gl.glGenBuffers(1, vertexBuffer, 0);

        // fill the buffers
        final int cols = this.grid[0].length;
        for (short r = 0; r < patchHeight; r++) {
            for (short c = 0; c < cols; ++c) {
                vBuf.put(c);
                vBuf.put(r);
            }
        }
        vBuf.rewind();

        int nbytes = patchHeight * getCols() * 2 * Buffers.SIZEOF_SHORT;
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBuffer[0]);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, nbytes, vBuf, GL2.GL_STATIC_DRAW);

    }

    /**
     * allocates the index buffer and loads it to the GPU.
     * An integer index buffer must be used for large grids - and not shorts -
     * otherwise rendering becomes extremely slow, because shorts are not
     * aligned to 4 bytes boundaries.
     * The index buffer contains two indices per grid value for triangle strips
     * (except for the last row) and one index for each row (except for
     * the topmost and the bottomost row).
     * @param gl
     */
    private void loadIndexBuffer(GL gl) {

        int cols = getCols();

        // create index buffer
        IntBuffer indexBuf = Buffers.newDirectIntBuffer(indexBufferSize());
        for (int y = 0; y < patchHeight - 1; y += 2) {

            // even row from left to right
            for (int x = 0; x < cols; x++) {
                final int i = x + y * cols;
                indexBuf.put(i);
                indexBuf.put(i + cols);
            }

            // stop if this was the last row
            if (y + 1 >= patchHeight - 1) {
                break;
            }

            // add a degenerate triangle
            // http://www.gamedev.net/community/forums/topic.asp?topic_id=227553&whichpage=1&#1477073
            indexBuf.put((cols - 1) + y * cols + cols);

            // odd rows from right to left
            for (int x = cols - 1; x >= 0; x--) {
                final int i = x + (y + 1) * cols;
                indexBuf.put(i);
                indexBuf.put(i + cols);
            }

            // add a degenerate triangle if this is not the last row
            if (y + 2 < patchHeight - 1) {
                indexBuf.put((y + 1) * cols + cols);
            }
        }

        // create name for index buffer and load it to the GPU
        indexBuffer = new int[1];
        gl.glGenBuffers(1, indexBuffer, 0);
        indexBuf.rewind();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
        int bufBytes = indexBuf.capacity() * Buffers.SIZEOF_INT;
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, bufBytes, indexBuf, GL2.GL_STATIC_DRAW);
    }

    protected void loadShaderProgram(GL gl) {
        if (grid == null) {
            return;
        }
        
        shaders = new Map3DShader();
        shaders.enableShaders(gl, vertexShaderPath, fragmentShaderPath,
                new String[]{VERTEX_ATTRIB_NAME}, new int[]{VERTEX_ATTRIB_ID});

        int cols = getCols();
        int rows = getRows();
        float hTextureScale = cols >= rows ? 1 : (float) rows / cols;
        float vTextureScale = rows >= cols ? 1 : (float) cols / rows;
        shaders.setUniform(gl, "textureScale", hTextureScale, vTextureScale);

        // scale factor for converting between columns/rows to unity box
        float scaleGridToUnity = 1.0f / (Math.max(cols, rows) - 1);
        shaders.setUniform(gl, "scaleGridToUnity", scaleGridToUnity);
        float scaleZToUnity = 1f / ((maxValue - minValue) / cellSize);
        shaders.setUniform(gl, "scaleZToUnity", scaleZToUnity);

        // texture unit 0 is for height values
        shaders.setUniform(gl, "zTexture", 0);
        
        // texture unit 1 is the optional 2D image draped onto the terrain or
        // the optional 1D hypsometric color range
        // The third texture uniform must also be set (as uniforms are declared
        // in the shaders) but is not used.
        if (texture.is2D) {
            shaders.setUniform(gl, "imageTexture", 1);
            shaders.setUniform(gl, "hypsoLookUpTexture", 2);
            shaders.setUniform(gl, "hypsoTexture", 3); // not used
        } else {
            shaders.setUniform(gl, "hypsoTexture", 1);
            shaders.setUniform(gl, "hypsoLookUpTexture", 2);
            shaders.setUniform(gl, "imageTexture", 3); // not used
        }
        
        // shearing
        shaders.setUniform(gl, "shearXY", this.shearX, this.shearY);
        shaders.setUniform(gl, "shearBaseline", this.shearBaseline);
    }

    /**
     * Returns a buffer with all grid values, scaled to columns/rows grid units.
     * The minimum value of the grid is subtracted from values in the returned 
     * grid, i.e. the smallest value is 0.
     * @return
     */
    private FloatBuffer gridTexture() {
        int cols = getCols();
        int rows = getRows();
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // FIXME no shifting to avoid inconsistent results with plan oblique rendering
                buffer.put((grid[r][c] /*- minValue*/) / cellSize);
            }
        }
        buffer.rewind();
        return buffer;
    }
    
    private ByteBuffer nonLinearHypsoTextureBuffer() {
        int cols = getCols();
        int rows = getRows();
        ByteBuffer buffer = Buffers.newDirectByteBuffer(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = (int)(texture1DMapper.get1DTextureCoordinate(c, r) * 255);
                buffer.put((byte)(i));
            }
        }
        buffer.rewind();
        return buffer;
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
    
    public void setShearing(float shearX, float shearY) {
        this.shearX = shearX;
        this.shearY = shearY;
    }
    
    public void setShearBaseline(float z){
      this.shearBaseline = z;
    }
}
