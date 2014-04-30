package ch.ethz.karto.map3d;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

/**
 * Vertex and fragment shader.
 * @author Bernhard Jenny, Institute of Cartography, ETH ZUrich
 */
public class Map3DShader {
    
    private static final String INVALID_UNIFORM_EXCEPTION_MSG = "Invalid uniform "
            + "location. Uniform may be defined but not be used by the shader code.";
    
    private int vertexShader, fragmentShader, shaderProgram;
    protected Map3DShader() {
    }

    /**
     * Loads a vertex and a fragment shader from two files; compiles and links
     * the shaders; and creates and links a shader program and attaches the
     * shaders. At the end the shaders are ready to be used.
     * @param gl
     * @param vShaderPath A path to a file with the vertex shader code.
     * @param fShaderPath A path to a file with the fragment shader code.
     * @param attribName Array of attribute names used in shaders. Can be null.
     * @param attribID Array of indices associated with the attribute names.
     * Can be null.
     */
    protected void enableShaders(GL gl1, String vShaderPath, String fShaderPath,
            String[] attribName, int[] attribID) {

        GL2 gl = (GL2)gl1;

        int[] params = new int[1];
        
        String src = loadShader(vShaderPath);
        vertexShader = enableShader(gl, src, GL2.GL_VERTEX_SHADER);
        
        src = loadShader(fShaderPath);
        fragmentShader = enableShader(gl, src, GL2.GL_FRAGMENT_SHADER);
       
        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);

        // bind attribute names used in shaders to predefined locations
        // slot 0 should be used, normally for vertices
        // http://www.gamedev.net/community/forums/topic.asp?topic_id=413008
        if (attribName != null && attribID != null) {
            assert attribName.length == attribID.length;
            for (int i = 0; i < attribID.length; i++) {
                gl.glBindAttribLocation(shaderProgram, attribID[i], attribName[i]);
            }
        }

        gl.glLinkProgram(shaderProgram);

        // check for linking error
        gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, params, 0);
        if (params[0] == 0) {
            // failure to link
            gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, params, 0);
            String msg = "Error on shader program linking";
            if (params[0] > 0) {
                byte[] log = new byte[params[0]];
                gl.glGetProgramInfoLog(shaderProgram, params[0], params, 0, log, 0);
                msg += ": " + new String(log, 0, log.length - 1);
                gl.glDeleteProgram(shaderProgram);
            }
            throw new GLException(msg);
        }

        gl.glUseProgram(shaderProgram);
    }

    protected int getAttribLocation(GL gl1, String attribName) {
        GL2 gl = (GL2)gl1;
        return gl.glGetAttribLocation(shaderProgram, attribName);
    }

    /**
     * Validate a shader program before rendering. This should only be called
     * during application development, because it can severly hinder performance.
     * @param gl
     * @return
     */
    protected boolean validateProgram(GL gl1) {
        int[] params = new int[1];
        GL2 gl = (GL2)gl1;
        gl.glValidateProgram(shaderProgram);
        gl.glGetProgramiv(shaderProgram, GL2.GL_VALIDATE_STATUS, params, 0);
        if (params[0] == 0) {
            // validation failed
            gl.glGetProgramiv(shaderProgram, GL2.GL_LINK_STATUS, params, 0);
            String msg = "Error on shader program validation";
            if (params[0] > 0) {
                byte[] log = new byte[params[0]];
                gl.glGetProgramInfoLog(shaderProgram, params[0], params, 0, log, 0);
                msg += ": " + new String(log, 0, log.length - 1);
                gl.glDeleteProgram(shaderProgram);
            }
            System.err.println(msg);
        }
        return params[0] != 0;
    }

    protected void disableShaders(GL gl1) {
        GL2 gl = (GL2)gl1;

        gl.glUseProgram(0);

        // glDeleteProgram and glDeleteShader silently ignore a value of 0
        gl.glDeleteProgram(shaderProgram);
        shaderProgram = 0;

        gl.glDeleteShader(vertexShader);
        vertexShader = 0;

        gl.glDeleteShader(fragmentShader);
        fragmentShader = 0;
    }

    private int enableShader(GL gl1, String src, int shaderType) {

        int[] params = new int[1];
        GL2 gl = (GL2)gl1;
        int shaderID = gl.glCreateShader(shaderType);

        if (shaderID != 0 && src != null) {
            gl.glShaderSource(shaderID, 1, new String[]{src}, (int[]) null, 0);
            gl.glCompileShader(shaderID);

            // check for compilation errors
            gl.glGetShaderiv(shaderID, GL2.GL_COMPILE_STATUS, params, 0);
            if (params[0] == 0) {
                int[] buffer = new int[1];
                gl.glGetShaderiv(shaderID, GL2.GL_INFO_LOG_LENGTH, buffer, 0);
                String msg = "Error on shader compilation";
                if (buffer[0] > 0) {
                    byte[] log = new byte[buffer[0]];
                    gl.glGetShaderInfoLog(shaderID, buffer[0], buffer, 0, log, 0);
                    msg += ": " + new String(log, 0, log.length - 1);
                    gl.glDeleteShader(shaderID);
                }
                throw new GLException(msg);
            }
        }
        return shaderID;
    }

    private String loadShader(String shaderPath) {
        BufferedReader reader = null;
        try {
            java.net.URL url = Map3DViewer.class.getResource(shaderPath);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            InputStreamReader isr = new InputStreamReader(bis, "UTF-8");
            reader = new BufferedReader(isr);
            String line, shader = "";
            while ((line = reader.readLine()) != null) {
                shader += line + "\n";
            }
            return shader;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    protected void setUniform(GL gl1, String name, boolean i) {
        GL2 gl = (GL2)gl1;
        int loc = gl.glGetUniformLocation(shaderProgram, name);
        if (loc != -1) {
            // a uniform bool in a vertex shader is set using an int
            gl.glUniform1i(loc, i ? 1 : 0);
        } else {
            throw new GLException(INVALID_UNIFORM_EXCEPTION_MSG);
        }
    }

    protected void setUniform(GL gl1, String name, int i) {
        GL2 gl = (GL2)gl1;
        int loc = gl.glGetUniformLocation(shaderProgram, name);
        if (loc != -1) {
            gl.glUniform1i(loc, i);
        } else {
            throw new GLException(INVALID_UNIFORM_EXCEPTION_MSG);
        }
    }

    protected void setUniform(GL gl1, String name, float v) {
        GL2 gl = (GL2)gl1;
        int loc = gl.glGetUniformLocation(shaderProgram, name);
        if (loc != -1) {
            gl.glUniform1f(loc, v);
        } else {
            throw new GLException(INVALID_UNIFORM_EXCEPTION_MSG);
        }
    }

    protected void setUniform(GL gl1, String name, float v1, float v2, float v3) {
        GL2 gl = (GL2)gl1;
        int loc = gl.glGetUniformLocation(shaderProgram, name);
        if (loc != -1) {
            gl.glUniform3fv(loc, 1, new float[]{v1, v2, v3}, 0);
        } else {
            throw new GLException(INVALID_UNIFORM_EXCEPTION_MSG);
        }
    }

    protected void setUniform(GL gl1, String name, float v1, float v2) {
        GL2 gl = (GL2)gl1;
        int loc = gl.glGetUniformLocation(shaderProgram, name);
        if (loc != -1) {
            gl.glUniform2fv(loc, 1, new float[]{v1, v2}, 0);
        } else {
            throw new GLException(INVALID_UNIFORM_EXCEPTION_MSG);
        }
    }
}
