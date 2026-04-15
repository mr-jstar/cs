package prc;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jstar
 */
public class CircuitFactoryTest {
    
    public CircuitFactoryTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of makeGridRCircuit method, of class CircuitFactory.
     */
    @Test
    public void testMakeGridRCircuit() {
        System.out.println("makeGridRCircuit");
        int nCols = 4;
        int nRows = 5;
        double minResistance = 1.0;
        double maxResistance = 1.0;
        CircuitFactory instance = new CircuitFactory();
        GridPassiveResistiveCircuit result = instance.makeGridRCircuit(nCols, nRows, minResistance, maxResistance);
        assertEquals(nCols*nRows, result.noNodes());
        for( int i= 1; i < result.noNodes(); i++ ) {
            //System.out.println((i-1) + "-" + i);
            if( i % nRows != 0 )
                assertEquals(1.0, result.resistance(i-1, i),0.0);
            else
                assertEquals(Double.POSITIVE_INFINITY, result.resistance(i-1, i),0.0);
        }
        //System.out.println("---");
        for( int i= 0; i < result.noNodes()-1; i++ ) {
            //System.out.println(i + "-" + (i+1));
            if( i % nRows != nRows-1 )
                assertEquals(1.0, result.resistance(i+1, i),0.0);
            else
                assertEquals(Double.POSITIVE_INFINITY, result.resistance(i+1, i),0.0);
        }
    }
    
}
