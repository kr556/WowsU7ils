package org.w7ls.common.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.common.utils.MinMax;

public class HEShell implements Shell {
    private final double penetration;
    private final double fireRate;
    private final int damage;

    public HEShell(double penetration, double fireRate, int damage) {
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
        return ShellType.HE_SHELL;
    }

    @Override
    public MinMax ricochetAngle() {
        return noRecochetShellAngle;
    }
}
