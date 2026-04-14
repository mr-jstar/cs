package prc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jstar
 */
public class PassiveCircuitImplementation implements PassiveResistiveCircuit {

    private Map<Connection,Set<Element>> elements = new HashMap<>();
    private Map<Integer,Set<Integer>> neighbours = new HashMap<>();
    
    private static class Connection {

        int n1;
        int n2;

        Connection(int n1, int n2) {
            this.n1 = n1;
            this.n2 = n2;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Connection b) {
                return (b.n1 == n1 && b.n2 == n2 || b.n1 == n2 && b.n2 == n1);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Math.min(n1, n2);
            hash = 59 * hash + Math.max(n1, n2);;
            return hash;
        }
    }

    private static class Element {

        int n1;
        int n2;
        double R;

        Element(int n1, int n2, double R) {
            this.n1 = n1;
            this.n2 = n2;
            this.R = R;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Element e) {
                return e.R == R && (e.n1 == n1 && e.n2 == n2 || e.n1 == n2 && e.n2 == n1);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Math.min(n1, n2);
            hash = 59 * hash + Math.max(n1, n2);;
            hash = 59 * hash + (int) (Double.doubleToLongBits(this.R) ^ (Double.doubleToLongBits(this.R) >>> 32));
            return hash;
        }
    }

    public void addElement(int i, int j, double R) {
        Connection nb = new Connection(i,j);
        if( !elements.containsKey(nb))
            elements.put(nb, new HashSet<>());
        if (!neighbours.containsKey(i)) {
            neighbours.put(i,new HashSet<>());
        }
        if (!neighbours.containsKey(j)) {
            neighbours.put(j,new HashSet<>());
        }
        elements.get(nb).add(new Element(i, j, R));
        neighbours.get(i).add(j);
        neighbours.get(j).add(i);
    }

    @Override
    public int noNodes() {
        return neighbours.size();
    }

    @Override
    public Set<Integer> neighbourNodes(int n) {
        Set<Integer> s = neighbours.get(n);
        if( s == null ) {
            s = new HashSet<Integer>();
        }  
        return s;
    }

    @Override
    public double resistance(int n1, int n2) {
        Connection b = new Connection(n1,n2);
        if( elements.containsKey(b) ) {
            Set<Element> elems = elements.get(b);
            double g = 0;
            for( Element e : elems )
                g += 1 / e.R;
            return 1 / g;
        } else 
            return Double.POSITIVE_INFINITY;
            
    }
}
