package prc;

import java.util.Set;

/**
 *
 * @author jstar
 */
public interface PassiveResistiveCircuit {
    public int noNodes();
    public Set<Integer> neighbourNodes( int n );      
    public double resistance( int n1, int n2 );
}
