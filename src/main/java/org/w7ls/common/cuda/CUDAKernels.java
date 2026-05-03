package org.w7ls.common.cuda;

import jcuda.driver.CUdeviceptr;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nd4j.shade.guava.collect.Iterables;
import org.nd4j.shade.guava.collect.Sets;
import org.w7ls.annotation.cuda.*;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static jcuda.driver.JCudaDriver.cuMemFree;
import static org.w7ls.common.cuda.CUDAKernel.*;
import static org.w7ls.common.W7lsProperties.*;
import static org.w7ls.annotation.cuda.CULaunchKernelOption.*;
import static org.w7ls.annotation.cuda.CUTypes.*;
import static org.w7ls.common.cuda.CUWrapper.noSupportTypeArg;
import static org.w7ls.common.cuda.CUWrapper.sizebyte;

public final class CUDAKernels {
    private CUDAKernels() {}

    public enum KernelTypes {
        NORMAL(CUDAKernelImpl.class),
        MANUAL_MEM_FREE(ManualMemFreeKernel.class),
        ;
        final Class<? extends CUDAKernelImpl> type;

        KernelTypes(Class<? extends CUDAKernelImpl> type) {
            this.type = type;
        }
    }

    // オブジェクトをcudaで実装する(多分ちょっとオーバーヘッドが大きい)
    @Nullable
    public static <R> R bindInstance(Class<R> clazz) {
        return bindInstance(clazz, KernelTypes.NORMAL);
    }

    /**
     * @param clazz require implements {@link BindableImage}. not need implement method that BindableImage.
     */
    @SuppressWarnings({"unchecked", "resource"})
    public static <R extends AutoCloseable & BindableImage> R bindInstanceImage(Class<R> clazz, CUImagePtr canvas) {
        CUDAKernel kernel;
        CUHeader cuHeader = clazz.getAnnotation(CUHeader.class);
        String cuc = cuHeader.cudaCode();
        kernel = switch (cuHeader.codeType()) {
            case PTX_FILE -> new CUDAImageKernel(cuc, Set.of(
                    Arrays.stream(clazz.getMethods())
                            .filter(m -> m.getAnnotation(CUMethod.class) != null)
                            .map(m -> m.getAnnotation(CUMethod.class).functionName())
                            .toArray(String[]::new)), canvas);
            case CU_SRC_FILE, SRC_CODE -> throw noSupportTypeArg.apply("has not been implemented yet. sorry!");
        };

        if (clazz.isInterface())
            return (R) Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[]{clazz},
                    new KernelInvoker(kernel));
        else {
            try {
                KernelInterceptor knlIcr = new KernelInterceptor(kernel);
                return new ByteBuddy().subclass(clazz)
                        .method(ElementMatchers.isAbstract())
                        .intercept(MethodDelegation.to(knlIcr))
                        .make()
                        .load(clazz.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("cant make instance. " + e);
            }
        }
    }

    @Nullable
    public static <R extends AutoCloseable> R bindInstanceCloseable(Class<R> clazz, KernelTypes types) {
        return bindInstance(clazz, types);
    }

