package ch.ethz.karto.map3d;

public class Map3DNonLinearTexture1DMapper extends Map3DTexture1DMapper {

    /**
     * Don't use the grid passed in init(), but use this one.
     */
    private float[][] overwritingGrid;

    public Map3DNonLinearTexture1DMapper(float[][] grid) {
        this.overwritingGrid = grid;
    }

    @Override
    public boolean isLinearHeightMapping() {
        return false;
    }

    @Override
    public void init(float grid[][], float minValue, float maxValue) {
        this.grid = overwritingGrid;
        float minMax[] = getMinMax();
        this.minValue = minMax[0];
        this.heightSpanInv = 1 / (minMax[1] - minMax[0]);
    }

    /**
     * Returns the minimum and maximum value of the grid. This can potentially
     * be expensive as the whole grid is parsed.
     */
    private float[] getMinMax() {
        int rows = grid.length;
        int cols = grid[0].length;
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                if (grid[r][c] < min) {
                    min = grid[r][c];
                }
                if (grid[r][c] > max) {
                    max = grid[r][c];
                }
            }
        }
        return new float[]{min, max};
    }
}
