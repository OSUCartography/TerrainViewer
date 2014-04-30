/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.ethz.karto.map3d;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author jenny
 */
public class Map3DModelDisplayList extends Map3DModel {
    /**
     * The display-list name for the base of the terrain.
     * This ID is only generated once. When it is reused with glNewList and
     * glEndList, a pre-existing list is replaced when glEndList is called.
     */
    private int baseListID = 0;
    /**
     * The display-list name for the terrain.
     * This ID is only generated once. When it is reused with glNewList and
     * glEndList, a pre-existing list is replaced when glEndList is called.
     */
    private int terrainListID = 0;

    @Override
    public void loadModel(GL gl1, Map3DTexture texture) {
        if (modelInitialized) {
            return;
        }
        GL2 gl = (GL2)gl1;
        if (this.terrainListID != 0) {
            gl.glDeleteLists(this.terrainListID, 1);
        }
        this.terrainListID = gl.glGenLists(1);
        gl.glNewList(this.terrainListID, GL2.GL_COMPILE);
        boolean hasVoidValueOnBorder = constructSurface(gl, texture);
        gl.glEndList();
        // base of relief model
        // base of relief model
        if (this.baseListID != 0) {
            gl.glDeleteLists(this.baseListID, 1);
        }
        this.baseListID = gl.glGenLists(1);
        gl.glNewList(baseListID, GL2.GL_COMPILE);
        if (!hasVoidValueOnBorder) {
            this.constructBoundingBox(gl);
        }
        gl.glEndList();
        this.modelInitialized = true;
    }

    @Override
    public void draw(GL gl1, boolean shading, boolean fog) {
        /*
        // draw the base
        gl.glDisable(GL2.GL_TEXTURE_2D);
        this.enableLight(gl, true);
        gl.glCallList(this.model.getBaseListID());

        this.enableLight(gl, this.lightEnabled);
        */
        GL2 gl = (GL2)gl1;
        gl.glCallList(this.getTerrainListID());
    }

