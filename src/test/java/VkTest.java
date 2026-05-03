import org.w7ls.annotation.cuda.CUHeader;
import org.w7ls.annotation.cuda.CULaunchKernelOption;
import org.w7ls.annotation.cuda.CUMethod;

import static org.w7ls.annotation.cuda.CUHeader.HeaderFileType.PTX_FILE;
import static org.w7ls.annotation.cuda.CULaunchKernelOption.USER_SETTING;
import static org.w7ls.annotation.cuda.CUTypes.*;

public class VkTest {
    public static void main(String[] args) {
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
    public interface TestCUStruct {
        @CULaunchKernelOption(w = USER_SETTING, h = USER_SETTING)
        @CUMethod(functionName = "mandelbrot_set",args = {_int, _int, p_float, p_float, p_float,})
        void mandelbrotSet(int w, int h, float[] r, float[] g, float[] b);
    }

    @CUHeader(cudaCode = "mandelbulb", codeType = PTX_FILE)
    public abstract static class TestCUStruct2 {
        @CULaunchKernelOption(w = USER_SETTING, h = USER_SETTING)
        @CUMethod(functionName = "mandelbulb", args = {_int, _int, p_float, p_float, p_float, _float, _float, _float, _float, _int})
        public abstract void mandelbulb(int w, int h,
                                        float[] r, float[] g, float[] b,
                                        float c_x, float c_y, float c_z,
                                        float power, int maxIter);
    }
}
