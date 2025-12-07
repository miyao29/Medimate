package com.example.medimate.recommendation.api;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "item", strict = false)
public class Product {
    @Element(name = "PRDUCT", required = false)
    public String productName;

    @Element(name = "ENTRPS", required = false)
    public String company;

    @Element(name = "MAIN_FNCTN", required = false)
    public String mainFunction;
}