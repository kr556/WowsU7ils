package org.w7ls.main.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.utils.MinMax;

public class Torpedo implements Shell {
    private final int damage;
    private final double floodingRate;

    public Torpedo(int damage, double floodingRate) {
        this.damage = damage;
        this.floodingRate = floodingRate;
    }

    @Override
    public double penetration(double distance) {
        return Double.MAX_VALUE;
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
        return floodingRate;
    }

    @Override
    public @NotNull ShellType type() {
        return ShellType.TORPEDO;
    }

    @Override
    public MinMax ricochetAngle() {
        return noRecochetShellAngle;
    }
}
