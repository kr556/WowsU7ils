package org.w7ls.main.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TorpedoTest {
    @Test
    void ricochetTest() {
        Torpedo torp = new Torpedo(10000, 1);
        for (int i = 0; i < 90; i++) {
            System.out.println(torp.ricochet(i));
        }
    }
}