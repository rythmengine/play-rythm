package org.rythmengine.play.utils;

import org.rythmengine.utils.F;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JavaHelper {
    private static Map<F.T2<Class<?>, String>, Field> cache = new HashMap<F.T2<Class<?>, String>, Field>();
    public static boolean hasProperty(Object o, String prop) {
        F.T2 key = F.T2(o.getClass(), prop);
        if (cache.containsKey(key)) return true;
        try {
            Field f = o.getClass().getField(prop);
            cache.put(key, f);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static <T> T getProperty(Object o, String prop) {
        F.T2 key = F.T2(o.getClass(), prop);
        Field f = cache.get(key);
        try {
            if (null == f) {
                f = o.getClass().getField(prop);
                cache.put(key, f);
            }
            return (T)f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void setProperty(Object o, String prop, Object val) {
        F.T2 key = F.T2(o.getClass(), prop);
        Field f = cache.get(key);
        try {
            if (null == f) {
                f = o.getClass().getField(prop);
                cache.put(key, f);
            }
            f.set(o, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
