package org.hyperledger.besu.ethereum.blockcreation;

import avm.Blockchain;
import org.aion.avm.tooling.abi.Callable;

public class HelloWorld {

    private static String myStr = "Hello AVM";

    @Callable
    public static void sayHello() {
        Blockchain.println("Hello World!");
    }

    @Callable
    public static final String greet(final String name) {
        return "Hello " + name;
    }

    @Callable
    public static final String getString() {
        Blockchain.println("Current string is " + myStr);
        return myStr;
    }

    @Callable
    public static final void setString(final String newStr) {
        myStr = newStr;
        Blockchain.println("New string is " + myStr);
    }

}
