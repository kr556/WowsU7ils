package org.w7ls.common.weapon;

import org.w7ls.common.utils.MinMax;

public record Dispersal(
        double atkInterval,// 弾体発射間隔(s)
        double atkCount,   // 弾数/機
        MinMax salvoSizeX,  // 最小散布界-最大散布界x
        MinMax salvoSizeY,  // 最小散布界-最大散布界y
        MinMax spread) {
}
