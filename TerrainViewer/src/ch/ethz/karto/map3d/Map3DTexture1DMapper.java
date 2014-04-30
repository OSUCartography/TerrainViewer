package ch.ethz.karto.map3d;

/**
 * Returns a one-dimensional coordinate for a 1D texture. This default
 * implementation simply maps the terrain height to a value between 0 and 1.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Map3DTexture1DMapper {

    /**
     * A reference to a grid with height values
     */
    protected float grid[][];

    /**
     * scaling factor: 1 / (max - min)
     */
    protected float heightSpanInv;

    /**
     * the minimum value of all grid values
     */
    protected float minValue;

    public Map3DTexture1DMapper() {
    }

    /**
     * Returns true if the height is linearly mapped to a color. Derived
     * may return true.
     * @return Always true for Map3DTexture1DMapper
     */
    public boolean isLinearHeightMapping() {
        return true;
    }

    /**
     * Initializes Map3DTexture1DMapper with a grid
     * @param grid The grid values.
     * @param minValue The minimum value of the grid
     * @param maxValue The maximum value of the grid
     */
    public void init(float grid[][], float minValue, float maxValue) {
        this.grid = grid;
        this.minValue = minValue;
        this.heightSpanInv = 1f / (maxValue - minValue);
        if (Float.isNaN(this.heightSpanInv)) {
            this.heightSpanInv = 0;
        }
    }

    /**
     * Returns the one-dimensional texture coordinate for a given cell.
     * @param col Horizontal coordinate of the cell.
     * @param row Vertical coordinate of the cell.
     * @return A value in [0..1]
     */
    public float get1DTextureCoordinate(int col, int row) {
        final float t = (grid[row][col] - minValue) * heightSpanInv;
        if (Float.isNaN(t)) {
            return 0;
        }
        return Math.min(1, Math.max(0, t));
    }
}
