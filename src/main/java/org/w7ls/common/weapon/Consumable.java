package org.w7ls.common.weapon;

import java.io.Serializable;

public interface Consumable extends Serializable {
    double coolDownTime();
    double validityTime();
    Name name();

    enum Name {
        ENGINE_COOLING,
        ;

        public Consumable create(double coolDownTime, double validityTime) {
            return new Consumable() {
                @Override
                public double coolDownTime() {
                    return coolDownTime;
                }

                @Override
                public double validityTime() {
                    return validityTime;
                }

                @Override
                public Name name() {
                    return Name.this;
                }
            };
        }
    }
}
