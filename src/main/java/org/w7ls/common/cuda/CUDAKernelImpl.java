package org.w7ls.common.cuda;

import jcuda.Pointer;
import jcuda.driver.*;
import jcuda.runtime.JCuda;
import org.jetbrains.annotations.Nullable;
import org.w7ls.annotation.cuda.CUKernelRequire;

import java.io.IOException;
import java.lang.constant.ConstantDesc;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static jcuda.driver.JCudaDriver.*;
import static jcuda.runtime.JCuda.*;
import static org.w7ls.annotation.cuda.CUTypes._int;
import static org.w7ls.annotation.cuda.CUTypes.p_float;
import static org.w7ls.common.cuda.CUWrapper.*;
import static org.w7ls.common.utils.StringTmp.*;
import static org.w7ls.common.utils.StringTmp.str;

class CUDAKernelImpl implements CUDAKernel {
    private static final String ptxDir = "bin/cuda";
    private static final String cuDir = "bin/cuda/tmp";

    protected final static CUcontext cucontext;
    final List<String> sfNames = new ArrayList<>();
    final HashMap<String, CUfunction> cuFunctions = new HashMap<>();
    boolean parallel = false;
    boolean sendCPU = true;

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

    public CUDAKernelImpl(String file, Set<String> functionNames) {
        this(Path.of(str(ptxDir, "/", file, ".ptx")), functionNames);
    }

