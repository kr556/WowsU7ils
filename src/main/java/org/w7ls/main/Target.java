package org.w7ls.main;

import org.w7ls.main.weapon.Dispersal;

public class Target {
    private final TargetCollision collision;
    private final int countX; // 精度
    private final int countY; // 精度

    public Target(TargetCollision collision) {
        this.countX = 100;
        this.countY = 100;
        this.collision = collision;
    }

    public Target(TargetCollision collision, int countX, int countY) {
        this.collision = collision;
        this.countX = countX;
        this.countY = countY;
    }

    /**
     * 渡された散布に対して，ダメージの期待値を返します
     */
    public double result(Dispersal dispersal) {
        return 0;
    }

    // フラグを返すようにしてください
    public interface TargetCollision {
        int apply(double px, double py);
    }
}
