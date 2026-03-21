package org.w7ls.main.weapon;

import org.junit.jupiter.api.Test;
import org.w7ls.utils.MinMax;
import org.w7ls.utils.StringTmp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.w7ls.utils.StringTmp.csvRow;

class APShellTest {

    @Test
    void penetration() throws FileNotFoundException {
        APShell s = new APShell(
                6350,
                2950,
                995,
                15,
                207,
                new MinMax(50, 65),
                0.25
        );

        System.out.println(s.penetration(20));

        PrintStream logPrinter = new PrintStream(new FileOutputStream("results/Test.csv"));
        logPrinter.println("dist, pen");
        for (double d = 0; d < 20; d += 0.01) {
            logPrinter.print(csvRow(d, s.penetration(d)));
        }
    }
}