    public CUDAKernelImpl(Path file, Set<String> functionNames) {
        CUmodule module = new CUmodule();
        cuModuleLoad(module, file.toString());
        for (String functionName : functionNames) {
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

    public static CUDAKernel srcToPtxFile(String src, CUDAKernels.KernelTypes kernel, Set<String> functoinNames) {
        String sfname = Long.toHexString(new Date().getTime());
        String fname = strSep("/", ptxDir, sfname+".cu");
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
        CUDAKernelImpl re;
        try {
            re = kernel.type.getConstructor(fname.getClass(), sfname.getClass()).newInstance(fname, pname);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        re.sfNames.add(sfname);
        return re;
    }

    @CUKernelRequire(type = _int, name = "c")
    @CUKernelRequire(type = p_float, name = "p")
    public CUdeviceptr[] invoke(String function, int count, Object... args) {
        return _invoke(function, count, new CUdeviceptr[args.length], args);
    }

    /**
     * @see CUDAKernelImpl#invoke(String, int, Object...)
     */
    @CUKernelRequire(type = _int, name = "w")
    @CUKernelRequire(type = _int, name = "h")
    @CUKernelRequire(type = p_float, name = "p")
    public CUdeviceptr[] invoke2d(String function, int w, int h, Object...args) {
        return _invoke2d(function, w, h, new CUdeviceptr[args.length], args);
    }

    /**
     * @see CUDAKernelImpl#invoke(String, int, Object...)
     */
    @CUKernelRequire(type = _int, name = "h")
    @CUKernelRequire(type = _int, name = "d")
    @CUKernelRequire(type = p_float, name = "p")
    public CUdeviceptr[] invoke3d(String function, int w, int h, int d, Object...args) {
        return _invoke3d(function, w, h, d, new CUdeviceptr[args.length], args);
    }

    CUdeviceptr[] _invoke(String function, int count, CUdeviceptr[] dargs, Object...args) {
        return _invokeXd(function, count, 1, 1, 256, dargs, args);
    }

    CUdeviceptr[] _invoke2d(String function, int w, int h, CUdeviceptr[] dargs, Object...args) {
        return _invokeXd(function, w, h, 1, 16, dargs, args);
    }

    CUdeviceptr[] _invoke3d(String function, int w, int h, int d, CUdeviceptr[] dargs, Object...args) {
        return _invokeXd(function, w, h, d, 8, dargs, args);
    }

    public CUdeviceptr[] invokeXd(String function, int w, int h, int d, int bsize, Object... args) {
        if (d > 1)
            return invoke3d(function, w, h, d, args);
        else if (h > 1)
            return invoke2d(function, w, h, args);
        else if (w > 1)
            return invoke(function, w, args);
        return null;
    }

    CUdeviceptr[] _invokeXd(String function, int w, int h, int d, int bsize, CUdeviceptr[] dargs, Object...args) {
        final int argcount = args.length;

        int dimcount = 0;
        if (w != 1) dimcount++;
        if (h != 1) dimcount++;
        if (d != 1) dimcount++;

        final Pointer[]     hargs   = new Pointer[argcount + dimcount];
        final long[]        sizes   = new long[argcount];

        if (parallel) cuCtxSetCurrent(cucontext);

        final int[] countArrW = new int[]{w};
        final int[] countArrH = new int[]{h};
        final int[] countArrD = new int[]{d};
        if (w != 1) hargs[0] = Pointer.to(countArrW);
        if (h != 1) hargs[1] = Pointer.to(countArrH);
        if (d != 1) hargs[2] = Pointer.to(countArrD);

        for (int i = 0; i < argcount; i++) {
            Object arg = args[i];
            sizes[i] = sizebyte(arg);

            if (dargs[i] != null) {
                hargs[i + dimcount] = Pointer.to(dargs[i]);
            } else if (arg.getClass().isArray()) {
                dargs[i] = cpyPointerHtoD(sizes[i], arg);
                hargs[i + dimcount] = Pointer.to(dargs[i]);
            } else if (arg instanceof CUWrapper<?> wrapper) {
                dargs[i] = cpyPointerHtoD(sizes[i], wrapper.p);
                hargs[i + dimcount] = Pointer.to(dargs[i]);
            } else {
                dargs[i] = null;
                Object tmp = scalarToArray(arg);
                hargs[i + dimcount] = toPointer(tmp);
            }
        }


        Pointer kernelParms = Pointer.to(hargs);

        int gridSizeW = (w + bsize - 1) / bsize;
        int gridSizeH = (h + bsize - 1) / bsize;
        int gridSizeD = (d + bsize - 1) / bsize;

        cuLaunchKernel(cuFunctions.get(function), gridSizeW, gridSizeH, gridSizeD,
                w > 1 ? bsize : 1,
                h > 1 ? bsize : 1,
                d > 1 ? bsize : 1, 0, null, kernelParms, null);

        endPtrOper(argcount, sizes, dargs, args);
        return dargs;
    }

    public final void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public final void setSendCPU(boolean sendCPU) {
        this.sendCPU = sendCPU;
    }

    @Override
    public boolean isAutoSendHost() {
        return sendCPU;
    }

    @Override
    public final boolean isParallel() {
        return parallel;
    }
    
    boolean isAutoMemFree() {
        return true;
    }

    void endPtrOper(int argcount, long[] sizes, CUdeviceptr[] dargs, Object[] args) {
        for (int i = 0; i < argcount; i++)
            if (dargs[i] != null) {
                if (isAutoSendHost()) {
                    if (args[i] instanceof CUWrapper<?> wrapper) {
                        cpyPointerDtoH(sizes[i], wrapper.p, dargs[i]);
                    } else
                        cpyPointerDtoH(sizes[i], args[i], dargs[i]);
                } if (isAutoMemFree())
                    cuMemFree(dargs[i]);
            }
    }

    static Object scalarToArray(Object arg) {
        if (arg instanceof Float a)     return new float[]{a};
        if (arg instanceof Integer a)   return new int[]{a};
        if (arg instanceof Double a)    return new double[]{a};
        if (arg instanceof Long a)      return new long[]{a};
        if (arg instanceof Short a)     return new short[]{a};
        if (arg instanceof Byte a)      return new byte[]{a};
        throw CUWrapper.noSupportTypeArg.apply(arg);
    }

    static @Nullable Pointer toPointer(Object arr) {
        if (arr instanceof float[] a)  return Pointer.to(a);
        if (arr instanceof int[] a)    return Pointer.to(a);
        if (arr instanceof double[] a) return Pointer.to(a);
        if (arr instanceof long[] a)   return Pointer.to(a);
        if (arr instanceof short[] a)  return Pointer.to(a);
        if (arr instanceof byte[] a)   return Pointer.to(a);
        if (arr.getClass().isPrimitive() || arr instanceof Number) return null;
        throw CUWrapper.noSupportTypeArg.apply(arr);
    }

    // CUWrapper対応済み
    static @Nullable CUdeviceptr cpyPointerHtoD(long size, Object arg) {
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
            else throw CUWrapper.noSupportTypeArg.apply(arg);
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
        else throw CUWrapper.noSupportTypeArg.apply(arg);

        return darg;
    }

    static void cpyPointerDtoH(long size, Object arg, CUdeviceptr darg) {
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
            else throw CUWrapper.noSupportTypeArg.apply(arg);
        else if (!(arg instanceof ConstantDesc))
            throw CUWrapper.noSupportTypeArg.apply(arg);
    }

    @Override
    public void close() throws Exception {}
}
