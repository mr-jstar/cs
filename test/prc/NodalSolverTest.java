package prc;

import solvers.NodalSolver;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author jstar
 */
public class NodalSolverTest {

    public NodalSolverTest() {
    }

    @Test
    public void testGetPotentials() {
        int nCols = 2;
        int nRows = 2;
        double minResistance = 1.0;
        double maxResistance = 1.0;
        CircuitFactory instance = new CircuitFactory();
        GridPassiveResistiveCircuit c = instance.makeGridRCircuit(nCols, nRows, minResistance, maxResistance);
        NodalSolver s = new NodalSolver(c, 0, 3, 1);
        s.solve(1e-6);
        double[] v = s.getPotential();
        assertEquals(4, v.length);
        assertEquals(0.0, v[0], 1e-6);
        assertEquals(1.0, v[3], 1e-6);
        assertEquals(0.5, v[1], 1e-6);
        assertEquals(0.5, v[2], 1e-6);
        nCols = 3;
        nRows = 3;
        c = instance.makeGridRCircuit(nCols, nRows, minResistance, maxResistance);
        s = new NodalSolver(c, 0, 8, 10);
        s.solve(1e-6);
        v = s.getPotential();
        assertEquals(9, v.length);
        assertEquals(0.0, v[0], 1e-6);
        assertEquals(3.333, v[1], 1e-3);
        assertEquals(4.999, v[2], 1e-3);
        assertEquals(3.333, v[3], 1e-3);
        assertEquals(5.0, v[4], 1e-3);
        assertEquals(6.666, v[5], 1e-3);
        assertEquals(5.0, v[6], 1e-3);
        assertEquals(6.666, v[7], 1e-3);
        assertEquals(10.0, v[8], 1e-6);
    }

}
