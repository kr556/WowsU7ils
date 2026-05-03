package org.w7ls.annotation.cuda;

import java.lang.annotation.*;

// カーネル側で必ず必要な引数を表す
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CUKernelRequiredContainer.class)
public @interface CUKernelRequire {
    CUTypes type();
    String name() default "";
}
