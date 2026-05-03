package org.w7ls.common.cuda;

import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w7ls.annotation.cuda.CUTypes;

import java.util.Objects;
import java.util.function.Function;

// kernelの引数に渡すオブジェクト
public class CUWrapper<T> implements CUReusable {
    final T p;
    private final @Nullable CUTypes types;
    private final boolean reuse;
    static final Function<Object, RuntimeException> noSupportTypeArg = o -> new IllegalArgumentException(" the type is no supported : " + o);
    transient CUdeviceptr deviceptr;
    private transient boolean using;
    private transient boolean used;
    transient boolean sendToHostWhenClose;

    public static <T> @NotNull CUWrapper<T> to(T obj) {
        return new CUWrapper<>(obj, false);
    }

    public static <T> @NotNull CUWrapper<T> toReuse(T obj) {
        return new CUWrapper<>(obj, true);
    }

    protected CUWrapper(T p, boolean reuse) {
        this.p = p;
        this.types = Objects.requireNonNullElseGet(CUTypes.toC(p.getClass()), () -> {throw noSupportTypeArg.apply(p.getClass().getTypeName());});
        this.reuse = reuse;
    }

    public final CUdeviceptr getDeviceptr() {
        return deviceptr;
    }

    public final CUTypes getCUType() {
        return types;
    }

    public int sizebytes() {
        return sizebyte(p);
    }

    static int sizebyte(Object arg) {
        if (arg.getClass().isArray())
            if (arg instanceof float[] arg_)
                return Sizeof.FLOAT * arg_.length;
            else if (arg instanceof int[] arg_)
                return Sizeof.INT * arg_.length;
            else if (arg instanceof double[] arg_)
                return Sizeof.DOUBLE * arg_.length;
            else if (arg instanceof byte[] arg_)
                return Sizeof.BYTE * arg_.length;
            else if (arg instanceof long[] arg_)
                return Sizeof.LONG * arg_.length;
            else if (arg instanceof short[] arg_)
                return Sizeof.SHORT * arg_.length;
            else if (arg instanceof char[] arg_)
                return Sizeof.INT * arg_.length;
            else throw CUWrapper.noSupportTypeArg.apply(arg);
        else if (arg instanceof CUWrapper<?> wrapper)
            return sizebyte(wrapper.p);
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
        else throw CUWrapper.noSupportTypeArg.apply(arg);
    }

    public T getPointer() {
        return p;
    }

    @Override
    public boolean isReuse() {
        return reuse;
    }

    @Override
    public boolean using() {
        boolean using_ = using;
        using = true;
        used = false;
        return using_;
    }

    @Override
    public void used() {
        if (using) {
            using = false;
            used = true;
        }
        throw new RuntimeException("did not used.");
    }

    public void setSendToHostWhenClose(boolean set) {
        this.sendToHostWhenClose = set;
    }
}
