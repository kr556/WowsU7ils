package org.w7ls.vmeditor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// ローカルで宣言して外部から参照できないオブジェクトの判別に使う．メンバを減らすため
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetName {
    String name();
}
