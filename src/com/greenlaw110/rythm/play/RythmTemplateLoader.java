package com.greenlaw110.rythm.play;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.mvc.Controller;
import play.templates.Template;
import play.vfs.VirtualFile;

import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.runtime.ITag;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class RythmTemplateLoader {
    private static VirtualFileTemplateResourceLoader resourceLoader = VirtualFileTemplateResourceLoader.instance;
    static Map<String, RythmTemplate> cache = new HashMap<String, RythmTemplate>();

    //TODO support system wide templates like 404.html etc
    private static Set<String> whiteList = new HashSet<String>();
    private static Set<String> blackList = new HashSet<String>();
    
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
    
    static boolean whiteListed(String path) {
        if (RythmPlugin.defaultEngine == RythmPlugin.EngineType.rythm || path.contains("rythm")) return true;
        if (Play.mode == Play.Mode.DEV) {
            Method m = getActionMethod(path);
            if (null != m) {
                if (m.getAnnotation(UseSystemTemplateEngine.class) != null) return false;
                if (m.getAnnotation(UseRythmTemplateEngine.class) != null) return true;
                Class<?> c = m.getDeclaringClass();
                if (c.getAnnotation(UseSystemTemplateEngine.class) != null) return false;
                if (c.getAnnotation(UseRythmTemplateEngine.class) != null) return true;
            }
            return false;
        }
        // strip off /app/views
        String templateRoot = RythmPlugin.templateRoot;
        int pos = path.indexOf(templateRoot);
        if (-1 != pos) path = path.substring(pos + templateRoot.length());
        // strip off leading slash
        while (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);
        // strip off file extension
        pos = path.lastIndexOf('.');
        if (-1 != pos) path = path.substring(0, pos);
        return whiteList.contains(path);
    } 
    
    static boolean blackListed(String path) {
        if (Play.mode == Play.Mode.DEV) {
            Method m = getActionMethod(path);
            if (null != m) {
                if (m.getAnnotation(UseSystemTemplateEngine.class) != null) return true;
                if (m.getAnnotation(UseRythmTemplateEngine.class) != null) return false;
                Class<?> c = m.getDeclaringClass();
                if (c.getAnnotation(UseSystemTemplateEngine.class) != null) return true;
                if (c.getAnnotation(UseRythmTemplateEngine.class) != null) return false;
            }
            return false;
        }
        return blackList.contains(path);
    }
    
    private static void scanTagFolder(VirtualFile root) {
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
                    TemplateClass templateClass = RythmPlugin.engine.classes.getByTemplate(resource.getKey());
                    if (null == templateClass) {
                        templateClass = new TemplateClass(resource, RythmPlugin.engine);
                    }
                    ITag tag = (ITag)templateClass.asTemplate();
                    if (null != tag)RythmPlugin.engine.registerTag(tag);
                } catch (Exception e) {
                    Logger.warn(e, "error loading tag: %s", f.relativePath());
                    // might be groovy template, let's ignore it
                }
            }
        }
        new FileTraversal().traverse(root);
    }
    
    static void scanTagFolder() {
        RythmPlugin.trace("start to scan tags");
        long ts = System.currentTimeMillis();
        String s = RythmPlugin.tagRoot;
        for (VirtualFile root: Play.roots) {
            VirtualFile tagRoot = root.child(s);
            if (!tagRoot.isDirectory()) continue;
            scanTagFolder(tagRoot);
        }
        ts = System.currentTimeMillis() - ts;
        RythmPlugin.trace("%sms to scan tags", ts);
    }
    
    static void buildBlackWhiteList() {
        if (Play.mode == Play.Mode.DEV) return;
        RythmPlugin.trace("start to build black and white list");
        long ts = System.currentTimeMillis();
        List<ApplicationClasses.ApplicationClass> controllers = Play.classes.getAssignableClasses(Controller.class);
        for (ApplicationClasses.ApplicationClass ac: controllers) {
            Class<?> c = ac.javaClass;
            String sCls = c.getName().replace("controllers.", "").replace('.', '/');
            Method[] ma = c.getMethods();
            for (Method m: ma) {
                int flag = m.getModifiers();
                if (!Modifier.isStatic(flag) || Modifier.isAbstract(flag) || !m.getReturnType().equals(Void.TYPE)) {
                    // action method is non-abstract static void
                    continue;
                }
                UseRythmTemplateEngine ar = m.getAnnotation(UseRythmTemplateEngine.class);
                boolean useRythm = ar != null;
                UseSystemTemplateEngine as = m.getAnnotation(UseSystemTemplateEngine.class);
                boolean useSystem = as != null;
                if (!useRythm && !useSystem) {
                    // no annotation found, check class annotation
                    useRythm  = c.getAnnotation(UseRythmTemplateEngine.class) != null;
                    useSystem = c.getAnnotation(UseSystemTemplateEngine.class) != null;

                    if (!useRythm && !useSystem) continue; // no annotation found on class either, use RythmPlugin default configuration
                    if (useRythm && useSystem) {
                        Logger.warn("Both UseRythmTemplateEngine and UseSystemTemplateEngine found on class [%s]. You should choose only one. System template engine will be used", c.getName());
                        useRythm = false;
                    }
                }
                if (useRythm && useSystem) {
                    Logger.warn("Both UseRythmTemplateEngine and UseSystemTemplateEngine found on method [%s]. You should choose only one. System template engine will be used", m.toString());
                    useRythm = false;
                }
                String path = sCls + "/" + m.getName();
                if (useRythm) {
                    RythmPlugin.trace("adding %s to white list", path);
                    whiteList.add(path);
                } else {
                    RythmPlugin.trace("adding %s to black list", path);
                    blackList.add(path);
                }
            }
        }
        ts = System.currentTimeMillis() - ts;
        RythmPlugin.trace("%sms to build black and white list", ts);
    }

    public static Template loadTemplate(VirtualFile file) {
        String path = file.relativePath();
RythmPlugin.info("loading template from virtual file: %s", file.relativePath());

        RythmTemplate rt = cache.get(path);
        if (null != rt) {
            rt.refresh(); // check if the resource is still valid
            return rt.isValid() ? rt : null;
        }
        
        // load template from the virtual file
        ITemplateResource resource = resourceLoader.load(file);
RythmPlugin.info("loaded template resource: %s", null == resource ? null : resource.getKey());
        if (null == resource || !resource.isValid()) return null;
        
        // are we already started?
        if (!Play.started) {
            // we can't load real template at precompile time because we pobably needs application to
            // register implicit variables
RythmPlugin.info("Play not started, return void template");
            return RythmPlugin.VOID_TEMPLATE;
        }
RythmPlugin.info("Play started, template returned");

        RythmTemplate tc = new RythmTemplate(resource);
        if (tc.isValid()) {
            cache.put(file.relativePath(), tc);
        } else {
            tc = null;
        }
                
        return tc;
    }

    public static void main(String[] args) {
        String path = "route";
        int dot = path.lastIndexOf('.');
        if (-1 == dot) path = path + ".rythm";
        else path = path.substring(0, dot) + ".rythm" + path.substring(dot);
        System.out.println(path);
    }
}
