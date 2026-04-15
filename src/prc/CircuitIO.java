package prc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.Set;

/**
 *
 * @author jstar
 */
public class CircuitIO {

    public static void savePassiveResistiveCircuit(PassiveResistiveCircuit c, String fileName) throws IOException {
        PrintWriter w = new PrintWriter(new File(fileName));
        for (int n = 0; n < c.noNodes(); n++) {
            Set<Integer> nbrs = c.neighbourNodes(n);
            for (Integer nn : nbrs) {
                if (nn > n) {  // ignore duplicated entries
                    double r = c.resistance(n, nn);
                    if (r != Double.POSITIVE_INFINITY) {
                        w.println(n + " " + nn + " " + r);
                    }
                }
            }
        }
        w.close();
    }

    public static PassiveResistiveCircuit readPassiveResistiveCircuit(String fileName) throws IOException {
        PassiveCircuitImplementation c = new PassiveCircuitImplementation();
        LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(fileName)));
        String line;
        while ((line = r.readLine()) != null) {
            String[] fields = line.split("\\s+");
            try {
                c.addElement(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Double.parseDouble(fields[2]));
            } catch (ArrayIndexOutOfBoundsException|NumberFormatException e) {
                throw new IOException("Invalid line # " + r.getLineNumber() + " in file " + fileName);
            }
        }
        return c;
    }
}
