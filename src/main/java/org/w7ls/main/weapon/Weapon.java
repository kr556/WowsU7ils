package org.w7ls.main.weapon;

import java.io.Serializable;

public interface Weapon extends Serializable {
    enum Type {
        MAIN_BATTERY,
        SUB_BATTERY,

        AIE_PLANE,

        ARMOR,
        ;
    }
}
