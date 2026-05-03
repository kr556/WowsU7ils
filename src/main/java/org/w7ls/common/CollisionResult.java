package org.w7ls.common;

public enum CollisionResult {
    OVER_PEN(0.1,       0b100000),
    PENETRATION(1d/3,   0b010000),
    CITADEL(1,          0b001000),
    NO_PEN(0,           0b000100),
    RICOCHET(0,         0b000010),
    NO_HIT(0,           0b000001);

    public final double damage;
    public final int frag;

    CollisionResult(double damage, int frag) {
        this.damage = damage;
        this.frag = frag;
    }
}
