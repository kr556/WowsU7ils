package org.w7ls.annotation.cuda;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CULaunchKernelOption {
    int blockSize() default 16;
    int w() default DEFAULT;
    int h() default DEFAULT;
    int d() default DEFAULT;

    int USER_SETTING = Integer.MIN_VALUE;
    int DEFAULT = 1;
}
