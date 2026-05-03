package org.w7ls.chart;

import org.w7ls.annotation.cuda.CUHeader;
import org.w7ls.common.W7lsProperties;
import org.w7ls.common.cuda.CUDAKernels;
import org.w7ls.common.cuda.CUImagePtr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.w7ls.annotation.cuda.CUHeader.HeaderFileType.CU_SRC_FILE;

@CUHeader(cudaCode = "scatter", codeType = CU_SRC_FILE)
public class Scatter implements Chart {
    private static CUDAKernels.CUDAImageKernel plotter;
    private int width, height;
    private CUImagePtr plotedImage;

    static {
        try {
            String src = Files.readString(Path.of(W7lsProperties.cuSrcDir ));
            System.out.println(src);
            plotter = CUDAKernels.useImage(src, Set.of("plot"));
        } catch (IOException e) {
            throw new RuntimeException("cant create ptx file. " + e);
        }
    }

    public Scatter(int w, int h) {
        width = w;
        height = h;
//        plotedImage = new CUImagePtr(w, h);
    }

    @Override
    public boolean plot() {
//        plotter.invokeImage("plot", plotedImage);
        return false;
    }
}
