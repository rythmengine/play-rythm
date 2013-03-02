package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.internal.ExtensionManager;
import com.greenlaw110.rythm.internal.IJavaExtension;
import play.Play;
import play.classloading.ApplicationClasses;
import play.templates.JavaExtensions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 21/02/12
 * Time: 6:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class JavaExtensionBridge {

    static class PlayVoidParameterExtension extends IJavaExtension.VoidParameterExtension {
        public PlayVoidParameterExtension(String name) {
            super("JavaExtensions", name, String.format("JavaExtensions.%s", name));
        }
    }

    static class PlayParameterExtension extends IJavaExtension.ParameterExtension {
        public PlayParameterExtension(String name, String signature) {
            super("JavaExtensions", name, signature, String.format("JavaExtensions.%s", name));
        }
    }

    public static void registerAppJavaExtensions(RythmEngine engine) {
        long l = System.currentTimeMillis();
        ExtensionManager em = engine.extensionManager();
        List<ApplicationClasses.ApplicationClass> classes = Play.classes.getAssignableClasses(JavaExtensions.class);
        for (ApplicationClasses.ApplicationClass ac : classes) {
            Class<?> jc = ac.javaClass;
            for (Method m : jc.getDeclaredMethods()) {
                int flag = m.getModifiers();
                if (!Modifier.isPublic(flag) || !Modifier.isStatic(flag)) continue;
                int len = m.getParameterTypes().length;
                if (len <= 0) continue;
                String cn = jc.getSimpleName();
                String cn0 = jc.getName();
                String mn = m.getName();
                if (len == 1) {
                    em.registerJavaExtension(new IJavaExtension.VoidParameterExtension(cn, mn, String.format("%s.%s", cn0, mn)));
                } else {
                    em.registerJavaExtension(new IJavaExtension.ParameterExtension(cn, mn, ".+", String.format("%s.%s", cn0, mn)));
                }
            }
        }
        RythmPlugin.debug("%sms to register application java extension", System.currentTimeMillis() - l);
    }

    public static void registerPlayBuiltInJavaExtensions(RythmEngine engine) {
        long l = System.currentTimeMillis();
        String[] voidExtensions = {
                "enumValues",
                "asXml",
                "eval",
                "since",
                "addSlashes",
                "pluralize",
                "page",
                "slugify",
                "last"
        };
        String[] nonVoidExtensions = {
                "contains",
                "add",
                "remove",
                "pad",
                "page",
                "since",
                "asdate",
                "cut",
                "divisibleBy",
                "pluralize",
                "slugify",
                "yesno",
                "join"
        };
        ExtensionManager em = engine.extensionManager();
        for (String s : voidExtensions) {
            em.registerJavaExtension(new PlayVoidParameterExtension(s));
        }
        for (String s : nonVoidExtensions) {
            em.registerJavaExtension(new PlayParameterExtension(s, ".+"));
        }
        // moved to codegen.source_code_enhancer.impl: engine.registerGlobalImports("play.templates.JavaExtensions");
        RythmPlugin.debug("%sms to register play built-in java extension", System.currentTimeMillis() - l);
    }
}
