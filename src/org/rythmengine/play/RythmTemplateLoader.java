package org.rythmengine.play;

import org.rythmengine.resource.ITemplateResource;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class RythmTemplateLoader {
    private static VirtualFileTemplateResourceLoader resourceLoader = VirtualFileTemplateResourceLoader.instance;
    static ConcurrentMap<String, RythmTemplate> cache = new ConcurrentHashMap<String, RythmTemplate>();

    static Method getActionMethod(String path) {
        // strip off /app/views
        String templateRoot = RythmPlugin.templateRoot;
        int pos = path.indexOf(templateRoot);
        if (-1 != pos) path = path.substring(pos + templateRoot.length());
        // strip off leading slash
        while (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);
        // strip off file extension
        pos = path.lastIndexOf('.');
        if (-1 != pos) path = path.substring(0, pos);
        path = path.replace('/', '.');
        pos = path.lastIndexOf('.');
        if (-1 == pos) {
            // should be top level layout template like: main.html or template content
            return null;
        }
        String cName = "controllers." + path.substring(0, pos);
        String mName = path.substring(pos + 1);
        ApplicationClasses.ApplicationClass ac = Play.classes.getApplicationClass(cName);
        if (null == ac) {
            // should be something like 404.html etc
            return null;
        }
        Class<?> c = ac.javaClass;
        Method[] methods = c.getMethods();
        for (Method m: methods) {
            int flag = m.getModifiers();
            if (Modifier.isAbstract(flag) || !Modifier.isStatic(flag) || !Void.TYPE.equals(m.getReturnType())) continue;
            if (mName.equalsIgnoreCase(m.getName())) return m;
        }

        //throw new UnexpectedException("oops, how can I come here without Controller action invocation?");
        // it must be layout template without 'rythm' in path
        return null;
    }

    static void scanRythmFolder() {
        RythmPlugin.info("start to preload templates");
        RythmPlugin.engine.resourceManager().scan();
    }

    public static String templatePath(VirtualFile file) {
        String path = file.relativePath();
        if (!path.contains(RythmPlugin.R_VIEW_ROOT)) return null;
        if (path.indexOf("conf/routes") != -1) return null; // we don't handle routes file at the moment
        if (path.endsWith(".xls") || path.endsWith(".xlsx") || path.endsWith(".pdf")) return null; // we don't handle binary files
        return path;
    }
    
    public static Template cachedTemplate(String path) {
        return cache.get(path);
    }

    public static Template createTemplate(VirtualFile file, String path) {
        ITemplateResource resource = resourceLoader.load(file);
        if (null == resource || !resource.isValid()) return null;

        RythmTemplate tc = new RythmTemplate(resource);
        if (Play.mode.isDev()) tc.refresh(true);
        else tc.refresh();
        if (tc.isValid()) {
            cache.put(file.relativePath(), tc);
        } else {
            tc = null;
        }

        return tc;
    }

    public static Template loadTemplate(VirtualFile file) {
        if (Logger.isTraceEnabled()) RythmPlugin.trace("about to load template: %s", file);
        String path = templatePath(file);
        if (null == path) return null;
        RythmTemplate rt = cache.get(path);
        if (null != rt) {
            if (Logger.isTraceEnabled()) RythmPlugin.trace("template[%s] loaded from cache. About to refresh it", file);
            if (RythmPlugin.engine.mode().isDev()) rt.refresh(); // check if the resource is still valid
            if (Logger.isTraceEnabled()) RythmPlugin.trace("template[%s] refreshed", file);
            return rt.isValid() ? rt : null;
        } else {
            return createTemplate(file, path);
        }
    }

    static void clear() {
        cache.clear();
    }

    public static void main(String[] args) {
        String path = "route";
        int dot = path.lastIndexOf('.');
        if (-1 == dot) path = path + ".rythm";
        else path = path.substring(0, dot) + ".rythm" + path.substring(dot);
        System.out.println(path);
    }
}
