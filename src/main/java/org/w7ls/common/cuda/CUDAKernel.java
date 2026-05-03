package org.w7ls.common.cuda;

import jcuda.driver.CUdeviceptr;
import org.w7ls.annotation.cuda.CUKernelRequire;

import static org.w7ls.annotation.cuda.CUTypes.*;

public interface CUDAKernel extends AutoCloseable {
    static UnsupportedOperationException unsupportedDimension(/*int req, int res*/) {
        throw new UnsupportedOperationException("the method cant use that dimension. "+"");
    }


    /**
     * Example code invoke mandelbrot-set(If when use pointer is rgb.).<br>
     * in java:
     * <pre>{@code
     * int w = 512;
     * int h = 512;
     * int count = w * h;
     * float[] r = new float[count];
     * float[] g = new float[count];
     * float[] b = new float[count];
     *
     * f.invoke2d("mandelbrot_set", w, h, r, g, b);
     * }<pre/><br>
     * in cuda:
     * <pre>{@code
     * extern "C"
     * __global__ void mandelbrot_set(int w, int h, float *r, float *g, float *b) {
     *     int x = blockIdx.x * blockDim.x + threadIdx.x;
     *     int y = blockIdx.y * blockDim.y + threadIdx.y;
     *     int i = y * w + x;
     *     ...
     *     r[i] = res;
     *     g[i] = res;
     *     b[i] = res;
     * }
     *
     * }</pre>
     *
     * @param function function name in cuida file.
     * @param count length of arg. if when invoke(~, ~, new float[40]), count is 40.
     * @param arg Argument that pass to cuda. The argument can use pointer. If when use pointer, write in cuda file.
     * @return all pointer
     */
    CUdeviceptr[] invoke(String function,
                         @CUKernelRequire(type = _int) int count,
                         Object... arg);

    CUdeviceptr[] invoke2d(String function,
                           @CUKernelRequire(type = _int) int w,
                           @CUKernelRequire(type = _int) int h, Object... arg);

    CUdeviceptr[] invoke3d(String function,
                           @CUKernelRequire(type = _int) int w,
                           @CUKernelRequire(type = _int) int h,
                           @CUKernelRequire(type = _int) int d, Object... arg);

    CUdeviceptr[] invokeXd(String function, int w, int h, int d, int blockSize, Object...arg);

    void setParallel(boolean parallel);

    void setSendCPU(boolean send);

    boolean isAutoSendHost();

    boolean isParallel();
}
