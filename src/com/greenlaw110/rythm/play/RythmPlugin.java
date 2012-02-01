package com.greenlaw110.rythm.play;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.greenlaw110.rythm.play.parsers.AbsoluteUrlReverseLookupParser;
import com.greenlaw110.rythm.play.parsers.UrlReverseLookupParser;
import com.greenlaw110.rythm.spi.IParserFactory;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.exceptions.ConfigurationException;
import play.exceptions.UnexpectedException;
import play.templates.Template;
import play.vfs.VirtualFile;

import com.greenlaw110.rythm.IByteCodeHelper;
import com.greenlaw110.rythm.Rythm;
import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.logger.ILogger;
import com.greenlaw110.rythm.logger.ILoggerFactory;
import com.greenlaw110.rythm.spi.ITemplateClassEnhancer;
import com.greenlaw110.rythm.template.ITemplate;
import com.greenlaw110.rythm.utils.IRythmListener;

public class RythmPlugin extends PlayPlugin {
    public static final String VERSION = "0.2";

    public static void info(String msg, Object... args) {
        Logger.info(msg_(msg, args));
    }
    
    public static void info(Throwable t, String msg, Object... args) {
        Logger.info(t, msg_(msg, args));
    }
    
    public static void debug(String msg, Object... args) {
        Logger.debug(msg_(msg, args));
    }
    
    public static void debug(Throwable t, String msg, Object... args) {
        Logger.debug(t, msg_(msg, args));
    }
    
    public static void trace(String msg, Object... args) {
        Logger.trace(msg_(msg, args));
    }
    
    public static void trace(Throwable t, String msg, Object... args) {
        Logger.warn(t, msg_(msg, args));
    }

    public static void warn(String msg, Object... args) {
        Logger.warn(msg_(msg, args));
    }

    public static void warn(Throwable t, String msg, Object... args) {
        Logger.warn(t, msg_(msg, args));
    }

    public static void error(String msg, Object... args) {
        Logger.error(msg_(msg, args));
    }

    public static void error(Throwable t, String msg, Object... args) {
        Logger.error(t, msg_(msg, args));
    }

    public static void fatal(String msg, Object... args) {
        Logger.fatal(msg_(msg, args));
    }

    public static void fatal(Throwable t, String msg, Object... args) {
        Logger.fatal(t, msg_(msg, args));
    }

    private static String msg_(String msg, Object... args) {
        return String.format("RythmPlugin-" + VERSION + "> %1$s",
                String.format(msg, args));
    }

    public static RythmEngine engine;
    
    public static enum EngineType {
        rythm, system;
        public static EngineType parseEngineType(String s) {
            if ("rythm".equalsIgnoreCase(s)) return rythm;
            else if ("system".equalsIgnoreCase(s) || "groovy".equalsIgnoreCase(s)) return system;
            else {
                throw new ConfigurationException(String.format("unrecongized engine type[%s] found, please use either \"rythm\" or \"system\"", s));
            }
        }
    }

    public static EngineType defaultEngine = EngineType.system;
    public static boolean underscoreImplicitVariableName = false;
    public static boolean refreshOnRender = true;
    public static String templateRoot = "app/views";
    public static String tagRoot = "app/views/tags/rythm";
    
