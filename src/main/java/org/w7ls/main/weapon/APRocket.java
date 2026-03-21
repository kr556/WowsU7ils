package org.w7ls.main.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.utils.MinMax;

public class APRocket implements Shell {
    private final double penetration;
    private final int damage;
    private final MinMax ricochetAngle;

    public APRocket(double penetration, int damage, MinMax ricochetAngle) {
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
        return ShellType.AP_ROCKET;
    }

    @Override
    public MinMax ricochetAngle() {
        return ricochetAngle;
    }
}
