package org.crosswire.jsword.passage;

import java.util.Arrays;

/**
 * Simple test to manually verify tokenization behavior
 */
public class TokenizeTest {
    public static void main(String[] args) {
        try {
            // Test problematic cases from GitHub issues
            System.out.println("Testing tokenization for problematic cases:");
            
            String[] test1 = AccuracyType.tokenize("2Macc.10.38");
            System.out.println("2Macc.10.38 -> " + Arrays.toString(test1));
            
            String[] test2 = AccuracyType.tokenize("1Esd.1.12");
            System.out.println("1Esd.1.12 -> " + Arrays.toString(test2));
            
            String[] test3 = AccuracyType.tokenize("3John.1.1");
            System.out.println("3John.1.1 -> " + Arrays.toString(test3));
            
            // Test that regular cases still work
            System.out.println("\nTesting regular cases:");
            
            String[] test4 = AccuracyType.tokenize("Gen.1.1");
            System.out.println("Gen.1.1 -> " + Arrays.toString(test4));
            
            String[] test5 = AccuracyType.tokenize("Genesis 1:1");
            System.out.println("Genesis 1:1 -> " + Arrays.toString(test5));
            
            String[] test6 = AccuracyType.tokenize("Bible:Gen.4.26");
            System.out.println("Bible:Gen.4.26 -> " + Arrays.toString(test6));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
