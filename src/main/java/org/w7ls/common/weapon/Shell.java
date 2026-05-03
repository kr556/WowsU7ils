package org.w7ls.common.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.annotation.ValueRange;
import org.w7ls.common.utils.MinMax;

import java.io.Serializable;
import java.util.Random;

// 魚雷や爆弾も含む
public interface Shell extends Serializable {
    MinMax noRecochetShellAngle = new MinMax(90, 90);
    @ValueRange(min = -1)
    double penetration(double distance);
    int damage();
    @ValueRange(min = 0, max = 1)
    double fireRate();
    @ValueRange(min = 0, max = 1)
    double floodingRate();
    @NotNull
    ShellType type();

    // 0 ~ min : 貫通
    // min ~ max : 線形確率跳弾
    // max ~ 90 跳弾
    @ValueRange(min = 0, max = 90)
    MinMax ricochetAngle();

    default boolean ricochet(double landingAngleDegrees) {
        MinMax ricochetAngle = ricochetAngle();
        if (landingAngleDegrees <= ricochetAngle.min())
            return false;
        else if (ricochetAngle.max() <= landingAngleDegrees)
            return true;
        else {
            return Statics.random.nextDouble() <
                   (landingAngleDegrees - ricochetAngle.min()) /
                   (ricochetAngle.max() - ricochetAngle.min());
        }
    }

    default double overmatch() {
        return Double.MAX_VALUE;
    }

    enum ShellType {
        HE_SHELL,
        SAP_SHELL,
        AP_SHELL,

        HE_ROCKET,
        AP_ROCKET,
        HE_BOMB,
        AP_BOMB,

        TORPEDO,
    }
}
class Statics {
    static Random random = new Random();
}
