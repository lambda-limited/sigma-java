/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lambda.sigma;

import java.io.IOException;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author James Thorpe
 */
public class TypesTest {
    
    public TypesTest() {
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
     * Test of coerceType method, of class Types.
     */
    @Test
    public void testCoerceType() {
        System.out.println("Test null coercion");
        Assert.assertNull(Types.coerceType(null, String.class));
        Assert.assertNull(Types.coerceType(null, void.class));
        testThrow(() -> Types.coerceType(null, double.class), "unable to coerce null");
    }
    
    private void testThrow(Supplier<Object> fn, String msgFragment)  {
        try {
            fn.get();
            fail();
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains(msgFragment));
        }
    }
}
