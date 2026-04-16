package prc;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author jstar
 */
public class CircuitFactory {

    public static GridPassiveResistiveCircuit makeGridRCircuit(int nCols, int nRows, double minResistance, double maxResistance) {
        GridCircuitImplementation circ = new GridCircuitImplementation();
        double spread = maxResistance - minResistance;
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int c = 0; c < nCols; c++) {
            for (int e = 0; e < nRows-1; e++) {
                circ.setColumnResistance(c, e, minResistance + spread * rand.nextDouble());
            }
        }
        for (int r = 0; r < nRows; r++) {
            for (int e = 0; e < nCols-1; e++) {
                circ.setRowResistance(r, e, minResistance + spread * rand.nextDouble());
            }
        }
        return circ;
    }
}
