package nl.stefanvonk.shoppinglist;

import java.util.List;

public class Lijst {
    String lijstId;
    String lijstName;
    List<String> lijstProducts;

    public Lijst(){

    }

    public Lijst(String lijstId, String lijstName, List<String> lijstProducts) {
        this.lijstId = lijstId;
        this.lijstName = lijstName;
        this.lijstProducts = lijstProducts;
    }

    public String getLijstId() {
        return lijstId;
    }

    public String getLijstName() {
        return lijstName;
    }

    public List getLijstProducts() {
        return lijstProducts;
    }
}