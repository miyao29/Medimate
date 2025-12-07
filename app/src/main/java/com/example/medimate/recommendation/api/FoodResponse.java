package com.example.medimate.recommendation.api;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "response", strict = false)
public class FoodResponse {
    @Element(name = "body", required = false)
    public Body body;
}
