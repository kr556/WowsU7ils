package org.w7ls.cuda;

import org.junit.jupiter.api.Test;
import org.w7ls.annotation.cuda.*;

import static org.w7ls.annotation.cuda.CUHeader.HeaderFileType.PTX_FILE;
import static org.w7ls.annotation.cuda.CULaunchKernelOption.USER_SETTING;
import static org.w7ls.annotation.cuda.CUTypes._int;
import static org.w7ls.annotation.cuda.CUTypes.p_float;

public class CUDAKernelsTest {
    @Test
    void testUseImpl() {
    }

    public static int toRGB(float r, float g, float b) {
        int ri = (int) (255 * r);
        int gi = (int) (255 * g);
        int bi = (int) (255 * b);
        return (ri << 16) |
               (gi << 8) |
               (bi);
    }

    @CUHeader(cudaCode = "mandelbrot_set", codeType = PTX_FILE)
    static abstract class TestCuda {
        @CUMethod(functionName = "mandelbrot_set", args = {_int, _int, p_float, p_float, p_float})
        @CULaunchKernelOption(w = USER_SETTING, h = USER_SETTING)
        public abstract void mandelbrotSet(int w, int h, float[] r, float[] g, float[] b);
    }
}