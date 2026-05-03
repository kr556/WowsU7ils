package org.w7ls.weapon;

import org.junit.jupiter.api.Test;
import org.w7ls.common.weapon.Torpedo;

class TorpedoTest {
    @Test
    void ricochetTest() {
        Torpedo torp = new Torpedo(10000, 1);
        for (int i = 0; i < 90; i++) {
            System.out.println(torp.ricochet(i));
        }
    }
}