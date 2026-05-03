package org.w7ls.annotation.cuda;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//メソッドをcudaで実装することを表す
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUHeader {
    String cudaCode() default "";

    HeaderFileType codeType();

    KernelType kernelType() default KernelType.COMMON;

    // type of cuda code.
    enum HeaderFileType {
        // compiled ptx file
        PTX_FILE,
        // non compiled cuda src file
        CU_SRC_FILE,
        // cudaCode is src code
        SRC_CODE
    }

    enum KernelType {
        COMMON,
        IMAGE
    }
}

