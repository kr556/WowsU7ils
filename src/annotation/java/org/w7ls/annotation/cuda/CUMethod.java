package org.w7ls.annotation.cuda;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// cudaで実装することを表す
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUMethod {
    String functionName();
    CUTypes[] args() default {};
}
