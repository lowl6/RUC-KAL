package cn.edu.ruc.kal.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/** 用于把简单 List<Map> 序列化成 String 存进 VARCHAR 字段。 */
public final class JsonText {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonText() {}

    public static String stringify(List<Map<String, String>> v) {
        if (v == null || v.isEmpty()) return null;
        try { return MAPPER.writeValueAsString(v); }
        catch (Exception e) { return null; }
    }

    public static List<Map<String, String>> parseList(String s) {
        if (s == null || s.isBlank()) return List.of();
        try {
            return MAPPER.readValue(s, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