    @Nullable
    @SuppressWarnings({"unchecked", "resource"})
    public static <R> R bindInstance(Class<R> clazz, KernelTypes kernelType) {
        CUDAKernel kernel;
        try {
            CUHeader cuHeader = clazz.getAnnotation(CUHeader.class);
            String cuc = cuHeader.cudaCode();
            kernel = switch (cuHeader.codeType()) {
                case PTX_FILE -> CUDAKernels.use(cuc, kernelType, Set.of(
                        Arrays.stream(clazz.getMethods())
                                .filter(m -> m.getAnnotation(CUMethod.class) != null)
                                .map(m -> m.getAnnotation(CUMethod.class).functionName())
                                .toArray(String[]::new)));
                case CU_SRC_FILE -> CUDAKernelImpl.srcToPtxFile(Files.readString(Path.of(cuSrcDir + '/' + cuc + cuFileExtension)), kernelType, Set.of(
                        Arrays.stream(clazz.getMethods())
                                .filter(m -> m.getAnnotation(CUMethod.class) != null)
                                .map(m -> m.getAnnotation(CUMethod.class).functionName())
                                .toArray(String[]::new)));
                case SRC_CODE -> CUDAKernelImpl.srcToPtxFile(cuc, kernelType, Set.of(
                        Arrays.stream(clazz.getMethods())
                                .filter(m -> m.getAnnotation(CUMethod.class) != null)
                                .map(m -> m.getAnnotation(CUMethod.class).functionName())
                                .toArray(String[]::new)));
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (clazz.isInterface())
            return (R) Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[]{clazz},
                    new KernelInvoker(kernel));
        else {
            try {
                KernelInterceptor knlIcr = new KernelInterceptor(kernel);
                return new ByteBuddy().subclass(clazz)
                        .method(ElementMatchers.isAbstract())
                        .intercept(MethodDelegation.to(knlIcr))
                        .make()
                        .load(clazz.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("cant make instance. " + e);
            }
        }
    }

    public static CUDAImageKernel useImage(String ptxName, Set<String> functionNames) {
        return new CUDAImageKernel(ptxName, functionNames);
    }

    public static final class KernelInterceptor {
        private final CUDAKernel cuKernel;

        // dont use
        private KernelInterceptor() {this.cuKernel = null;}

        private KernelInterceptor(CUDAKernel  cuKernel) {
            this.cuKernel = cuKernel;
        }


        @RuntimeType
        public Object intercept(@Origin Method method, @AllArguments Object...args) {
            method.setAccessible(true);
            if (method.getName().equals("close")) {
                try {
                    cuKernel.close();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            CUMethod cuMethod = method.getAnnotation(CUMethod.class);
            method.setAccessible(true);
            if (cuMethod == null)
                return null;

            return executeAbstract(cuMethod, method, args, cuKernel);
        }
    }

    public static class KernelInvoker implements InvocationHandler {
        final CUDAKernel cuHeader;

        // dont use
        private KernelInvoker() {cuHeader = null;}

        private KernelInvoker(CUDAKernel cuKernel) {
            this.cuHeader = cuKernel;
        }

        @Override
        @Nullable
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            CUMethod cuMethod = method.getAnnotation(CUMethod.class);
            method.setAccessible(true);

            if (method.getName().equals("close"))
                cuHeader.close();

            if (cuMethod == null)
                return method.invoke(proxy, args);

            return CUDAKernels.executeAbstract(cuMethod, method, args, cuHeader);
        }
    }

    private static CUdeviceptr[] executeAbstract(CUMethod cuMethod, Method method, Object[] args, CUDAKernel cuHeader) {
        if (cuMethod.args().length != args.length)
            throw new IllegalArgumentException("unmatched args count. require " + cuMethod.args().length + ". result " + args.length);

        CULaunchKernelOption lkOp = method.getAnnotation(CULaunchKernelOption.class);
        int max = max(max(lkOp.w(), lkOp.h()), lkOp.d());
        int min = min(min(lkOp.w(), lkOp.h()), lkOp.d());
        int mid = lkOp.w() + lkOp.h() + lkOp.d() - min - max;

        Object[] dest;

        if (cuMethod instanceof CUDAImageKernel imageKernel) {
            dest = new Object[args.length - 2];
            System.arraycopy(args, 2, dest, 0, dest.length);
            return imageKernel.invokeImage(cuMethod.functionName(), imageKernel.image.w, imageKernel.image.h, dest);
        } else {
            if (max == USER_SETTING) {
                dest = new Object[args.length - 3];
                max = (int) args[0];
                mid = (int) args[0];
                min = (int) args[0];
                System.arraycopy(args, 3, dest, 0, dest.length);
                return cuHeader.invokeXd(cuMethod.functionName(), max, mid, min, lkOp.blockSize(), dest);
            } else if (mid == USER_SETTING) {
                dest = new Object[args.length - 2];
                max = (int) args[0];
                mid = (int) args[0];
                min = DEFAULT;
                System.arraycopy(args, 2, dest, 0, dest.length);
                return cuHeader.invokeXd(cuMethod.functionName(), max, mid, min, lkOp.blockSize(), dest);
            } else if (min == USER_SETTING) {
                dest = new Object[args.length - 1];
                max = (int) args[0];
                mid = DEFAULT;
                min = DEFAULT;
                System.arraycopy(args, 1, dest, 0, dest.length);
                return cuHeader.invokeXd(cuMethod.functionName(), max, mid, min, lkOp.blockSize(), dest);
            } else {
                return cuHeader.invokeXd(cuMethod.functionName(), max, mid, min, lkOp.blockSize(), args);
            }
        }
    }

    public static CUDAKernel use(String fname, KernelTypes kernelTypes, Set<String> functionNames) {
        return switch (kernelTypes) {
            case NORMAL -> new CUDAKernelImpl(fname, functionNames);
            case MANUAL_MEM_FREE -> new ManualMemFreeKernel(fname, functionNames) {};
        };
    }

    public static CUDAKernel use1d(String fname, Set<String> functionNames) {
        return new CUDAKernelImpl(fname, functionNames) {
            @Override
            public CUdeviceptr[] invoke2d(String function, int w, int h, Object... args) {throw unsupportedDimension();}
            @Override
            public CUdeviceptr[] invoke3d(String function, int w, int h, int d, Object... args) {throw unsupportedDimension();}
        };
    }

    public static CUDAKernel use2d(String fname, Set<String> functionNames) {
        return new CUDAKernelImpl(fname, functionNames) {
            @Override
            public CUdeviceptr[] invoke(String function, int count, Object... args) {throw unsupportedDimension();}
            @Override
            public CUdeviceptr[] invoke3d(String function, int w, int h, int d, Object... args) {throw unsupportedDimension();}
        };
    }

    public static CUDAKernel use3d(String fname, Set<String> functionNames) {
        return new CUDAKernelImpl(fname, functionNames) {
            @Override
            public CUdeviceptr[] invoke(String function, int count, Object... args) {throw unsupportedDimension();}
            @Override
            public CUdeviceptr[] invoke2d(String function, int w, int h, Object... args) {throw unsupportedDimension();}
        };
    }

    public static CUDAKernel useSrc(String src, KernelTypes kernelTypes, Set<String> functionNames) {
        return CUDAKernelImpl.srcToPtxFile(src, kernelTypes, functionNames);
    }

    public static class CUDAImageKernel extends ManualMemFreeKernel {
        private CUImagePtr image;

        private CUDAImageKernel(String file, Set<String> functoinNames) {
            super(file, functoinNames);
            image = null;
        }

        private CUDAImageKernel(String file, Set<String> functionNames, CUImagePtr canvas) {
            this(file, functionNames);
            image = canvas;
        }

        public void setImage(CUImagePtr image) {
            this.image = image;
        }

        @CUKernelRequire(type = _int)
        @CUKernelRequire(type = _int)
        @CUKernelRequire(type = p_int) // bitmap
        public CUdeviceptr[] invokeImage(String function, Object...args) {
            if (image == null)
                throw new RuntimeException("use setImage.");
            Object[] argsDst = new Object[args.length + 1];
            argsDst[0] = image.bitmap;
            System.arraycopy(args, 0, argsDst, 1, args.length);
            return invoke2d(function, image.w, image.h, argsDst);// TODO
        }

        public void drawLine(int pixX0, int pixY0, int pixX1, int pixY1) {

        }
    }

    // deviceptrのmemFreeを手動で行う
    public /*TODO private*/ static abstract class ManualMemFreeKernel extends CUDAKernelImpl {
        private final List<CUWrapper<?>> reuseWrappers = new ArrayList<>();
        private static final Set<String> utils_cuhFunctionName = Set.of("deva_f", "deva_i");

        public ManualMemFreeKernel(String file, Set<String> functionNames) {
            super(file, Sets.newHashSet(Iterables.concat(functionNames, utils_cuhFunctionName)));
            sendCPU = false;
        }

        public ManualMemFreeKernel(Path path, Set<String> functionNames) {
            super(path, functionNames);
        }

        /**
         * Register the objects that require memFree. If cant register, All objects will be memFree.
         */
        protected final synchronized <T> void registersMemFree(@NotNull CUWrapper<T> reqMemFree) {
            try {
                Collections.addAll(reuseWrappers, reqMemFree);
            } catch (Exception e) {

                throw new RuntimeException("cant register memfree object.");
            }
        }

        @Override
        final CUdeviceptr[] _invokeXd(String function, int w, int h, int d, int bsize, CUdeviceptr[] dargs, Object...args) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof CUWrapper<?> wrapper) {
                    if (wrapper.isReuse() && wrapper.deviceptr == null) {
                        wrapper.deviceptr = cpyPointerHtoD(wrapper.sizebytes(), wrapper.p);
                        registersMemFree(wrapper);
                    }
                    dargs[i] = wrapper.deviceptr;
                }
            }
            return super._invokeXd(function, w, h, d, bsize, dargs, args);
        }

        /**
         * Send pointer device to host.
         * @param pointer not wrapper
         * @param deviceptr in cu deviceptr.
         * @return pointer
         */
        public final Object sendHost(Object pointer, CUdeviceptr deviceptr) {
            cpyPointerDtoH(sizebyte(pointer), pointer, deviceptr);
            return pointer;
        }


        public final <T> T sendHost(CUWrapper<T> pointer) {
            sendHost(pointer.p, pointer.deviceptr);
            return pointer.p;
        }

        @Override
        final boolean isAutoMemFree() {
            return false;
        }

        @Override
        public boolean isAutoSendHost() {
            return false;
        }

        // VRAMでのGCを行う
        @Override
        public void close() {
            for (CUWrapper<?> wrapper : reuseWrappers) {
                if (wrapper.sendToHostWhenClose)
                    sendHost(wrapper);
                cuMemFree(wrapper.deviceptr);
            }
        }
    }
}
