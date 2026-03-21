import org.w7ls.main.CUDAHeader;
import org.w7ls.main.CheckersJC;
import org.w7ls.main.weapon.AirPlanes;
import org.w7ls.main.weapon.Consumable;
import org.w7ls.main.weapon.Shell;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.lang.Math.sqrt;

public class JCudaTest {

    static int c = 0;
    public static void main(String[] args) throws FileNotFoundException {
        Test.printToFile("results/Test.csv", getCSV(42 * sqrt(2d), 0.01, a -> a.weapon().type() == Shell.ShellType.HE_BOMB));
    }

    public static String getCSV(double dist, double dh, Predicate<AirPlanes> map) {
        HashMap<String, AirPlanes> planes = Test.initAirplanes();
        CheckersJC ckrsjc = new CheckersJC();
        ckrsjc.setParallel(true);
        ckrsjc.setEnableConsumable(Consumable.Name.ENGINE_COOLING, true);

        int count;
        count = (int) (dist / dh);

        AirPlanes[] planesA = planes.values().stream()
                .filter(map)
                .toArray(AirPlanes[]::new);

        String[][] cvPlanesRes = new String[planesA.length + 1][];
        cvPlanesRes[0] = IntStream.range(1, count + 1)
                .mapToDouble(d -> d * dh)
                .mapToObj(String::valueOf)
                .toArray(String[]::new);
        cvPlanesRes[0][0] = "dist";

        IntStream.range(1, cvPlanesRes.length)
                .parallel()
                .forEach(i -> {
            AirPlanes plane = planesA[i - 1];
            String[] res = cvPlanesRes[i] = new String[count + 1];
            String[] restmp = Arrays.stream(ckrsjc.dpmCVs(plane, dist + dh, dh))
                    .mapToObj(String::valueOf)
                    .toArray(String[]::new);
            System.arraycopy(restmp, 0, res, 1, count);
            res[0] = plane.name();
        });

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cvPlanesRes[0].length; i++) {
            for (int j = 0; j < cvPlanesRes.length; j++) {
                sb.append(cvPlanesRes[j][i]);
                if (j < cvPlanesRes.length - 1)
                    sb.append(", ");
            }
            if (i < cvPlanesRes[0].length - 1)
                sb.append("\n");
        }
        return sb.toString();
    }

    public static void mandelbrotSet() {
                CUDAHeader cm = new CUDAHeader("mandelbrot_set", Set.of("mandelbrot_set"));

        int w = 512;
        int h = 512;
        int n = w * h;
        float[] r = new float[n];
        float[] g = new float[n];
        float[] b = new float[n];

        cm.invoke2d("mandelbrot_set", w, h, r, g, b);

        BufferedImage bf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int i;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y * w + x;
                bf.setRGB(x, y, toRGB(r[i], g[i], b[i]));
            }
        }
        Path path = Path.of("results/man.png");
        try (FileOutputStream fo = new FileOutputStream(path.toFile())) {
            if (!Files.exists(path))
                Files.createFile(path);
            ImageIO.write(bf, "PNG", fo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int toRGB(float r, float g, float b) {
        int ri = (int) (255 * r);
        int gi = (int) (255 * g);
        int bi = (int) (255 * b);
        return (ri << 16) |
               (gi << 8) |
               (bi);
    }
}
