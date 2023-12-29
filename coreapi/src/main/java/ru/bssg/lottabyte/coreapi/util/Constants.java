package ru.bssg.lottabyte.coreapi.util;

import org.springframework.beans.factory.annotation.Value;
import ru.bssg.lottabyte.core.model.ca.AttributeType;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static Integer sqlInLimit = 1000;

    Map<AttributeType, Integer> WHEN_ADDING = new HashMap<AttributeType, Integer>() {{
        put(AttributeType.valueOf("String"), 6);
        put(AttributeType.valueOf("Date"), 5);
        put(AttributeType.valueOf("Enumerated"), 4);
        put(AttributeType.valueOf("Numeric"), 3);
        put(AttributeType.valueOf("Reference"), 7);
    }};

    Map<Integer, AttributeType> WHEN_EXTRACTING = new HashMap<Integer, AttributeType>() {{
        put(6, AttributeType.valueOf("String"));
        put(5, AttributeType.valueOf("Date"));
        put(4, AttributeType.valueOf("Enumerated"));
        put(3, AttributeType.valueOf("Numeric"));
        put(7, AttributeType.valueOf("Reference"));
    }};
}
