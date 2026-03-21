package org.w7ls.main;

import org.w7ls.main.weapon.Weapon;

import java.util.HashMap;

public final class Ship {
    private final HashMap<Weapon.Type, Weapon[]> weapons = new HashMap<>();

    public Ship() {}

    public Weapon[] getWeapons(Weapon.Type type) {
        return weapons.get(type);
    }
}
