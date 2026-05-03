import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w7ls.common.Checkers;
import org.w7ls.common.weapon.*;
import org.w7ls.common.utils.MinMax;
import org.w7ls.common.utils.MinMidMax;
import org.w7ls.common.utils.StringTmp;
import org.w7ls.vme.VMAudioFile;

import java.io.*;
import java.util.*;

import static org.w7ls.common.weapon.Consumable.Name.ENGINE_COOLING;
import static org.w7ls.common.utils.StringTmp.str;
import static org.w7ls.common.weapon.Shell.ShellType.*;

public class  Test {
    private static HashMap<String, AirPlanes> airplanesMap; // key...<enName>_<weaponName>_<N>

    public static void main(String[] args) throws IOException {
        VMAudioFile audioFile = new VMAudioFile(new File("C:/Games/wows_my_mod_installer/Genshin_voiceDownLoader/voices/kinich_voices/1204_jp.ogg"));
        audioFile.trim(5, 10);
        audioFile.startTrimed();

//        initAirplanes();
//
//        printToFile("results/Test.csv", getCSV(30, 0.1, (s, a) -> (a.weapon() instanceof HEBomb || a.weapon() instanceof APBomb) && !a.isTactical()));
    }

    // 敵艦までの距離，微小距離，計算する航空機の条件
    public static String getCSV(double distMax, double dh, Match dataType) {
        List<String[]> keyList = new ArrayList<>(airplanesMap.keySet()).stream()
                .map(s -> s.split("_"))
                .toList();
        List<AirPlanes> valueList = new ArrayList<>(airplanesMap.values());

        Checkers checker = new Checkers();

        StringBuilder matchPlanesDPM = new StringBuilder();
        matchPlanesDPM.append("dist, ");
        for (double dist = 0; dist < distMax; dist+=dh)
            matchPlanesDPM.append(String.format("%.2f, ", dist));
        matchPlanesDPM.append("\n");
        for (int i = 0; i < keyList.size(); i++) {
            if (dataType.apply(keyList.get(i), valueList.get(i))) {
                matchPlanesDPM
                        .append(str((Object[]) keyList.get(i)))
                        .append(", ");
                for (double dist = 0; dist < distMax; dist+=dh, matchPlanesDPM.append(", "))
                    matchPlanesDPM
                            .append(String.format("%.2f", checker.dpmCV(valueList.get(i), dist, null)));
                matchPlanesDPM.append("\n");
            }
        }

        return transpose(matchPlanesDPM.toString());
    }

