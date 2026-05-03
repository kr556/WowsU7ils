package org.w7ls.common.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.common.utils.MinMax;

public class APBomb implements Shell {
    private final double penetration;
    private final int damage;
    private final MinMax ricochetAngle;

    public APBomb(double penetration, int damage, MinMax ricochetAngle) {
        this.penetration = penetration;
        this.damage = damage;
        this.ricochetAngle = ricochetAngle;
    }

    @Override
    public double penetration(double distance) {
        return penetration;
    }

    @Override
    public int damage() {
        return damage;
    }

    @Override
    public double fireRate() {
        return 0;
    }

    @Override
    public double floodingRate() {
        return 0;
    }

    @Override
    public @NotNull ShellType type() {
        return ShellType.AP_BOMB;
    }

    @Override
    public MinMax ricochetAngle() {
        return ricochetAngle;
    }
}
