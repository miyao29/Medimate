package com.example.medimate.recommendation.api;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import java.util.List;

@Root(name = "items", strict = false)
public class Items {
    @ElementList(inline = true, name = "item", required = false)
    public List<Product> productList;
}