    /**
     * Constructs the terrain surface.
     * @param gl
     * @param textureID This texture is drapped onto the terrain if it differs from 0.
     * @return Returns true if the grid contains void values along its border.
     */
    private boolean constructSurface(GL gl1, Map3DTexture texture) {
        if (this.grid == null) {
            return true;
        }
        
        GL2 gl = (GL2)gl1;
        
        final int rows = this.getRows();
        final int cols = this.getCols();
        final float s = 1.0F / (Math.max(cols, rows) - 1);
        final float zScale = 1.0F / this.cellSize;
        final float[] normal = new float[3];
        boolean hasVoidValueOnBorder = false;
        texture.updateEnabledState(gl);
        // compute surface normals and texture coordinates
        // compute surface normals and texture coordinates
        for (int rowCenter = 1; rowCenter < rows; ++rowCenter) {
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);
            int rowTop = rowCenter - 1;
            int rowBottom = Math.min(rowCenter + 1, rows - 1);
            for (int colCenter = 0; colCenter < cols; ++colCenter) {
                final int colRight = Math.min(colCenter + 1, cols - 1);
                final float v00 = zScale * (this.grid[rowTop][colCenter] - this.minValue);
                final float v10 = zScale * (this.grid[rowCenter][colCenter] - this.minValue);
                final float v20 = zScale * (this.grid[rowBottom][colCenter] - this.minValue);
                final float v01 = zScale * (this.grid[rowTop][colRight] - this.minValue);
                final float v11 = zScale * (this.grid[rowCenter][colRight] - this.minValue);
                if (Float.isNaN(v00) || Float.isNaN(v10) || Float.isNaN(v20) || Float.isNaN(v01) || Float.isNaN(v11)) {
                    hasVoidValueOnBorder = rowTop == 0 || rowTop == rows - 1 || colCenter == 0 || colCenter == cols - 1;
                    continue;
                }
                final float x = colCenter * s;
                final float y = rowTop * s;
                if (texture.hasTexture()) {
                    if (texture.is1D()) {
                        gl.glTexCoord1f(x);
                    } else {
                        gl.glTexCoord2f(x, y);
                    }
                } /*
                final float dx = v01 - v11;
                final float dy = v10 - v20;
                final float dz = 2f * s;
                final float linv = (float)(1./Math.sqrt(dx*dx+dy*dy+dz*dz));
                normal[0] = dx * linv;
                normal[1] = dy * linv;
                normal[2] = dz * linv;*/
                /*
                final float dx = v01 - v11;
                final float dy = v10 - v20;
                final float dz = 2f * s;
                final float linv = (float)(1./Math.sqrt(dx*dx+dy*dy+dz*dz));
                normal[0] = dx * linv;
                normal[1] = dy * linv;
                normal[2] = dz * linv;*/
                computeNormal(normal, v00, v01, v10);
                gl.glNormal3fv(normal, 0);
                gl.glVertex3f(x, y, v00 * s + ZOFFSET);
                if (texture.hasTexture()) {
                    if (texture.is1D()) {
                        gl.glTexCoord1f(x);
                    } else {
                        gl.glTexCoord2f(x, y);
                    }
                }
                computeNormal(normal, v10, v11, v20);
                gl.glNormal3fv(normal, 0);
                gl.glVertex3f(x, y + s, v10 * s + ZOFFSET);
            }
            gl.glEnd();
        }
        return hasVoidValueOnBorder;
    }

    /**
     * Constructs the base below the terrain.
     * @param gl
     */
    private void constructBoundingBox(GL gl1) {
        if (this.grid == null) {
            return;
        }
        GL2 gl = (GL2)gl1;

        float x;
        float y;
        int rows = this.getRows();
        int cols = this.getCols();
        float s = 1.0F / (Math.max(cols, rows) - 1);
        float zScale = s / this.cellSize;
        float boxWidth = Math.min(1, (float) cols / rows);
        float boxHeight = Math.min(1, (float) rows / cols);
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        // left plane
        // left plane
        gl.glNormal3f(-1.0F, 0.0F, 0.0F);
        x = 0.0F;
        for (int r = 0; r < rows; ++r) {
            y = r * s;
            float z = this.grid[r][0];
            //gl.glTexCoord2f(y, 0.f);
            //gl.glTexCoord2f(y, 0.f);
            gl.glVertex3f(x, y, 0.0F);
            //gl.glTexCoord2f(y, z * tScale);
            //gl.glTexCoord2f(y, z * tScale);
            gl.glVertex3f(x, y, (z - this.minValue) * zScale + ZOFFSET);
        }
        // front plane
        // front plane
        gl.glNormal3f(0.0F, 1.0F, 0.0F);
        y = boxHeight;
        for (int c = 0; c < cols; ++c) {
            x = c * s;
            float z = this.grid[rows - 1][c];
            //gl.glTexCoord2f(x, 0.f);
            //gl.glTexCoord2f(x, 0.f);
            gl.glVertex3f(x, y, 0.0F);
            //gl.glTexCoord2f(x, z * tScale);
            //gl.glTexCoord2f(x, z * tScale);
            gl.glVertex3f(x, y, (z - this.minValue) * zScale + ZOFFSET);
        }
        // right plane
        // right plane
        gl.glNormal3f(1.0F, 0.0F, 0.0F);
        x = boxWidth;
        for (int r = rows - 1; r >= 0; --r) {
            y = r * s;
            float z = this.grid[r][cols - 1];
            //gl.glTexCoord2f(1.0f - y, 0.f);
            //gl.glTexCoord2f(1.0f - y, 0.f);
            gl.glVertex3f(x, y, 0.0F);
            //gl.glTexCoord2f(1.0f - y, z * tScale);
            //gl.glTexCoord2f(1.0f - y, z * tScale);
            gl.glVertex3f(x, y, (z - this.minValue) * zScale + ZOFFSET);
        }
        // back plane
        // back plane
        gl.glNormal3f(0.0F, -1.0F, 0.0F);
        y = 0.0F;
        for (int c = cols - 1; c >= 0; --c) {
            x = c * s;
            float z = this.grid[0][c];
            //gl.glTexCoord2f(1.0f - x, 0.f);
            //gl.glTexCoord2f(1.0f - x, 0.f);
            gl.glVertex3f(x, y, 0.0F);
            //gl.glTexCoord2f(1.0f - x, z * tScale);
            //gl.glTexCoord2f(1.0f - x, z * tScale);
            gl.glVertex3f(x, y, (z - this.minValue) * zScale + ZOFFSET);
        }
        gl.glVertex3f(0.0F, 0.0F, 0.0F);
        gl.glEnd();
    }

    /**
     * Returns the OpenGL list name of the base of the terrain.
     * @return
     */
    public int getBaseListID() {
        return baseListID;
    }

    /**
     * Returns the OpenGL list name of the terrain surface.
     * @return
     */
    public int getTerrainListID() {
        return terrainListID;
    }

    @Override
    public void releaseModel(GL gl) {
    }

    @Override
    public boolean canRun() {
        return true;
    }

}
