package org.w7ls.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w7ls.common.weapon.AirPlanes;
import org.w7ls.common.weapon.Consumable;
import org.w7ls.common.weapon.Shell;
import org.w7ls.common.utils.MinMidMax;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public sealed class Checkers permits CheckersJC {
    public static double knotPerMps = 2.6854; // wowsKnot / (m/s)
    public static double engineBoostTimeDefault = 20/*seconds*/;
    public static double engineBoostRechargeDefault = 40/*seconds*/;

    private final Set<Shell.ShellType> canGetCitadelDefault = new HashSet<>();
    private Target target = null;
    private final HashMap<Consumable.Name, Boolean> enableConsumable = new HashMap<>(Arrays.stream(Consumable.Name.values())
            .collect(Collectors.toMap(s -> s, s -> false)));

    public Checkers() {
        canGetCitadelDefault.add(Shell.ShellType.AP_BOMB);
        canGetCitadelDefault.add(Shell.ShellType.AP_SHELL);
        canGetCitadelDefault.add(Shell.ShellType.AP_ROCKET);
        canGetCitadelDefault.add(Shell.ShellType.TORPEDO);
    }

    public Checkers(Map<Consumable.Name, Boolean> enable) {
        this(null, enable);
    }

    public Checkers(Target target) {
        this(target, null);
    }

    public Checkers(Target target, Map<Consumable.Name, Boolean> enable) {
        this.target = target;
        enableConsumable.putAll(Objects.requireNonNullElse(enable, enableConsumable));
    }

    public double dpmCV(@NotNull AirPlanes airPlanes, final double targetRange, @Nullable Set<Shell.ShellType> canGetCitadel) {
        double compTime = cvAtkCompTime(airPlanes, targetRange);
        double atkingRateM = 60 / (compTime +
                             (airPlanes.isTactical() ? max(airPlanes.restorationTime() - compTime, 0) : 0));
        return atkingRateM * damagePerWave(airPlanes, canGetCitadel);
    }

    public double[] dpmCVs(@NotNull AirPlanes airPlanes, final double targetRange, final double dh) {
        final int count = (int) (targetRange / dh);
        final double[] res = new double[count];
        for (int i = 0; i < count; i++) res[i] = dpmCV(airPlanes, i * dh, canGetCitadelDefault);
        return res;
    }

    public double damagePerWave(@NotNull AirPlanes airPlanes, @Nullable Set<Shell.ShellType> canGetCitadel) {
        int atkCounts = airPlanes.squadronSize() / airPlanes.platoonSize();
        return atkCounts * damagePerAtk(airPlanes, canGetCitadel);
    }

    public double damagePerAtk(@NotNull AirPlanes airPlanes, @Nullable Set<Shell.ShellType> canGetCitadel) {
        Shell weapon = airPlanes.weapon();
        canGetCitadel = canGetCitadel == null ? canGetCitadelDefault : canGetCitadel;

        double dmg = airPlanes.payload() * weapon.damage() * airPlanes.platoonSize();

        return canGetCitadel.contains(weapon.type()) ? dmg : dmg / 3;
    }

    public double cvAtkCompTime(@NotNull AirPlanes airPlanes, final double targetRange) {
        final double atkingTimeFastest = timeFastestAtking(airPlanes);
        final double arrivalTime = arrivalTime(airPlanes, targetRange);
        return atkingTimeFastest + arrivalTime;
    }

    public static double timeFastestAtking(@NotNull AirPlanes airPlanes) {
        final double atkPreparationTime = airPlanes.atkPreparationTime(); // 攻撃準備時間
        final double atkCoolDown = airPlanes.atkCoolDown(); // 攻撃クールタイム
        final int platoon = airPlanes.platoonSize(); // 小隊機数
        final int companySize = airPlanes.squadronSize(); // 中隊機数

        final int atkCount = companySize / platoon;

        return atkCount * atkPreparationTime + (atkCount - 1) * atkCoolDown;
    }

    public void setTarget(@Nullable Target target) {
        this.target = target;
    }

    public void setEnableConsumable(Consumable.Name name, boolean enable) {
        this.enableConsumable.put(name, enable);
    }

    public double[] arrivalTimes(AirPlanes airPlanes, double distance, double dh/*微小距離*/) {
        final int count = (int) (distance / dh);
        double[] res = new double[count];
        for (int i = 0; i < count; i++)
            res[i] = arrivalTime(airPlanes, dh * i);
        return res;
    }

    public double arrivalTime(@NotNull AirPlanes airPlanes, final double targetRange) {
        final double engineBoostDuration = airPlanes.engineBoostDuration();
        final MinMidMax speed = airPlanes.speed();
        double initialBoostDuration = airPlanes.initialBoostDuration();
        final double initialBoostSpeed = airPlanes.initialBoostSpeed();
        boolean enableEngineCooling = enableConsumable.get(Consumable.Name.ENGINE_COOLING);
        Consumable engineCooling = null;
        if (airPlanes.consumables() != null)
            for (Consumable c : airPlanes.consumables())
                if (c != null && c.name() == Consumable.Name.ENGINE_COOLING) {
                    engineCooling = c;
                    break;
                }
        final double engineBoostRecharge = engineBoostTimeDefault / engineBoostDuration * engineBoostRechargeDefault;

        //
        // エンジンブーストが20%溜まるとブーストを炊く移動方法とする
        // 発艦してからブーストが無くなるまでフルで炊くとする
        //
        final double dt = 0.01; // timeの微小区間(second)
        final double engineBoostReuse = 0.2 * engineBoostDuration; // ここまで溜まったらエンジンブーストを再度炊く
        double nowRange = 0;
        double nowSpeed;
        double boostingTime = engineBoostDuration;
        final double rechargeRate = engineBoostDuration / engineBoostRecharge; // エンジンブーストの秒間回復(second)
        double time = 0;
        boolean boostState = true; // true...boost, false...boost recharging
        for (;nowRange < targetRange; time += dt) {
            if (boostState) {
                nowSpeed = speed.max();
                checkBoost : if ((boostingTime -= dt) <= 0) {
                    if (enableEngineCooling) {
                        boostingTime += engineCooling.validityTime() + engineBoostDuration;
                        enableEngineCooling = false;
                        break checkBoost;
                    }
                    boostState = false;
                }
            } else {
                nowSpeed = speed.mid();
                if ((boostingTime += (dt * rechargeRate)) >= engineBoostReuse) boostState = true;
            }

            if ((initialBoostDuration -= dt) > 0) {
                nowSpeed *= initialBoostSpeed;
            }
            nowRange += nowSpeed * dt * knotPerMps / 1000; // km
        }

        return time;
    }
}
