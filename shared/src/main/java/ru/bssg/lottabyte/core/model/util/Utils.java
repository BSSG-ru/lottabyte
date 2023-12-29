package ru.bssg.lottabyte.core.model.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Utils {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

    public static String toString(Object object) {
        return GSON.toJson(object);
    }

}
