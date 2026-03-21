package org.w7ls.main;

import org.jetbrains.annotations.NotNull;
import org.w7ls.main.weapon.AirPlanes;
import org.w7ls.main.weapon.Consumable;

import java.util.HashMap;
import java.util.Set;

/**
 * Checkersの一部メソッドをCUDAで高速化してます
 * @see CheckersJC#arrivalTimes
 * @see CheckersJC#dpmCVs(AirPlanes, double, double)
 */
public final class CheckersJC extends Checkers {
    final HashMap<String, CUDAHeader> gpgpus = new HashMap<>();
    final String chckersJC = "checkers";

    public CheckersJC() {
        gpgpus.put(chckersJC, new CUDAHeader(chckersJC, Set.of("arrival_time", "dpm_cv", "dpm_cv_tac")));
    }

    public void setParallel(boolean parallel) {
        gpgpus.get(chckersJC).setParallel(parallel);
    }

    @Override
    public double[] dpmCVs(@NotNull AirPlanes airPlanes, double targetRange, double dh) {
        final int count = (int) (targetRange / dh);
        float engineBoostDuration = (float) airPlanes.engineBoostDuration();
        float initialBoostDuration = (float) airPlanes.initialBoostDuration();
        float initialBoostSpeed = (float) airPlanes.initialBoostSpeed();
        float speedMin = (float) airPlanes.speed().min();
        float speedMid = (float) airPlanes.speed().mid();
        float speedMax = (float) airPlanes.speed().max();
        Consumable consumable = airPlanes.consumables()[0];
        float engineCoolingDuration = (float) consumable.validityTime();
        float engineCoolingCooltime = (float) consumable.coolDownTime();

        double[] pointer = new double[count];

        Object[] arg = new Object[]{
                pointer,
                engineBoostDuration,
                initialBoostDuration,
                initialBoostSpeed,
                speedMin,
                speedMid,
                speedMax,
                engineCoolingDuration,
                engineCoolingCooltime,
                (float) dh,

                (float) timeFastestAtking(airPlanes),
                (float) damagePerWave(airPlanes, null),
                (float) airPlanes.restorationTime()

        };
        CUDAHeader func = gpgpus.get(chckersJC);
        if (airPlanes.isTactical())
            func.invoke("dpm_cv_tac", count, arg);
        else
            func.invoke("dpm_cv", count, arg);
        return pointer;
    }

    @Override
    public double[] arrivalTimes(AirPlanes airPlanes, double distance, double dh/*微小距離*/) {
        final int count = (int) (distance / dh);
        final float engineBoostDuration = (float)airPlanes.engineBoostDuration();
        final float initialBoostDuration = (float)airPlanes.initialBoostDuration();
        final float initialBoostSpeed = (float)airPlanes.initialBoostSpeed();
        final float speedMin = (float)airPlanes.speed().min();
        final float speedMid = (float)airPlanes.speed().mid();
        final float speedMax = (float)airPlanes.speed().max();
        final Consumable consumable = airPlanes.consumables()[0];
        final float engineCoolingDuration = (float)consumable.validityTime();
        final float engineCoolingCooltime = (float)consumable.coolDownTime();
        final double[] pointer = new double[count];
        gpgpus.get(chckersJC).invoke("arrival_time", count, pointer,
                engineBoostDuration,
                initialBoostDuration,
                initialBoostSpeed,
                speedMin,
                speedMid,
                speedMax,
                engineCoolingDuration,
                engineCoolingCooltime,
                dh);

        return pointer;
    }
}
