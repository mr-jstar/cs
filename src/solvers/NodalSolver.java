package solvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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
    private double[] V;
    private List<Double> err = new ArrayList<>();
    private int it;

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

    public void solve(double tolerance) {
        boolean stop = false;
        double[] cV = new double[c.noNodes()];
        double[] pV = new double[c.noNodes()];
        boolean[] active = new boolean[c.noNodes()];
        Arrays.fill(active, true);
        int maxit = cV.length * 10;
        for (int g : groundNodes) {
            pV[g] = 0.0;
            active[g] = false;
        }
        for (int i = 0; i < sourceNodes.length; i++) {
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
                    double noderr = Math.abs(cV[i] - pV[i]);
                    if (noderr > maxErr) {
                        maxErr = noderr;
                    }
                    if (noderr > tolerance) {
                        stop = false;
                    }
                }
            }
            err.add(maxErr);
            //System.out.println("Iteration " + it + ": err <= " + maxErr);
            if (!stop) {
                System.arraycopy(cV, 0, pV, 0, cV.length);
            }
        }
        this.V = cV;
    }

    public double[] getPotential() {
        return V;
    }

    public List<Double> err() {
        return err;
    }

    private class SolvingThread extends Thread {

        private PassiveResistiveCircuit c;
        private double[] cV;
        private double[] pV;
        private boolean[] active;
        private int no;
        private double[] errors;
        private int firstNode;
        private int lastNode;
        CyclicBarrier barrier;
        boolean stop = false;

        SolvingThread(int i, PassiveResistiveCircuit c, double[] pV, double[] cV, boolean[] active, double[] currErrors, int first, int last, CyclicBarrier barrier) {
            this.no = i;
            this.c = c;
            this.cV = cV;
            this.pV = pV;
            this.active = active;
            this.errors = currErrors;
            this.firstNode = first;
            this.lastNode = last;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            System.out.println("Thread " + no + " starting");
            while (!stop) {
                double maxErr = 0.0;
                for (int i = firstNode; i < lastNode; i++) {
                    if (active[i]) {
                        cV[i] = 0.0;
                        double Ys = 0;
                        for (int j : c.neighbourNodes(i)) {
                            double Y = 1 / c.resistance(i, j);
                            cV[i] += Y * pV[j];
                            Ys += Y;
                        }
                        cV[i] /= Ys;
                        double noderr = Math.abs(cV[i] - pV[i]);
                        if (noderr > maxErr) {
                            maxErr = noderr;
                        }
                    }
                }
                errors[no] = maxErr;
                try {
                    //System.out.println(no + " at 1st barrier");
                    barrier.await();
                    //System.out.println(no + " at 2nd barrier");
                    barrier.await();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            System.out.println("Thread " + no + " stopped");
        }

    }

    public void solveInParallel(double tolerance, int nThreads) throws InterruptedException, BrokenBarrierException {
        boolean stop = false;
        int n = c.noNodes();
        double[] cV = new double[n];
        double[] pV = new double[n];
        boolean[] active = new boolean[n];
        Arrays.fill(active, true);
        int maxit = cV.length;
        for (int g : groundNodes) {
            pV[g] = 0;
            cV[g] = 0;
            active[g] = false;
        }
        for (int i = 0; i < sourceValues.length; i++) {
            pV[sourceNodes[i]] = sourceValues[i];
            cV[sourceNodes[i]] = sourceValues[i];
            active[sourceNodes[i]] = false;
        }
        it = 0;
        err.clear();
        CyclicBarrier barrier = new CyclicBarrier(nThreads + 1);
        SolvingThread[] threads = new SolvingThread[nThreads];
        double[] currErrors = new double[nThreads];
        int perThread = n / nThreads;
        if( n % nThreads != 0 )
            perThread++;
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new SolvingThread(i, c, pV, cV, active, currErrors, i * perThread, (i + 1) * perThread > n ? n : (i + 1) * perThread, barrier);
            threads[i].start();
        }
        for (it = 0; it < maxit; it++) {
            //System.out.println("Main at 1st barrier");
            barrier.await();
            double maxErr = currErrors[0];
            for (int i = 0; i < nThreads; i++) {
                if (currErrors[i] > maxErr) {
                    maxErr = currErrors[i];
                }
            }
            err.add(maxErr);
            if (maxErr <= tolerance || it == maxit - 1) { // stop
                System.out.println("Stopping threads");
                for (int i = 0; i < nThreads; i++) {
                    threads[i].stop = true;
                }
            } else {
                System.arraycopy(cV, 0, pV, 0, cV.length);
                for (int g : groundNodes) {
                    cV[g] = 0;
                }
                for (int i = 0; i < sourceValues.length; i++) {
                    cV[sourceNodes[i]] = sourceValues[i];
                    //System.out.println(sourceNodes[i] + " = " + cV[sourceNodes[i]]);
                }
            }
            //System.out.println("Main at 2nd barrier");
            barrier.await();
        }
        for (int i = 0; i < nThreads; i++) {
            threads[i].join();
        }
        this.V = cV;
    }

    public static void main(String[] args) {
        int nCols = 80;
        int nRows = 80;
        double minResistance = 2.0;
        double maxResistance = 2.0;
        CircuitFactory instance = new CircuitFactory();
        try {
            CircuitIO.savePassiveResistiveCircuit(instance.makeGridRCircuit(nCols, nRows, minResistance, maxResistance), "tmp");
            PassiveResistiveCircuit c = CircuitIO.readPassiveResistiveCircuit("tmp");
            int[] gnd = new int[nRows];
            int[] src = new int[nRows];
            double[] vls = new double[nRows];
            for (int i = 0; i < nRows; i++) {
                gnd[i] = i;
                src[i] = nCols * nRows - 1 - i;
                vls[i] = 1;
            }
            NodalSolver s = new NodalSolver(c, gnd, src, vls);
            
            long start = System.nanoTime();

            //s.solve(1e-6);
            s.solveInParallel(1e-6, 2);

            long end = System.nanoTime();
            long elapsed = end - start;

            System.out.println("Czas [ns]: " + elapsed);
            System.out.println("Czas [ms]: " + elapsed / 1_000_000.0);
            System.out.println("Czas [s]: " + elapsed / 1e9);

            double[] v = s.getPotential();
            if( v.length < 50)
                for (Double i : v) {
                    System.out.println(i);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
