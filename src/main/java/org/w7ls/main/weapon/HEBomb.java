package org.w7ls.main.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.utils.MinMax;

public class HEBomb implements Shell {
    private static final MinMax heBombRicochetAngle = new MinMax(90, 90);
    private final double penetration;
    private final double fireRate;
    private final int damage;

    public HEBomb(double penetration, double fireRate, int damage) {
        this.penetration = penetration;
        this.fireRate = fireRate;
        this.damage = damage;
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
        return fireRate;
    }

    @Override
    public double floodingRate() {
        return 0;
    }

    @Override
    public @NotNull ShellType type() {
        return ShellType.HE_BOMB;
    }

    @Override
    public MinMax ricochetAngle() {
        return noRecochetShellAngle;
    }
}
