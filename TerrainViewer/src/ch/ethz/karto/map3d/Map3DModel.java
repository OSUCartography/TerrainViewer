package ch.ethz.karto.map3d;

import javax.media.opengl.*;

public abstract class Map3DModel {

    protected static final float ZOFFSET = 0;//0.03f;
    protected float cellSize = 1.0f;
    protected float grid[][];
    protected float minValue = Float.MAX_VALUE;
    protected float maxValue = Float.MIN_VALUE;
    protected boolean modelInitialized = false;

    protected Map3DTexture1DMapper texture1DMapper;

    public Map3DModel() {
    }

    /**
     * Constructs the terrain model.
     * @param gl
     */
    public abstract void loadModel(GL gl, Map3DTexture texture);

    /**
     * Draws the terrain model
     * @param gl
     */
    public abstract void draw(GL gl, boolean shading, boolean fog);

    /**
     * Releases any resources allocated. The model will not be used anymore
     * after <code>release</code> is called.
     * @param gl
     */
    public abstract void releaseModel(GL gl);

    /**
     * Returns whether the required hardware and software are present to run.
     * @return
     */
    public abstract boolean canRun();

    public boolean canDisplay(GL gl, float[][] grid) {
        return grid != null && grid.length >= 2 && grid[0].length >= 2;
    }
    
    public boolean isInitialized() {
        return this.grid != null;
    }

    public void setModel(float grid[][], float cellSize, Map3DTexture1DMapper texture1DMapper) {
        this.grid = grid;
        this.cellSize = cellSize;
        computeMinMax();
        this.modelInitialized = false;
        this.texture1DMapper = texture1DMapper;
        this.texture1DMapper.init(grid, minValue, maxValue);
    }

    public int getCols() {
        return grid[0].length;
    }

    public int getRows() {
        return grid.length;
    }

    public float getNormalizedModelWidth() {
        if (grid == null) {
            return Float.NaN;
        }
        return Math.min(1, (float) this.getCols() / this.getRows());
    }

    public float getNormalizedModelHeight() {
        if (grid == null) {
            return Float.NaN;
        }
        return Math.min(1, (float) this.getRows() / this.getCols());
    }
    
    public float getNormalizedMinimumValue() {
        if (grid == null) {
            return Float.NaN;
        }
        return minValue / Math.max(this.getRows(), this.getCols());
    }

    private void computeMinMax() {

        final int rows = this.getRows();
        final int cols = this.getCols();

        this.minValue = Float.MAX_VALUE;
        this.maxValue = Float.MIN_VALUE;

        for (int r = 0; r < rows; ++r) {
            final float[] row = this.grid[r];
            for (int c = 0; c < cols; ++c) {
                final float value = row[c];
                if (!Float.isNaN(value)) {
                    this.minValue = Math.min(this.minValue, value);
                    this.maxValue = Math.max(this.maxValue, value);
                }
            }
        }
    }

    public float getCenterElevation() {
        if (this.grid == null) {
            return 0;
        }
        return (0.3f * this.maxValue) / ((this.getCols() - 1) * this.cellSize);
    }

    public float getMinElevation() {
        if (this.grid == null) {
            return 0;
        }
        else return this.minValue;
    }
     
     
    public float getMaxElevation() {
        if (this.grid == null) {
            return 0;
        }
        else return this.maxValue;
    }
    
        /**
     * Return the elevation at the point closest to the location provided.
     * @returns The elevation at the closest point (if one exists), otherwise -1.
     */
    public float getNearestNeighborElevation(double x, double y){
        final int rows = this.getRows();
        final int cols = this.getCols();
        int row = (int) Math.round(y * rows);
        int col = (int) Math.round(x * cols);
        if(row >= 0 && row < rows && col >= 0 && col < cols)
            return this.grid[row][col];
        else return -1;
    } 
    
    protected static void computeNormal(float[] normal, float z00, float z01, float z10) {
        final float dx = z00 - z01;
        final float dy = z00 - z10;
        final float len_inv = 1.f / (float) Math.sqrt(dx * dx + dy * dy + 1.0f);
        normal[0] = dx * len_inv;
        normal[1] = dy * len_inv;
        normal[2] = len_inv;
    }

    /**
     * Call textureChanged() after the texture for this terrain model has changed.
     */
    public void textureChanged() {
        this.modelInitialized = false;
    }
    
    /**
     * Returns a grid cell elevation
     * @param row
     * @param col
     * @return 
     */
    public float z(int row, int col) {
        final float zScale = 1.0F / cellSize;
        final float s = 1.0F / (Math.max(getCols(), getRows()) - 1);
        if(row < 0 || row >= getRows() || col < 0 || col >= getCols()) return 0;
        else return (grid[row][col]) * s * zScale;
    }

    /**
     * nearest neighbor elevation
     * @param x
     * @param y
     * @return 
     */
    public float z(double x, double y) {
        final float s = (Math.max(getCols(), getRows()) - 1);
        int col = (int)Math.round(x * s);
        int row = (int)Math.round(y * s);
        return z(row, col);
    }
}