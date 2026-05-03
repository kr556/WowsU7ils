package org.w7ls.annotation.cuda;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CUKernelRequiredContainer {
    CUKernelRequire[] value();
}
