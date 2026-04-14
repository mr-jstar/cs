package solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import prc.CircuitFactory;
import prc.CircuitIO;
import prc.PassiveResistiveCircuit;

/**
 *
 * @author jstar
 */
public class NodalSolver {

    private PassiveResistiveCircuit c;
    private int[] sourceNodes;
    private int[] groundNodes;
    private double[] sourceValues;
    private List<Double> err = new ArrayList<>();

    public NodalSolver(PassiveResistiveCircuit c, int[] g, int[] s, double[] v) {
        this.c = c;
        this.sourceNodes = s;
        this.groundNodes = g;
        this.sourceValues = v;
    }

    public NodalSolver(PassiveResistiveCircuit c, int g, int s, double v) {
        this.c = c;
        this.sourceNodes = new int[1];
        this.sourceNodes[0] = s;
        this.groundNodes = new int[1];
        this.groundNodes[0] = g;
        this.sourceValues = new double[1];
        this.sourceValues[0] = v;
    }

    public double[] getPotentials(double tolerance) {
        boolean stop = false;
        double[] cV = new double[c.noNodes()];
        double[] pV = new double[c.noNodes()];
        boolean[] active = new boolean[c.noNodes()];
        Arrays.fill(active, true);
        int maxit = cV.length;
        for (int g : groundNodes) {
            cV[g] = 0;
            active[g] = false;
        }
        for (int i = 0; i < sourceValues.length; i++) {
            pV[sourceNodes[i]] = sourceValues[i];
            active[sourceNodes[i]] = false;
        }
        double maxErr;
        err.clear();
        for (int it = 0; it < maxit && !stop; it++) {
            for (int g : groundNodes) {
                cV[g] = 0;
            }
            for (int i = 0; i < sourceValues.length; i++) {
                cV[sourceNodes[i]] = sourceValues[i];
                //System.out.println(sourceNodes[i] + " = " + cV[sourceNodes[i]]);
            }
            stop = true;
            maxErr = 0;
            for (int i = 0; i < c.noNodes(); i++) {
                if (active[i]) {
                    cV[i] = 0.0;
                    double Ys = 0;
                    for (int j : c.neighbourNodes(i)) {
                        double Y = 1 / c.resistance(i, j);
                        cV[i] += Y * pV[j];
                        Ys += Y;
                    }
                    cV[i] /= Ys;
                    double noderr= Math.abs(cV[i] - pV[i]);
                    if (noderr > maxErr) {
                        maxErr = noderr;
                    }
                    if (noderr > tolerance) {
                        stop = false;
                    }
                }
            }
            err.add(maxErr);
            System.out.println("Iteration " + it + ": err <= " + maxErr);
            if (!stop) {
                System.arraycopy(cV, 0, pV, 0, cV.length);
            }
        }
        return cV;
    }
    
    public List<Double> err() {
        return err;
    }

    public static void main(String[] args) {
        int nCols = 5;
        int nRows = 5;
        double minResistance = 2.0;
        double maxResistance = 2.0;
        CircuitFactory instance = new CircuitFactory();
        try {
            CircuitIO.savePassiveResistiveCircuit(instance.makeGridRCircuit(nCols, nRows, minResistance, maxResistance), "tmp");
            PassiveResistiveCircuit c = CircuitIO.readPassiveResistiveCircuit("tmp");
            int [] gnd = new int[nRows];
            int [] src = new int[nRows];
            double [] vls = new double[nRows];
            for( int i= 0; i < nRows; i++ ) {
                gnd[i] = i;
                src[i] = nCols * nRows - 1 - i;
                vls[i] = 1;
            }
            NodalSolver s = new NodalSolver(c, gnd, src, vls );
            double[] v = s.getPotentials(1e-6);
            for (Double i : v) {
                System.out.println(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