    // チャッピーに書かせたやつ
    public static String transpose(String csv) {
        String[] rows = csv.split("\\R");

        List<String[]> table = new ArrayList<>();
        int maxCols = 0;

        for (String row : rows) {
            String[] cols = row.split(",", -1);
            table.add(cols);
            maxCols = Math.max(maxCols, cols.length);
        }

        int rowCount = table.size();
        StringBuilder result = new StringBuilder();

        for (int c = 0; c < maxCols; c++) {
            for (int r = 0; r < rowCount; r++) {
                String[] row = table.get(r);
                if (c < row.length) {
                    result.append(row[c]);
                }
                if (r < rowCount - 1) {
                    result.append(",");
                }
            }
            if (c < maxCols - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    public static void printToFile(String fname, String s) {
        PrintStream logPrinter;
        try {
            logPrinter = new PrintStream(new FileOutputStream(fname));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        logPrinter.print(s);
    }

    public static MinMidMax defSpeed(double midSpeed) {
        return new MinMidMax(midSpeed - 30, midSpeed, midSpeed + 40);
    }

    public static @NotNull AirPlanes planes(String name, MinMidMax speed, int squadronSize, int platoonSize, double engineBoostDuration, double atkPreparationTime, double atkCoolDown, int payload, Shell weapon, @Nullable Consumable engineCooling, Dispersal dispersal) {
        return planes(name, speed, squadronSize, platoonSize, engineBoostDuration, 0, 0, atkPreparationTime, atkCoolDown, payload, weapon, engineCooling, dispersal);
    }

    public static @NotNull AirPlanes planes(String name, MinMidMax speed, int squadronSize, int platoonSize, double engineBoostDuration, double atkPreparationTime, double atkCoolDown, int payload, Shell weapon, @Nullable Consumable engineCooling, double restorationTime, Dispersal dispersal) {
        return planes(name, speed, squadronSize, platoonSize, engineBoostDuration, 0, 0, atkPreparationTime, atkCoolDown, payload, weapon, engineCooling, restorationTime, dispersal);
    }

    public static AirPlanes planes(String name, MinMidMax speed, int squadronSize, int platoonSize, double engineBoostDuration, double initialBoostDuration, double initialBoostSpeed, double atkPreparationTime, double atkCoolDown, int payload, Shell weapon, @Nullable Consumable engineCooling, Dispersal dispersal) {
        return planes(name, speed, squadronSize, platoonSize, engineBoostDuration, initialBoostDuration, initialBoostSpeed, atkPreparationTime, atkCoolDown, payload, weapon, engineCooling, 0, dispersal);
    }

    public static AirPlanes planes(String name, MinMidMax speed, int squadronSize, int platoonSize, double engineBoostDuration, double initialBoostDuration, double initialBoostSpeed, double atkPreparationTime, double atkCoolDown, int payload, Shell weapon, @Nullable Consumable engineCooling, double restorationTime, Dispersal dispersal) {
        return new AirPlanes(
                name,
                squadronSize,
                platoonSize,
                0,
                restorationTime,
                engineBoostDuration,
                initialBoostDuration,
                initialBoostSpeed,
                0,
                payload,
                atkPreparationTime,
                atkCoolDown,
                speed,
                weapon,
                new Consumable[]{engineCooling},
                restorationTime != 0,
                null
        );
    }

    public static String stra(String name, Shell.ShellType type) {
        return stra(name, type, 0).replace("_0", "");
    }

    public static String stra(String name, Shell.ShellType type, int n) {
        return StringTmp.str(name,"_",toShellKey(type),"_",n);
    }

    public static String toShellKey(Shell.ShellType type) {
        StringBuilder sb = new StringBuilder();
        String[] sn = type.name().toLowerCase().split("_");
        for (String s : sn) sb.append(s.replaceFirst(".", String.valueOf(s.charAt(0)).toUpperCase()));
        return sb.toString();
    }

    public static HashMap<String, AirPlanes> initAirplanes() {
        airplanesMap = new HashMap<>();
        String hakuryu = "Hakuryu";
        String admiralNakhimov = "AdmiralNakhimov";
        String audacious = "Audacious";
        String essex = "Essex";
        String fdr = "FDR";
        String midway = "Midway";
        String independencia = "Independencia";
        String malta = "Malta";
        String mvr = "MVR";
        String maxImmelmann = "MaxImmelmann";
        String ocean = "Ocean";
        String shinano = "Shinano";

        String shokaku = "Shokaku";

        Consumable dCooling = ENGINE_COOLING.create(80, 5);
        Consumable germCooling = ENGINE_COOLING.create(80, 10);
        Consumable shinanoCooling = ENGINE_COOLING.create(80, 15);
        MinMax dRcAngle = new MinMax(45, 60);

        airplanesMap.put(stra(hakuryu, HE_ROCKET), planes(hakuryu, defSpeed(165), 9, 3, 5.0, 2.0, 5.0, 6, new HERocket(30, 3150, 0.14), dCooling, null));
        airplanesMap.put(stra(hakuryu, TORPEDO), planes(hakuryu, defSpeed(145), 12, 2, 20.0, 4.0, 5.0, 1, new Torpedo(9333, 0.4), dCooling, null));
        airplanesMap.put(stra(hakuryu, AP_BOMB), planes(hakuryu, defSpeed(152), 12, 3, 20.0, 3.0, 9.0, 1, new APBomb(351, 6800, dRcAngle), dCooling, null));

        airplanesMap.put(stra(admiralNakhimov, HE_ROCKET), planes(admiralNakhimov, defSpeed(168), 8, 8, 5.0, 5.0, 1.2, 2.2, 5.0, 4, new HERocket(40, 4050, 0.22), dCooling, null));
        airplanesMap.put(stra(admiralNakhimov, TORPEDO), planes(admiralNakhimov, new MinMidMax(96, 124, 162), 7, 7, 20.0, 10.0, 1.5, 4.0, 5.0, 1, new Torpedo(5200, 0.45), dCooling, null));
        airplanesMap.put(stra(admiralNakhimov, HE_BOMB), planes(admiralNakhimov, new MinMidMax(98, 125, 162), 7, 7, 20.0, 10.0, 1.5, 4.0, 9.0, 1, new HEBomb(52, 0.49, 8700), dCooling, null));

        airplanesMap.put(stra(audacious, HE_ROCKET), planes(audacious, defSpeed(140), 9, 3, 5.0, 3.0, 5.0, 12, new HERocket(28, 2350, 0.09), dCooling, null));
        airplanesMap.put(stra(audacious, TORPEDO), planes(audacious, new MinMidMax(116, 142, 177), 9, 3, 20.0, 4.0, 5.0, 1, new Torpedo(5933, 0.51), dCooling, null));
        airplanesMap.put(stra(audacious, HE_BOMB), planes(audacious, new MinMidMax(116, 142, 177), 9, 3, 20.0, 3.0, 8.5, 1, new HEBomb(41, 0.36, 6400), dCooling, null));

        airplanesMap.put(stra(essex, HE_ROCKET), planes(essex, defSpeed(176), 6, 6, 5.0, 15.0, 1.2, 3.0, 5.0, 2, new HERocket(68, 5400, 0.33), dCooling, 126, null));
        airplanesMap.put(stra(essex, HE_BOMB), planes(essex, defSpeed(130), 6, 6, 20.0, 15.0, 1.25, 2.5, 9.0, 4, new HEBomb(42, 0.31, 5400), dCooling, 144, null));
        airplanesMap.put(stra(essex, TORPEDO), planes(essex, new MinMidMax(105, 131, 166), 8, 2, 20.0, 4.0, 5.0, 2, new Torpedo(6433, 0.53), dCooling, null));

        airplanesMap.put(stra(fdr, HE_ROCKET), planes(fdr, defSpeed(124), 14, 2, 5.0, 3.0, 25.0, 26, new HERocket(32, 1650, 0.05), dCooling, null));
        airplanesMap.put(stra(fdr, TORPEDO), planes(fdr, new MinMidMax(93, 119, 154), 14, 2, 20.0, 4.0, 25.0, 4, new Torpedo(4233, 0.33), dCooling, null));
        airplanesMap.put(stra(fdr, HE_BOMB), planes(fdr, new MinMidMax(93, 119, 154), 14, 2, 20.0, 3.0, 25.0, 4, new HEBomb(63, 0.6, 10600), dCooling, null));

        airplanesMap.put(stra(independencia, HE_ROCKET, 1), planes(independencia, defSpeed(176), 10, 5, 5.0, 2.2, 5.0, 3, new HERocket(68, 4600, 0.26), dCooling, null));
        airplanesMap.put(stra(independencia, HE_ROCKET, 2), planes(independencia, defSpeed(176), 7, 7, 5.0, 3.0, 5.0, 8, new HERocket(27, 1900, 0.07), dCooling, 90, null));
        airplanesMap.put(stra(independencia, TORPEDO), planes(independencia, new MinMidMax(95, 119, 154), 12, 4, 20.0, 4.0, 5.0, 2, new Torpedo(4533, 0.39), dCooling, null));

        airplanesMap.put(stra(malta, HE_ROCKET), planes(malta, defSpeed(137), 12, 4, 5.0, 3.0, 5.0, 10, new HERocket(28, 2350, 0.09), dCooling, null));
        airplanesMap.put(stra(malta, TORPEDO), planes(malta, new MinMidMax(110, 136, 171), 12, 4, 20.0, 4.0, 5.0, 1, new Torpedo(6533, 0.56), dCooling, null));
        airplanesMap.put(stra(malta, AP_BOMB), planes(malta, new MinMidMax(109, 134, 169), 12, 4, 20.0, 3.0, 8.5, 3, new APBomb(109, 4300, dRcAngle), dCooling, null));

        airplanesMap.put(stra(mvr, AP_ROCKET), planes(mvr, defSpeed(172), 9, 3, 5.0, 2.5, 5.0, 4, new APRocket(277, 3300, dRcAngle), germCooling, null));
        airplanesMap.put(stra(mvr, TORPEDO), planes(mvr, defSpeed(174), 9, 3, 20.0, 4.0, 5.0, 1, new Torpedo(4533, 0.39), germCooling, null));
        airplanesMap.put(stra(mvr, AP_BOMB), planes(mvr, defSpeed(174), 12, 3, 20.0, 2.5, 9.0, 1, new APBomb(331, 8800, dRcAngle), germCooling, null));

        airplanesMap.put(stra(maxImmelmann, HE_BOMB), planes(maxImmelmann, defSpeed(174), 12, 4, 20.0, 4.0, 9.0, 1, new HEBomb(68, 0.63, 11000), germCooling, null));
        airplanesMap.put(stra(maxImmelmann, TORPEDO), planes(maxImmelmann, defSpeed(154), 12, 4, 20.0, 4.0, 5.0, 1, new Torpedo(4767, 0.27), germCooling, null));

        airplanesMap.put(stra(midway, HE_ROCKET) + "_HVAR", planes(midway, defSpeed(176), 9, 3, 5.0, 3.0, 5.0, 10, new HERocket(33, 2000, 0.07), dCooling, null));
        airplanesMap.put(stra(midway, HE_ROCKET) + "_TinyTim", planes(midway, defSpeed(176), 9, 3, 5.0, 3.0, 5.0, 3, new HERocket(68, 5400, 0.33), dCooling, null));
        airplanesMap.put(stra(midway, TORPEDO), planes(midway, new MinMidMax(105, 131, 166), 9, 3, 20.0, 4.0, 5.0, 2, new Torpedo(5067, 0.42), dCooling, null));
        airplanesMap.put(stra(midway, HE_BOMB), planes(midway, new MinMidMax(105, 131, 166), 12, 3, 20.0, 2.5, 9.0, 2, new HEBomb(67, 0.64, 11200), dCooling, null));

        airplanesMap.put(stra(ocean, HE_ROCKET), planes(ocean, new MinMidMax(161, 161, 322), 3, 3, 30.0, 3.0, 5.0, 8, new HERocket(28, 2650, 0.11), dCooling, 160, null));
        airplanesMap.put(stra(ocean, HE_BOMB), planes(ocean, new MinMidMax(154, 154, 308), 4, 4, 30.0, 3.3, 9.0, 2, new HEBomb(62, 0.52, 9100), dCooling, 180, null));
        airplanesMap.put(stra(ocean, TORPEDO), planes(ocean, new MinMidMax(116, 142, 177), 12, 4, 20.0, 4.0, 5.0, 1, new Torpedo(5567, 0.47), dCooling, null));

        airplanesMap.put(stra(shinano, TORPEDO), planes(shinano, defSpeed(137), 9, 3, 20.0, 4.0, 5.0, 1, new Torpedo(6533, 0.36), shinanoCooling, null));
        airplanesMap.put(stra(shinano, AP_BOMB), planes(shinano, defSpeed(145), 12, 4, 20.0, 4.0, 9.0, 1, new APBomb(658, 6100, dRcAngle), shinanoCooling, null));
        airplanesMap.put(stra(shinano, HE_BOMB), planes(shinano, defSpeed(152), 8, 8, 20, 2.5, 8.5, 1, new HEBomb(55, 0.5, 8800), shinanoCooling, 120, null));

        airplanesMap.put(stra(shokaku,HE_ROCKET), planes(shokaku, defSpeed(151), 9, 3, 5.0, 2.0, 5.0, 6, new HERocket(28, 2200, 0.08), dCooling, null));

//        airplanesMap.put(stra(船の名前, 兵装名), planes(速度, 中隊機数, 小隊機数, ブースト時間, 攻撃準備時間, 攻撃クールダウン, ペイロード, 兵装, エンジン冷却, null));
//
//        airplanesMap.put(stra(船の名前, 兵装名), planes(速度, 中隊機数, 小隊機数, ブースト時間, 初期ブースト時間, 初期ブーストの効果, 攻撃準備時間, 攻撃クールダウン, ペイロード, 兵装, エンジン冷却, null));
//
//        速度
//        defSpped(137);
//        new MinMidMax(96, 125, 165);
//
//        エンジン冷却
//        dClooling;
//        germCooling;
//
//        兵装
//        new Torpedo(ダメージ, 浸水率);
//        new HEBomb(貫通力, 火災, ダメージ);
//        new APBomb(貫通, ダメージ, dRcAngle);
//        new HERocket(貫通, ダメージ, 火災);
//        new APRocket(貫通, ダメージ, dRcAngle);
        return airplanesMap;
    }

    public interface Match {
        boolean apply(String[] key, AirPlanes value);
    }
}
