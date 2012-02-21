package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.utils.IJavaExtension;

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

    public static void registerPlayBuiltInJavaExtensions(RythmEngine engine) {
        String[] voidExtensions = {
            "enumValues",
            "asXml",
            "capitalizeWords",
            "eval",
            "format",
            "since",
            "nl2br",
            "urlEncode",
            "formatSize",
            "addSlashes",
            "capFirst",
            "capAll",
            "pluralize",
            "noAccents",
            "slugify",
            "camelCase",
            "last"
        };
        String[] nonVoidExtensions = {
            "contains",
            "add",
            "remove",
            "pad",
            "format",
            "page",
            "since",
            "asdate",
            "formatCurrency",
            "cut",
            "divisibleBy",
            "pluralize",
            "slugify",
            "yesno",
            "join"
        };
        for (String s: voidExtensions) {
            engine.registerJavaExtension(new PlayVoidParameterExtension(s));
        }
        for (String s: nonVoidExtensions) {
            engine.registerJavaExtension(new PlayParameterExtension(s, ".+"));
        }
        engine.registerGlobalImports("play.templates.JavaExtensions");
    }
}
