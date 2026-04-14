package prc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jstar
 */
public class GridCircuitImplementation implements GridPassiveResistiveCircuit {

    private Map<Integer, Map<Integer, Double>> columns = new HashMap<>();
    private Map<Integer, Map<Integer, Double>> rows = new HashMap<>();

    @Override
    public int getRowsNo() {
        return Collections.max(rows.keySet()) + 1;
    }

    @Override
    public int getColsNo() {
        return Collections.max(columns.keySet()) + 1;
    }

    @Override
    public double getColumnResistance(int col, int resistor) {
        //System.out.println("get R for col " + col +" resitor " + resistor);
        if (!columns.containsKey(col)) {
            return Double.POSITIVE_INFINITY;
        } else {
            if (!columns.get(col).containsKey(resistor)) {
                return Double.POSITIVE_INFINITY;
            } else {
                return columns.get(col).get(resistor);
            }
        }
    }

    @Override
    public double getRowResistance(int row, int resistor) {
        //System.out.println("get R for row " + row +" resitor " + resistor);
        if (!rows.containsKey(row)) {
            return Double.POSITIVE_INFINITY;
        } else {
            if (!rows.get(row).containsKey(resistor)) {
                return Double.POSITIVE_INFINITY;
            } else {
                return rows.get(row).get(resistor);
            }
        }
    }

    public void setRowResistance(int row, int resistor, double value) {
        //System.out.println("set R for row " + row +" resitor " + resistor + " =" +value);
        if (!rows.containsKey(row)) {
            rows.put(row, new HashMap<>());
        }

        rows.get(row).put(resistor, value);
    }

    public void setColumnResistance(int col, int resistor, double value) {
        //System.out.println("set R for col " + col +" resitor " + resistor + " =" +value);
        if (!columns.containsKey(col)) {
            columns.put(col, new HashMap<>());
        }

        columns.get(col).put(resistor, value);
    }

}
