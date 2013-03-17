package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.exception.RythmException;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.resource.TemplateResourceManager;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
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

    private static void scanRythmFolder(VirtualFile root, final TemplateResourceManager rm) {
        class FileTraversal {
            public final void traverse( final VirtualFile f )  {
                if (f.isDirectory()) {
                    // aha, we don't want to traverse .svn
                    if (".svn".equals(f.getName())) return;
                    final List<VirtualFile> children = f.list();
                    for( VirtualFile child : children ) {
                        traverse(child);
                    }
                    return;
                }
                onFile(f);
            }
            public void onFile( final VirtualFile f ) {
                try {
                    VirtualFileTemplateResourceLoader.VirtualFileTemplateResource resource = new VirtualFileTemplateResourceLoader.VirtualFileTemplateResource(f);
                    rm.resourceLoaded(resource);
//                    TemplateClass templateClass = RythmPlugin.engine.classes().getByTemplate(resource.getKey());
//                    if (null == templateClass) {
//                        templateClass = new TemplateClass(resource, RythmPlugin.engine);
//                    }
//                    ITag tag = (ITag)templateClass.asTemplate();
//                    if (null != tag)RythmPlugin.engine.registerTemplate(tag);
                } catch (RythmException e) {
                    RythmTemplate.handleRythmException(e);
                } catch (Exception e) {
                    Logger.warn(e, "error pre-loading template: %s", f.relativePath());
                    // might be groovy template, let's ignore it
                }
            }
        }
        new FileTraversal().traverse(root);
    }

    static void scanRythmFolder() {
        RythmPlugin.info("start to preload templates");
//        long ts = System.currentTimeMillis();
        RythmPlugin.engine.resourceManager().scan(null);
//        String s = RythmPlugin.templateRoot;
//        TemplateResourceManager rm = RythmPlugin.engine.resourceManager();
//        for (VirtualFile root: Play.roots) {
//            VirtualFile templateRoot = root.child(s);
//            if (!templateRoot.isDirectory()) continue;
//            scanRythmFolder(templateRoot, rm);
//        }
//        ts = System.currentTimeMillis() - ts;
//        RythmPlugin.trace("%sms to preload templates", ts);
    }

    private static Object lock_ = new Object();
    
    public static String templatePath(VirtualFile file) {
        String path = file.relativePath();
//RythmPlugin.info("loading template from virtual file: %s", file.relativePath());
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
