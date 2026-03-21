package org.w7ls.main;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;
import jcuda.runtime.JCuda;

import java.io.IOException;
import java.lang.constant.ConstantDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static jcuda.driver.JCudaDriver.*;
import static  jcuda.runtime.JCuda.*;
import static org.w7ls.utils.StringTmp.*;
import static org.w7ls.utils.StringTmp.str;

public class CUDAHeader {
    private static final String ptxDir = "bin/cuda";
    private static final String cuDir = "bin/cuda/tmp";

    private static final Function<Object, RuntimeException> noSupportTypeArg = o -> new IllegalArgumentException(" the type is no supported : " + o);
    private final List<String> sfNames = new ArrayList<>();
    private final HashMap<String, CUfunction> cuFunctions = new HashMap<>();
    private boolean parallel = false;
    private final static CUcontext cucontext;

    static {
        JCuda.setExceptionsEnabled(true);
        cudaSetDevice(0);

        JCudaDriver.setExceptionsEnabled(true);
        cuInit(0);

        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);

        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);
        cucontext = context;
    }

    public CUDAHeader(String file, Set<String> functoinNames) {
        this(Path.of(str(ptxDir, "/", file, ".ptx")), functoinNames);
    }

    public CUDAHeader(Path file, Set<String> functoinNames) {
        CUmodule module = new CUmodule();
        cuModuleLoad(module, file.toString());
        for (String functionName : functoinNames) {
            CUfunction cufunction = new CUfunction();
            cuModuleGetFunction(cufunction, module, functionName);
            cuFunctions.put(functionName, cufunction);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (String sfname : sfNames) {
                // 絶対にtryを統合しないで
                try {
                    Files.delete(Path.of(strSep("/", ptxDir, sfname+".ptx")));
                } catch (IOException ignored) {}
                try {
                    Files.delete(Path.of(strSep("/", cuDir, sfname+".cu")));
                } catch (IOException ignored) {}
            }
        }));
    }

    public static CUDAHeader toCudaFile(String src, Set<String> functoinNames) {
        String sfname = Long.toHexString(new Date().getTime());
        String fname = strSep("/", ptxDir,"tmp", sfname+".cu");
        String pname = strSep("/", ptxDir, sfname+".ptx");


        try {
            Path path = Path.of(fname);
            Files.createFile(path);
            Files.write(path, src.getBytes());
            Process cmd = new ProcessBuilder("nvcc", "-ptx", fname, "-o", pname)
                    .redirectErrorStream(true)
                    .start();
            cmd.getInputStream().readAllBytes(); // 消すな
            cmd.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        CUDAHeader re = new CUDAHeader(sfname, functoinNames);
        re.sfNames.add(sfname);
        return re;
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
     *     float px = (x - w * 0.8f) / (w * 0.25f);
     *     float py = (y - h * 0.5f) / (h * 0.25f);
     *     float tmp = 0;
     *     float zx = 0;
     *     float zy = 0;
     *     float c;
     *     float max = 32768;
     *     for (c = 0; c < max && (zx * zx + zy * zy < 4.0); c++) {
     *         tmp = zx * zx - zy * zy + px;
     *         zy = 2.0 * zx * zy + py;
     *         zx = tmp;
     *     }
     *     float res = c / max;
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
     */
    public void invoke(String function, int count, Object arg) {
        invoke(function, count, new Object[]{arg});
    }

    /**
     * @see CUDAHeader#invoke(String, int, Object)
     */
    public void invoke(String function, int count, Object... args) {
        invokeXd(function, count, 1, 1, 256, args);
    }

    /**
     * @see CUDAHeader#invoke(String, int, Object)
     */
    public void invoke2d(String function, int w, int h, Object...args) {
        invokeXd(function, w, h, 1, 16, args);
    }

    private void invoke3d(String function, int w, int h, int d,  Object...args) {
        invokeXd(function, w, h, d, 8, args);
    }

    private void invokeXd(String function, int w, int h, int d, int bsize, Object...args) {
        final int argcount = args.length;
        final int count = w * h * d;

        final CUdeviceptr[] dargs   = new CUdeviceptr[argcount];
        final Pointer[]     hargs   = new Pointer[argcount + 1];
        final long[]        sizes   = new long[argcount];

        if (parallel) cuCtxSetCurrent(cucontext);

        final int[] countArr = new int[]{count};
        hargs[0] = Pointer.to(countArr);

        for (int i = 0; i < argcount; i++) {
            Object arg = args[i];
            sizes[i] = sizebyte(arg);

            if (arg.getClass().isArray()) {
                dargs[i]    = cpyPointerHtoD(sizes[i], arg);
                hargs[i+1]  = Pointer.to(dargs[i]);
            } else {
                dargs[i] = null;
                Object tmp = scalarToArray(arg);
                hargs[i+1] = toPointer(tmp);
            }
        }

        Pointer kernelParms = Pointer.to(hargs);

        final int gridSize  = (count + bsize - 1) / bsize;

        if (d > 1)
            cuLaunchKernel(cuFunctions.get(function), gridSize, gridSize, gridSize, bsize, bsize, bsize, 0, null, kernelParms, null);
        else if (h > 1)
            cuLaunchKernel(cuFunctions.get(function), gridSize, gridSize, 1, bsize, bsize, 1, 0, null, kernelParms, null);
        else if (w > 1)
            cuLaunchKernel(cuFunctions.get(function), gridSize, 1, 1, bsize, 1, 1, 0, null, kernelParms, null);

        for (int i = 0; i < argcount; i++)
            if (dargs[i] != null) {
                cpyPointerDtoH(sizes[i], args[i], dargs[i]);
                cuMemFree(dargs[i]);
            }
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    private Object scalarToArray(Object arg) {
        if (arg instanceof Float a)     return new float[]{a};
        if (arg instanceof Integer a)   return new int[]{a};
        if (arg instanceof Double a)    return new double[]{a};
        if (arg instanceof Long a)      return new long[]{a};
        if (arg instanceof Short a)     return new short[]{a};
        if (arg instanceof Byte a)      return new byte[]{a};
        throw noSupportTypeArg.apply(arg);
    }

    private Pointer toPointer(Object arr) {
        if (arr instanceof float[] a)  return Pointer.to(a);
        if (arr instanceof int[] a)    return Pointer.to(a);
        if (arr instanceof double[] a) return Pointer.to(a);
        if (arr instanceof long[] a)   return Pointer.to(a);
        if (arr instanceof short[] a)  return Pointer.to(a);
        if (arr instanceof byte[] a)   return Pointer.to(a);
        throw new RuntimeException("unsupported: " + arr);
    }

    private long sizebyte(Object arg) {
        if (arg.getClass().isArray())
            if (arg instanceof float[] arg_)
                return (long) Sizeof.FLOAT * arg_.length;
            else if (arg instanceof int[] arg_)
                return (long) Sizeof.INT * arg_.length;
            else if (arg instanceof double[] arg_)
                return (long) Sizeof.DOUBLE * arg_.length;
            else if (arg instanceof byte[] arg_)
                return (long) Sizeof.BYTE * arg_.length;
            else if (arg instanceof long[] arg_)
                return (long) Sizeof.LONG * arg_.length;
            else if (arg instanceof short[] arg_)
                return (long) Sizeof.SHORT * arg_.length;
            else if (arg instanceof char[] arg_)
                return (long) Sizeof.INT * arg_.length;
            else throw noSupportTypeArg.apply(arg);
        else if (arg instanceof Float)
            return Sizeof.FLOAT;
        else if (arg instanceof Integer)
            return Sizeof.INT;
        else if (arg instanceof Double)
            return Sizeof.DOUBLE;
        else if (arg instanceof Byte)
            return Sizeof.BYTE;
        else if (arg instanceof Long)
            return Sizeof.LONG;
        else if (arg instanceof Short)
            return Sizeof.SHORT;
        else if (arg instanceof Character)
            return Sizeof.INT;
        else throw noSupportTypeArg.apply(arg);
    }

    private CUdeviceptr cpyPointerHtoD(long size, Object arg) {
        CUdeviceptr darg = new CUdeviceptr();
        cuMemAlloc(darg, size);
        if (arg.getClass().isArray()) {
            if (arg instanceof float[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof int[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof double[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof byte[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof long[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof short[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else if (arg instanceof char[] arg_)
                cuMemcpyHtoD(darg, Pointer.to(arg_), size);
            else throw noSupportTypeArg.apply(arg);
        } else if (arg instanceof Float arg_)
            cuMemcpyHtoD(darg, Pointer.to(new float[]{arg_}), size);
        else if (arg instanceof Integer arg_)
            cuMemcpyHtoD(darg, Pointer.to(new int[]{arg_}), size);
        else if (arg instanceof Double arg_)
            cuMemcpyHtoD(darg, Pointer.to(new double[]{arg_}), size);
        else if (arg instanceof Byte arg_)
            cuMemcpyHtoD(darg, Pointer.to(new byte[]{arg_}), size);
        else if (arg instanceof Long arg_)
            cuMemcpyHtoD(darg, Pointer.to(new long[]{arg_}), size);
        else if (arg instanceof Short arg_)
            cuMemcpyHtoD(darg, Pointer.to(new short[]{arg_}), size);
        else if (arg instanceof Character arg_)
            cuMemcpyHtoD(darg, Pointer.to(new char[]{arg_}), size);
        else throw noSupportTypeArg.apply(arg);

        return darg;
    }

    private void cpyPointerDtoH(long size, Object arg, CUdeviceptr darg) {
        if (arg.getClass().isArray())
            if (arg instanceof float[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof int[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof double[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof byte[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof long[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof short[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else if (arg instanceof char[] arg_)
                cuMemcpyDtoH(Pointer.to(arg_), darg, size);
            else throw noSupportTypeArg.apply(arg);
        else if (!(arg instanceof ConstantDesc))
            throw noSupportTypeArg.apply(arg);
    }
}
