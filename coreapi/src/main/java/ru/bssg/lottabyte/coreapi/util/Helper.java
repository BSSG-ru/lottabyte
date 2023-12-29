package ru.bssg.lottabyte.coreapi.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Helper {
    public static String fillingInUpdate(String query, Map<String, Object> columns){
        List<String> setClauses = new ArrayList<>();
        if(columns != null && !columns.isEmpty()) {
            for (String key : columns.keySet()) {
                if (columns.get(key) != null) {
                    setClauses.add("?");
                }
            }
        }
        String s = String.format(
                query,
                StringUtils.join(setClauses, ",")
        );
        return s;
    }

    public static String stringClipping(String json, Integer lines){
        int count = count(json, "\n");
        for(int i = count; i > lines; i--){
            json = json.substring(0,json.lastIndexOf("\n"));
        }
        return json;
    }
    public static int count(String str, String target) {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }

    public static <T2> List<T2> getEmptyListIfNull(List<T2> list) {
        return (List)(list == null ? new ArrayList() : list);
    }
}
