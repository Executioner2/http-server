package com.lani;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void stringReplaceTest() {
        String uri = "//lani.com//servlet/getName/";

        System.out.println(uri.lastIndexOf("/"));
        System.out.println(uri.lastIndexOf("/", 9));
    }

}