    @Override
    public void onConfigurationRead() {
        Properties playConf = Play.configuration;
        
        // special configurations
        defaultEngine = EngineType.parseEngineType(playConf.getProperty("rythm.default.engine", "system"));
        underscoreImplicitVariableName = Boolean.parseBoolean(playConf.getProperty("rythm.implicitVariable.underscore", "false"));
        refreshOnRender = Boolean.parseBoolean(playConf.getProperty("rythm.resource.refreshOnRender", "true"));

        Properties p = new Properties();

        // set default configurations
        // p.put("rythm.root", new File(Play.applicationPath, "app/views"));
        // p.put("rythm.tag.root", new File(Play.applicationPath, tagRoot));
        p.put("rythm.tag.autoscan", false); // we want to scan tag folder coz we have Virtual Filesystem
        p.put("rythm.classLoader.parent", Play.classloader);
        p.put("rythm.resource.refreshOnRender", "true");
        p.put("rythm.resource.loader", new VirtualFileTemplateResourceLoader());
        p.put("rythm.classLoader.byteCodeHelper", new IByteCodeHelper() {
            @Override
            public byte[] findByteCode(String typeName) {
                ApplicationClasses classBag = Play.classes;
                if (classBag.hasClass(typeName)) {
                    ApplicationClasses.ApplicationClass applicationClass = classBag.getApplicationClass(typeName);
                    return applicationClass.enhancedByteCode;
                } else {
                    return null;
                }
            }
        });
        p.put("rythm.logger.factory", new ILoggerFactory() {
            @Override
            public ILogger getLogger(Class<?> clazz) {
                return PlayRythmLogger.instance;
            }
        });
        // put implicit render args declarations
        // see http://www.playframework.org/documentation/1.2.4/templates#implicits
        Map<String, Object> m = new HashMap<String, Object>();
        for (ImplicitVariables.Var var: ImplicitVariables.vars) {
            m.put(var.name(), var.type);
        }
        p.put("rythm.defaultRenderArgs", m);

        // set user configurations - coming from application.conf
        for (String key: playConf.stringPropertyNames()) {
            if (key.startsWith("rythm.")) {
                p.setProperty(key, playConf.getProperty(key));
            }
        }
        
        // set template root
        templateRoot = p.getProperty("rythm.root", templateRoot);
        p.put("rythm.root", new File(Play.applicationPath, templateRoot));

        // set tag root
        tagRoot = p.getProperty("rythm.tag.root", tagRoot);
        if (tagRoot.endsWith("/")) tagRoot = tagRoot.substring(0, tagRoot.length() - 1);
        p.put("rythm.tag.root", new File(Play.applicationPath, tagRoot));
        
        if (Play.Mode.PROD == Play.mode) p.put("rythm.mode", Rythm.Mode.prod);

        if (null == engine) {
            engine = new RythmEngine(p);
            engine.registerListener(new IRythmListener() {
                @Override
                public void onRender(ITemplate template) {
                    Map<String, Object> m = new HashMap<String, Object>();
                    for (ImplicitVariables.Var var: ImplicitVariables.vars) {
                        m.put(var.name(), var.evaluate());
                    }
                    template.setRenderArgs(m);
                }
            });
            engine.registerTemplateClassEnhancer(new ITemplateClassEnhancer() {
                @Override
                public byte[] enhance(String className, byte[] classBytes) throws  Exception {
                    ApplicationClasses.ApplicationClass applicationClass = new ApplicationClasses.ApplicationClass();
                    applicationClass.javaByteCode = classBytes;
                    applicationClass.enhancedByteCode = classBytes;
                    File f = File.createTempFile("rythm_", className.contains("$") ? "$1" : "" + ".java", Play.tmpDir);
                    applicationClass.javaFile = VirtualFile.open(f);
                    new TemplatePropertiesEnhancer().enhanceThisClass(applicationClass);
                    return applicationClass.enhancedByteCode;
                }
            });
            Rythm.engine = engine;

            IParserFactory[] factories = {new AbsoluteUrlReverseLookupParser(), new UrlReverseLookupParser()};
            engine.getExtensionManager().registerUserDefinedParsers(factories);
        } else {
            engine.init(p);
        }

        info("template engine initialized");
    }
    
    @Override
    public void onApplicationStart() {
        RythmTemplateLoader.buildBlackWhiteList();
        FastTagBridge.registerFastTags();
        registerJavaTags();
        RythmTemplateLoader.scanTagFolder();
    }
    
    private void registerJavaTags() {
        // -- register application java tags
        List<ApplicationClasses.ApplicationClass> classes = Play.classes.getAssignableClasses(FastRythmTag.class);
        for (ApplicationClasses.ApplicationClass ac: classes) {
            registerJavaTag(ac.javaClass);
        }
        
        // -- register PlayRythm build-in tags
        Class<?>[] ca = FastRythmTags.class.getDeclaredClasses();
        for (Class<?> c: ca) {
            registerJavaTag(c);
        }
    }
    
    private void registerJavaTag(Class<?> jc) {
        int flag = jc.getModifiers();
        if (Modifier.isAbstract(flag)) return;
        try {
            Constructor<?> c = jc.getConstructor(new Class[]{});
            c.setAccessible(true);
            FastRythmTag tag = (FastRythmTag)c.newInstance();
            engine.registerTag(tag);
        } catch (Exception e) {
            throw new UnexpectedException("Error initialize JavaTag: " + jc.getName(), e);
        }
    }

    @Override
    public Template loadTemplate(VirtualFile file) {
        if (null == engine) {
            // in prod mode this method is called in preCompile() when onConfigurationRead() has not been called yet
            onConfigurationRead();
        }
        return RythmTemplateLoader.loadTemplate(file);
    }

    @Override
    public void detectChange() {
        if (!refreshOnRender) engine.classLoader.detectChanges();
    }
}
