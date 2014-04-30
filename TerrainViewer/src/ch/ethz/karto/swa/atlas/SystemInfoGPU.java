package ch.ethz.karto.swa.atlas;

import ch.ethz.karto.map3d.Map3DGLCapabilities;
import ika.utils.PropertiesLoader;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.Properties;


import javax.media.opengl.glu.GLU;

public class SystemInfoGPU extends SystemInfo {

    private static final String GPUINFO_FILE = "ch.ethz.karto.swa.atlas.gpuinfo";

    private Properties properties;

    public SystemInfoGPU(Frame parent) {
        super (parent);
    }

    @Override
    protected void addInfo() {
        super.addInfo();

        properties = PropertiesLoader.loadProperties(GPUINFO_FILE, true);

        addTitle2(res("gpuTitle"));
        if (!Map3DGLCapabilities.hasOpenGL()) {
            addParagraph(res("notAvailable"));
        } else {
            String str = Map3DGLCapabilities.getGLRenderer();
            String str2 = Map3DGLCapabilities.getGLVendor();
            addParagraph(String.format(res("gpuInfo"), str, str2));
            addParagraph(String.format(res("gpuVRAM"), getAvailableAcceleratedMemory() / MB));
            str = Map3DGLCapabilities.getGLVersion();
            addParagraph(String.format(res("glVersion"), str));
            addParagraph(String.format(res("gluVersion"), GLU.versionString));

            // GL_SHADING_LANGUAGE_VERSION is available in OpenGL 2.0 and higher
            if (Map3DGLCapabilities.hasOpenGLVersion(2, 0)) {
                str = Map3DGLCapabilities.getGLShadingLanguageVersion();
                addParagraph(String.format(res("glslInfo"), str));
            }

            int bufs = Map3DGLCapabilities.getMaxDrawBuffers();
            addParagraph(String.format(res("gpuBuffers"), bufs));
            int texSize = Map3DGLCapabilities.getMaxTextureSize();
            addParagraph(String.format(res("gpuTexture"), texSize, texSize));
        }

        doc.setParagraphAttributes(0, doc.getLength(), docStyle, true);
        systemInfo.setCaretPosition(0);
    }

    private String res(String id) {
        return properties.getProperty(id);
    }

    private static int getAvailableAcceleratedMemory() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getAvailableAcceleratedMemory();
    }

}
