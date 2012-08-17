package com.greenlaw110.rythm.play.utils;

import com.greenlaw110.rythm.play.RythmPlugin;
import com.greenlaw110.rythm.play.VirtualFileTemplateResourceLoader;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.resource.StringTemplateResource;
import play.Play;
import play.libs.IO;
import play.templates.Template;
import play.vfs.VirtualFile;

/**
 * <code>TemplateClassAppEnhancer</code> scans a specific file named "app/rythm/_add_on.src" in which the
 * content will be added to template class source code
 */
public class TemplateClassAppEnhancer {
    public static final String ADD_ON_FILE_NAME = "__add_on.src";
    private static ITemplateResource cache = null;
    public static void clearCache() {cache = null;}
    public static boolean changed() {
        return null == cache ? false : cache.refresh();
    }
    public static String sourceCode() {
        if (null != cache) return cache.asTemplateContent();
        String fn = Play.configuration.getProperty("rythm.addon", ADD_ON_FILE_NAME);
        VirtualFile vf = Play.getVirtualFile("app/rythm/" + fn);
        if (null == vf || !vf.exists()) {
            cache = new StringTemplateResource("");
            return "";
        }
        VirtualFileTemplateResourceLoader loader = VirtualFileTemplateResourceLoader.instance;
        cache = loader.load(vf);
        return cache.asTemplateContent();
    }
}
