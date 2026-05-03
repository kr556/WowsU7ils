package org.w7ls.common.cuda;

import jcuda.driver.CUdeviceptr;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;

//public class CUDAKernelVk extends CUDAKernels.ManualMemFreeKernel {
//    public CUDAKernelVk(String file, Set<String> functionNames) {
//        super(file, functionNames);
//    }
//
//    public CUDAKernelVk(Path fname, Set<String> functionNames) {
//        super(fname, functionNames);
//    }
//
//    public /*TODO private*/ static long getNativeCudaptrAddress(CUdeviceptr cUdeviceptr) {
//        try {
//            Field nativePointer = cUdeviceptr.getClass()
//                    .getSuperclass()
//                    .getSuperclass()
//                    .getDeclaredField("nativePointer");
//            nativePointer.setAccessible(true);
//            return nativePointer.getLong(cUdeviceptr);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    public CUdeviceptr[] invoke(String function, int count, Object... args) {
//        return registersMemFree(super.invoke(function, count, args));
//    }
//
////    @Override
////    public CUdeviceptr[] invoke2d(String function, int w, int h, Object... args) {
////        return registersMemFree(super.invoke2d(function, w, h, args));
////    }
//
//    @Override
//    public CUdeviceptr[] invoke3d(String function, int w, int h, int d, Object... args) {
//        return registersMemFree(super.invoke3d(function, w, h, d, args));
//    }
//
//    @Override
//    public final CUdeviceptr[] invokeXd(String function, int w, int h, int d, int bsize, Object... args) {
//        return super.invokeXd(function, w, h, d, bsize, args);
//    }
//}
