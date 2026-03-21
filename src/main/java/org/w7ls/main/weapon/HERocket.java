package org.w7ls.main.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.utils.MinMax;

public class HERocket implements Shell {
    private final double penetration;
    private final int damage;
    private final double fireRate;

    public HERocket(double penetration, int damage, double fireRate) {
        this.penetration = penetration;
        this.damage = damage;
        this.fireRate = fireRate;
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
        return ShellType.HE_ROCKET;
    }

    @Override
    public MinMax ricochetAngle() {
        return noRecochetShellAngle;
    }
}
