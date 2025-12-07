package com.example.medimate.recommendation.api;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "body", strict = false)
public class Body {
    @Element(name = "totalCount", required = false)
    public int totalCount = 0;

    @Element(name = "items", required = false)
    public Items items;
}
