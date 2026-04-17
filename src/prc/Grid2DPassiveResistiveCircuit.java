package prc;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jstar
 */
public interface Grid2DPassiveResistiveCircuit extends PassiveResistiveCircuit {

    public int getRowsNo();

    public int getColsNo();

    public double getColumnResistance(int col, int resistor);

    public double getRowResistance(int row, int resistor);

    @Override
    default public int noNodes() {
        return getRowsNo() * getColsNo();
    }

    @Override
    default public Set<Integer> neighbourNodes(int node) {
        int c = node / getRowsNo();
        int r = node % getRowsNo();
        Set<Integer> n = new HashSet<>();
        if (c > 0) {
            n.add(node - getRowsNo());
        }
        if (r > 0) {
            n.add(node - 1);
        }
        if (c < getColsNo() - 1) {
            n.add(node + getRowsNo());
        }
        if (r < getRowsNo() - 1) {
            n.add(node + 1);
        }
        return n;
    }

    @Override
    default public double resistance(int node1, int node2) {
        if (node1 == node2) {
            return 0.0;
        }
        //System.out.println(getColsNo() + "x" + getRowsNo());
        int c1 = node1 / getRowsNo();
        int r1 = node1 % getRowsNo();
        int c2 = node2 / getRowsNo();
        int r2 = node2 % getRowsNo();
        //System.out.println( node1 + "-> (" +c1+","+r1+")");
        //System.out.println( node2 + "-> (" +c2+","+r2+")");
        if (c1 == c2) {
            if (Math.abs(r2 - r1) == 1) {
                return getColumnResistance(c1, Math.min(r1, r2));
            } else {
                return Double.POSITIVE_INFINITY;
            }
        } else if (r1 == r2) {
            if (Math.abs(c2 - c1) == 1) {
                return getRowResistance(r1, Math.min(c1, c2));
            } else {
                return Double.POSITIVE_INFINITY;
            }
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }
}
