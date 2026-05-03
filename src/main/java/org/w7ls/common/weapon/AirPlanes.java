package org.w7ls.common.weapon;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w7ls.annotation.ValueRange;
import org.w7ls.common.utils.MinMidMax;

public record AirPlanes(
        String name,
        @ValueRange(min = -1, max = Integer.MAX_VALUE)
        int squadronSize, // 機数
        int platoonSize, // 機数
        int hp,
        double restorationTime,
        double engineBoostDuration,
        double initialBoostDuration,
        double initialBoostSpeed,

        @ValueRange(min = 0, max = 1)
        double resDamageTakeAttacking,
        int payload,
        double atkPreparationTime,
        double atkCoolDown,
        @NotNull MinMidMax speed,
        @NotNull Shell weapon,
        @Nullable Consumable[] consumables,
        boolean isTactical,
        @Nullable Dispersal dispersal
) implements Weapon {
        public enum Type {
                FIGHTER,
                TORPEDO_BOMBER,
                DIVE_BOMBER,
        }

        public static AirPlanes map(JsonNode ship, Type type) { // TODO
                JsonNode prof = ship.get("default_profile");
//                int _platoonSize = ship.get(prof.get("torpedo_bomber").get());

                return null; // TODO
        }
}
