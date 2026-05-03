package org.w7ls.annotation;

import java.lang.annotation.*;

// メソッドが返しうる値の範囲
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
@Documented
public @interface ValueRange {
    double min() default 0;
    double max() default 0;
    String description() default ""; // レコードの引数に使用する場合のみ使ってください
}
