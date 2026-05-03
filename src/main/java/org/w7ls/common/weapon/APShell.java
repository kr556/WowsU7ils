package org.w7ls.common.weapon;

import org.jetbrains.annotations.NotNull;
import org.w7ls.common.utils.MinMax;

public class APShell implements Shell {
    private final double krupp;
    private final double firstVelocity;
    private final int damage;
    private final double overmatch;
    private final double mass;
    private final MinMax ricochetAngle;
    private final double bulletDrag;

    public APShell( int damage, double krupp, double velocity, double overmatch, double mass, MinMax ricochetAngle, double bulletDrag) {
        this.krupp = krupp;
        this.firstVelocity = velocity;
        this.damage = damage;
        this.overmatch = overmatch;
        this.mass = mass;
        this.ricochetAngle = ricochetAngle;
        this.bulletDrag = bulletDrag;
    }

    @Override
    public double penetration(double distanceKM) {
        return 0; // TODO
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
        return ShellType.AP_SHELL;
    }

    @Override
    public double overmatch() {
        return overmatch;
    }

    @Override
    public MinMax ricochetAngle() {
        return ricochetAngle;
    }
}
