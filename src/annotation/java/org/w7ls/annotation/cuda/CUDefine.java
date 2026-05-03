package org.w7ls.annotation.cuda;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// javaのインスタンスをcudaで実装する際，cuda側でdefineとして使用する
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUDefine {
    String fname = "bin/cuda/globals.cuh";
}
