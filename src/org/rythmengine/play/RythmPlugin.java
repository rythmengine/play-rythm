package org.rythmengine.play;

import org.rythmengine.Rythm;
import org.rythmengine.RythmEngine;
import org.rythmengine.exception.RythmException;
import org.rythmengine.extension.*;
import org.rythmengine.internal.IParserFactory;
import org.rythmengine.internal.dialect.SimpleRythm;
import org.rythmengine.logger.ILogger;
import org.rythmengine.play.parsers.*;
import org.rythmengine.play.utils.ActionInvokeProcessor;
import org.rythmengine.play.utils.PlayI18nMessageResolver;
import org.rythmengine.play.utils.StaticRouteResolver;
import org.rythmengine.play.utils.TemplateClassAppEnhancer;
import org.rythmengine.template.ITag;
import org.rythmengine.template.ITemplate;
import org.rythmengine.template.TemplateBase;
import org.rythmengine.toString.ToStringOption;
import org.rythmengine.toString.ToStringStyle;
import org.rythmengine.utils.S;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.classloading.enhancers.ControllersEnhancer;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Scope;
import play.mvc.results.*;
import play.templates.*;
import play.vfs.VirtualFile;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RythmPlugin extends PlayPlugin {

    public static final String VERSION = "1.0-b9l";

    public static final String R_VIEW_ROOT = "app/rythm";

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

    public static boolean underscoreImplicitVariableName = false;
    public static boolean refreshOnRender = true;
    public static String templateRoot = R_VIEW_ROOT;

    public static boolean enableCodeMarker = false;
    public static String jquery = "http://code.jquery.com/jquery-1.9.1.min.js";
    public static boolean fontawesome = false;
    //public static String templateRoot2 = R_VIEW_ROOT;
    //public static String tagRoot = "app/views/tags/rythm";

    public static List<ImplicitVariables.Var> implicitRenderArgs = new ArrayList<ImplicitVariables.Var>();

    public static VirtualFileTemplateResourceLoader resourceLoader;

    public static void registerImplicitRenderArg(final String name, final String type) {
        implicitRenderArgs.add(new ImplicitVariables.Var(name, type) {
            @Override
            protected Object evaluate() {
                return Scope.RenderArgs.current().get(name());
            }
        });
    }

    public static void loadTemplatePaths() {
        for (VirtualFile mroot : Play.modules.values()) {
            VirtualFile mviews = mroot.child(R_VIEW_ROOT);
            if (mviews.exists()) {
                Play.templatesPath.add(0, mviews);
            }
        }
        VirtualFile rythm = VirtualFile.open(Play.applicationPath).child(R_VIEW_ROOT);
        if (rythm.exists()) {
            Play.templatesPath.add(0, rythm);
        }
    }

    private boolean loadingRoute = false;

    public static boolean precompiling() {
        return System.getProperty("precompile") != null;
    }

    @Override
    public void onLoad() {
        if (null != engine) {
            engine.shutdown();
        }
        loadTemplatePaths();
        StaticRouteResolver.loadStaticRoutes();
        if (!precompiling()) {
            Play.lazyLoadTemplates = true;
        }
    }

    private boolean logActionInvocationTime;
    private ApplicationClassloader playAppClassLoader = new ApplicationClassloader() {
        private ApplicationClassloader pcl() {
            return Play.classloader;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return pcl().loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            if (name.endsWith(".class")) {
                String base;
                if (Play.usePrecompiled) {
                    base = "precompiled";
                } else {
                    base = "tmp";
                }
                File file = new File(Play.applicationPath, base + "/java/" + name);
                if (file.exists() && file.canRead()) {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new UnexpectedException(e);
                    }
                }
                return pcl().getParent().getResource(name);
            } else {
                return pcl().getResource(name);
            }
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return pcl().getResources(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return pcl().getResourceAsStream(name);
        }

        public Class<?> loadApplicationClass(String name) {
            try {
                return pcl().loadApplicationClass(name);
            } catch (SecurityException se) {
                return null;
            } catch (RythmException re) {
                if (re.getCause() instanceof SecurityException) {
                    return null;
                }
                throw re;
            }
        }
    };

    @Override
    public void onConfigurationRead() {
        if (null != engine && Play.mode.isProd()) return; // already configured
        if (null != engine && preloadConf && Play.mode.isDev()) {
            // the following load conf are caused by app restart at dev mode
            preloadConf = false;
            return;
        }

        Properties playConf = Play.configuration;

        // workaround for https://play.lighthouseapp.com/projects/57987-play-framework/tickets/1614-calling-to-plugins-beforeactioninvocation-and-afteractioninvocation-should-be-symmetric
        logActionInvocationTime = Boolean.parseBoolean(Play.configuration.getProperty("betterlogs.trace.actionInvocation.time", Play.mode.isDev() ? "true" : "false"));
        // eof workaround for https://play.lighthouseapp.com/projects/57987-play-framework/tickets/1614-calling-to-plugins-beforeactioninvocation-and-afteractioninvocation-should-be-symmetric

        // special configurations
        underscoreImplicitVariableName = Boolean.parseBoolean(playConf.getProperty("rythm.implicitVariable.underscore", "false"));
        refreshOnRender = Boolean.parseBoolean(playConf.getProperty("rythm.resource.refreshOnRender", "true"));

        enableCodeMarker = Play.mode.isDev() && Boolean.parseBoolean(playConf.getProperty("rythm.enableCodeMarker", "false"));
        jquery = playConf.getProperty("rythm.jquery", "http://code.jquery.com/jquery-1.9.1.min.js");
        fontawesome = Boolean.parseBoolean(playConf.getProperty("rythm.fontawesome", "false"));

        Properties p = new Properties();

        // set default configurations
        // p.put("rythm.root", new File(Play.applicationPath, "app/views"));
        // p.put("rythm.tag.root", new File(Play.applicationPath, tagRoot));
        final boolean isProd = Play.mode.isProd();
        p.put("rythm.engine.mode", isProd ? Rythm.Mode.prod : Rythm.Mode.dev);
        p.put("rythm.engine.plugin.version", VERSION);
        p.put("rythm.codegen.compact.enabled", isProd);
        p.put("rythm.engine.class_loader.parent", playAppClassLoader);
        p.put("rythm.engine.load_precompiled.enabled", Play.usePrecompiled);
        p.put("rythm.log.source.template.enabled", true);
        p.put("rythm.log.source.java.enabled", true);
        p.put("rythm.engine.precompile_mode.enabled", Play.mode.isProd() && System.getProperty("precompile") != null);
        if (Play.usePrecompiled || Play.getFile("precompiled").exists()) {
            File preCompiledRoot = new File(Play.getFile("precompiled"), "rythm");
            if (!preCompiledRoot.exists()) preCompiledRoot.mkdirs();
            p.put("rythm.home.precompiled", preCompiledRoot);
        }
        resourceLoader = new VirtualFileTemplateResourceLoader();
        p.put("rythm.resource.loader.impls", resourceLoader);
        p.put("rythm.resource.loader.def.enabled", false);
        p.put("rythm.resource.name.suffix", "");
        p.put("rythm.engine.class_loader.byte_code_helper", new IByteCodeHelper() {
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
        p.put("rythm.log.factory", new ILoggerFactory() {
            @Override
            public ILogger getLogger(Class<?> clazz) {
                return PlayRythmLogger.instance;
            }
        });
        p.put("rythm.feature.transformer.enabled", true); // enable java extension by default

        p.put("rythm.cache.prod_only", "true");
        p.put("rythm.default.cache_ttl", 60 * 60);
        p.put("rythm.cache.service", new ICacheService() {
            private int defaultTTL = 60 * 60;

            @Override
            public void startup() {
            }

            @Override
            public void put(String key, Serializable value, int ttl) {
                Cache.cacheImpl.set(key, value, ttl);
            }

            @Override
            public void put(String key, Serializable value) {
                Cache.cacheImpl.set(key, value, defaultTTL);
            }

            @Override
            public Serializable remove(String key) {
                Object o = Cache.get(key);
                Cache.delete(key);
                return null == o ? null : (o instanceof Serializable ? (Serializable) o : o.toString());
            }

            @Override
            public Serializable get(String key) {
                Object o = Cache.get(key);
                return null == o ? null : (o instanceof Serializable ? (Serializable) o : o.toString());
            }

            @Override
            public boolean contains(String key) {
                Object o = Cache.get(key);
                return null != o;
            }

            @Override
            public void clear() {
                try {
                    Cache.clear();
                } catch (Throwable e) {
                    //ignore it
                }
            }

            @Override
            public void setDefaultTTL(int ttl) {
                defaultTTL = ttl;
            }

            @Override
            public void shutdown() {
                clear();
            }
        });

        // set user configurations - coming from application.conf
        for (String key : playConf.stringPropertyNames()) {
            if (key.startsWith("rythm.")) {
                p.setProperty(key, playConf.getProperty(key));
            }
        }
        debug("User defined rythm properties configured");

        // restricted class in sandbox mode
        String appRestricted = p.getProperty("rythm.sandbox.restricted_classes", "");
        appRestricted += ";play.Play;play.classloading;play.server";
        p.setProperty("rythm.sandbox.restricted_classes", appRestricted);

        // set template root
        templateRoot = p.getProperty("rythm.home.template", templateRoot);
        p.put("rythm.home.template", new File(Play.applicationPath, templateRoot));
        if (Logger.isDebugEnabled()) debug("rythm template root set to: %s", p.get("rythm.home.template"));

//        // set tag root
//        tagRoot = p.getProperty("rythm.tag.root", tagRoot);
//        if (tagRoot.endsWith("/")) tagRoot = tagRoot.substring(0, tagRoot.length() - 1);
//        p.put("rythm.tag.root", new File(Play.applicationPath, tagRoot));
//        if (Logger.isDebugEnabled()) debug("rythm tag root set to %s", p.get("rythm.tag.root"));

        // set tmp dir
        debug("Play standalone play server? %s", Play.standalonePlayServer);
        //boolean isGaePresent = Boolean.valueOf(p.getProperty("rythm.gae", "false")) ;
        boolean isGaePresent = isGaeSdkInClasspath();
        if (isGaePresent) {
            warn("GAE SDK present in the classpath");
        }
        boolean gae = !Play.standalonePlayServer && isGaePresent;
        boolean readOnly = gae || Boolean.valueOf(p.getProperty("rythm.engine.file_write", "false"));
        if (!readOnly) {
            File tmpDir = new File(Play.tmpDir, "rythm");
            tmpDir.mkdirs();
            p.put("rythm.home.tmp", tmpDir);
            if (Logger.isDebugEnabled()) {
                debug("rythm tmp dir set to %s", p.get("rythm.home.tmp"));
            }
        } else if (gae) {
            warn("GAE enabled");
        }

        p.put("rythm.engine.mode", Play.mode.isDev() && Play.standalonePlayServer ? Rythm.Mode.dev : Rythm.Mode.prod);
        p.put("rythm.engine.playframework", true);

        p.put("rythm.render.listener", new IRythmListener.ListenerAdaptor() {
            @Override
            public void onInvoke(ITag tag) {
                RythmTagContext.enterTag(tag.__getName());
            }

            @Override
            public void invoked(ITag tag) {
                RythmTagContext.exitTag();
            }
        });

        final TemplatePropertiesEnhancer templateEnhancer = new TemplatePropertiesEnhancer();
        p.put("rythm.codegen.byte_code_enhancer", new IByteCodeEnhancer() {
            @Override
            public byte[] enhance(String className, byte[] classBytes) throws Exception {
                if (engine.conf().disableFileWrite()) return classBytes;
                ApplicationClasses.ApplicationClass applicationClass = new ApplicationClasses.ApplicationClass();
                applicationClass.javaByteCode = classBytes;
                applicationClass.enhancedByteCode = classBytes;
                File f = File.createTempFile("rythm_", className.contains("$") ? "$1" : "" + ".java", Play.tmpDir);
                applicationClass.javaFile = VirtualFile.open(f);
                try {
                    templateEnhancer.enhanceThisClass(applicationClass);
                } catch (Exception e) {
                    error(e, "Error enhancing class: %s", className);
                }
                if (!f.delete()) f.deleteOnExit();
                return applicationClass.enhancedByteCode;
            }
        });

        p.put("rythm.codegen.source_code_enhancer", new ISourceCodeEnhancer() {
            @Override
            public List<String> imports() {
                List<String> l = new ArrayList(Arrays.asList(TemplateClassAppEnhancer.imports().split("[,\n]+")));
                l.add(JavaExtensions.class.getName());
                l.add("models.*");
                l.add("controllers.*");
                return l;
            }

            @Override
            public String sourceCode() {
                String prop = "\n\tprotected <T> T _getBeanProperty(Object o, String prop) {"
                        + "\n\t\treturn (T)org.rythmengine.play.utils.JavaHelper.getProperty(o, prop);"
                        + "\n\t}\n"
                        + "\n\tprotected void _setBeanProperty(Object o, String prop, Object val) {"
                        + "\n\t\torg.rythmengine.play.utils.JavaHelper.setProperty(o, prop, val);"
                        + "\n\t}\n"
                        + "\n\tprotected boolean _hasBeanProperty(Object o, String prop) {"
                        + "\n\t\treturn org.rythmengine.play.utils.JavaHelper.hasProperty(o, prop);"
                        + "\n\t}\n";
                String url = "\n    protected play.mvc.Router.ActionDefinition _act(String action, Object... params) {return _act(false, action, params);}" +
                        "\n    protected play.mvc.Router.ActionDefinition _act(boolean isAbsolute, String action, Object... params) {" +
                        "\n        org.rythmengine.internal.compiler.TemplateClass tc = __getTemplateClass(true);" +
                        "\n        boolean escapeXML = (!tc.isStringTemplate() && tc.templateResource.getKey().toString().endsWith(\".xml\"));" +
                        "\n        return new org.rythmengine.play.utils.ActionBridge(isAbsolute, escapeXML).invokeMethod(action, params);" +
                        "\n   }\n" +
                        "\n    protected String _url(String action, Object... params) {return _url(false, action, params);}" +
                        "\n    protected String _url(boolean isAbsolute, String action, Object... params) { return _act(isAbsolute, action, params).toString();" +
                        "\n   }\n";

                String msg = "\n    protected String _msg(String key, Object ... params) {return play.i18n.Messages.get(key, params);}";
                // add String _url(String) method to template class
                return prop + msg + url + TemplateClassAppEnhancer.sourceCode();
            }

            @Override
            public Map<String, ?> getRenderArgDescriptions() {
                Map<String, Object> m = new HashMap<String, Object>();
                // App registered render args
                for (ImplicitVariables.Var var : implicitRenderArgs) {
                    m.put(var.name(), var.type);
                }
                // Play default render args
                for (ImplicitVariables.Var var : ImplicitVariables.vars) {
                    m.put(var.name(), var.type);
                }
                return m;
            }

            @Override
            public void setRenderArgs(ITemplate template) {
                Map<String, Object> m = new HashMap<String, Object>();
                for (ImplicitVariables.Var var : ImplicitVariables.vars) {
                    m.put(var.name(), var.evaluate());
                }
                template.__setRenderArgs(m);
            }
        });

        p.put("rythm.render.exception_handler", new IRenderExceptionHandler() {
            @Override
            public boolean handleTemplateExecutionException(Exception e, TemplateBase template) {
                boolean handled = false;
                if (e instanceof RenderTemplate) {
                    template.p(((RenderTemplate) e).getContent());
                } else if (e instanceof RenderHtml || e instanceof RenderJson || e instanceof RenderStatic || e instanceof RenderXml || e instanceof RenderText) {
                    Http.Response resp = new Http.Response();
                    resp.out = new ByteArrayOutputStream();
                    ((Result) e).apply(null, resp);
                    try {
                        template.p(resp.out.toString("utf-8"));
                    } catch (UnsupportedEncodingException e0) {
                        throw new UnexpectedException("utf-8 not supported?");
                    }
                }
                if (handled) {
                    // allow next controller action call
                    ControllersEnhancer.ControllerInstrumentation.initActionCall();
                    resetActionCallFlag();
                    return true;
                }
                return false;
            }
        });

        p.put("rythm.i18n.message.resolver", new PlayI18nMessageResolver());
        p.put("rythm.resource.autoScan", false);

        if (null != engine) {
            engine.shutdown();
        }

        engine = new RythmEngine(p);
        //Rythm.engine.cacheService.shutdown();
        ///Rythm.init(engine);

        IParserFactory[] factories = {new AbsoluteUrlReverseLookupParser(), new UrlReverseLookupParser(),
                new MessageLookupParser(), new GroovyVerbatimTagParser(), new ExitIfNoModuleParser()};
        engine.extensionManager().registerUserDefinedParsers(factories).registerUserDefinedParsers(SimpleRythm.ID, factories).registerExpressionProcessor(new ActionInvokeProcessor());
        debug("Play specific parser registered");

        FastTagBridge.registerFastTags(engine);
        registerJavaTags(engine);
        ActionTagBridge.registerActionTags(engine);
        if (engine.conf().transformEnabled()) {
            JavaExtensionBridge.registerPlayBuiltInJavaExtensions(engine);
            JavaExtensionBridge.registerAppJavaExtensions(engine);
        }

        RythmTemplateLoader.clear();
        if (engine.conf().gae()) {
            TemplateLoader.getAllTemplate();
        }
    }

    public static boolean isGaeSdkInClasspath() {
        try {
            String classname = "com.google.appengine.api.LifecycleManager";
            Class clazz = Class.forName(classname);
            return clazz != null;
        } catch (Throwable t) {
            // Nothing to do
        }
        return false;
    }

    public static final String ACTION_CALL_FLAG_KEY = "__RYTHM_PLUGIN_ACTION_CALL_";

    public static void resetActionCallFlag() {
        Stack<Boolean> actionCalls = Scope.RenderArgs.current().get(ACTION_CALL_FLAG_KEY, Stack.class);
        if (null != actionCalls) {
            actionCalls.pop();
        }
    }

    public static void setActionCallFlag() {
        Scope.RenderArgs renderAargs = Scope.RenderArgs.current();
        Stack<Boolean> actionCalls = renderAargs.get(ACTION_CALL_FLAG_KEY, Stack.class);
        if (null == actionCalls) {
            actionCalls = new Stack<Boolean>();
            renderAargs.put(ACTION_CALL_FLAG_KEY, actionCalls);
        }
        actionCalls.push(true);
    }

    public static boolean isActionCall() {
        Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
        if (null == renderArgs) {
            // calling from Mails?
            return false;
        }
        Stack<Boolean> actionCalls = Scope.RenderArgs.current().get(ACTION_CALL_FLAG_KEY, Stack.class);
        if (null == actionCalls || actionCalls.empty()) return false;
        return true;
    }

    @Override
    public void afterApplicationStart() {
        if (engine.mode().isProd()) {
            // pre load template classes if they are not loaded yet
            VirtualFile vf = Play.getVirtualFile("app/rythm/welcome.html");
            String key = vf.relativePath().replaceFirst("\\{.*?\\}", "");
            if (!engine.classes().tmplIdx.containsKey(key) || engine.conf().loadPrecompiled()) {
                RythmTemplateLoader.scanRythmFolder();
            }
        } else {
            //RythmTemplateLoader.scanRythmFolder();
        }
    }

    private void registerJavaTags(RythmEngine engine) {
        long l = System.currentTimeMillis();
        // -- register application java tags
        List<ApplicationClasses.ApplicationClass> classes = Play.classes.getAssignableClasses(FastRythmTag.class);
        for (ApplicationClasses.ApplicationClass ac : classes) {
            registerJavaTag(ac.javaClass, engine);
        }

        // -- register PlayRythm build-in tags
        Class<?>[] ca = FastRythmTags.class.getDeclaredClasses();
        for (Class<?> c : ca) {
            registerJavaTag(c, engine);
        }
        debug("%sms to register rythm java tags", System.currentTimeMillis() - l);
    }

    private void registerJavaTag(Class<?> jc, RythmEngine engine) {
        int flag = jc.getModifiers();
        if (Modifier.isAbstract(flag)) return;
        try {
            Constructor<?> c = jc.getConstructor(new Class[]{});
            c.setAccessible(true);
            FastRythmTag tag = (FastRythmTag) c.newInstance();
            engine.registerTemplate(tag);
        } catch (Exception e) {
            throw new UnexpectedException("Error initialize JavaTag: " + jc.getName(), e);
        }
    }

    public static final Template VOID_TEMPLATE = new Template() {
        @Override
        public void compile() {
            //
        }

        @Override
        protected String internalRender(Map<String, Object> args) {
            throw new UnexpectedException("It's not supposed to be called");
        }
    };

    private boolean preloadConf = false;

    @Override
    public Template loadTemplate(VirtualFile file) {
        if (loadingRoute) return null;
        if (null == engine) {
            preloadConf = true;
            // in prod mode this method is called in preCompile() when onConfigurationRead() has not been called yet
            onConfigurationRead();
            StaticRouteResolver.processVersionedRoutes();
        }
        //warn(">>>> %s", file.relativePath());
        if (precompiling()) {
            try {
                return RythmTemplateLoader.loadTemplate(file);
            } catch (Throwable e) {
                error(e, "Error precompiling template: %s", file);
                return null;
            }
        } else {
            return RythmTemplateLoader.loadTemplate(file);
        }
    }

    @Override
    public void detectChange() {
        if (!refreshOnRender) engine.classLoader().detectChanges();
    }

    private Map<Class<? extends ICacheKeyProvider>, ICacheKeyProvider> keyProviders = new HashMap<Class<? extends ICacheKeyProvider>, ICacheKeyProvider>();

    @Override
    public void beforeActionInvocation(Method actionMethod) {
        TagContext.init();
        if (Play.mode.isDev() && Boolean.valueOf(Play.configuration.getProperty("rythm.cache.prodOnly", "true"))) {
            return;
        }
        Http.Request request = Http.Request.current();
        Cache4 cache4 = actionMethod.getAnnotation(Cache4.class);
        if (null == cache4) {
            return;
        }
        if (logActionInvocationTime) {
            Logger.info("");
            Logger.info("[BL]>>>>>>> [%s]", Http.Request.current().action);
            Http.Request.current().args.put("__BL_COUNTER__", System.currentTimeMillis());
        }
        String m = request.method;
        if ("GET".equals(m) || "HEAD".equals(m) || (cache4.cachePost() && "POST".equals(m))) {
            String cacheKey = cache4.id();
            boolean sessSensitive = cache4.sessionSensitive();
            if (!sessSensitive) {
                sessSensitive = cache4.useSessionData();
            }
            boolean schemeSensitive = cache4.schemeSensitive();
            boolean langSensitive = cache4.langSensitive();
            if (S.isEmpty(cacheKey)) {
                Class<? extends ICacheKeyProvider> kpFact = cache4.key();
                try {
                    ICacheKeyProvider keyProvider = keyProviders.get(kpFact);
                    if (null == keyProvider) {
                        keyProvider = kpFact.newInstance();
                        keyProviders.put(kpFact, keyProvider);
                    }
                    cacheKey = keyProvider.getKey(sessSensitive, schemeSensitive, langSensitive);
                    if (S.isEmpty(cacheKey)) {
                        //warn("empty cache key found");
                        return;
                    }
                } catch (Exception e) {
                    error(e, "error get key from key provider");
                    return;
                }
                // Note we cannot do any transform on user supplied key
                // as it might be used to deprecate cache. So we will leave
                // the cache key provider to handle session and scheme

                //if (cache4.useSessionData()) {
                //cacheKey = cacheKey + Scope.Session.current().toString();
                //}
                //if (cache4.schemeSensitive()) cacheKey += request.secure;
            } else {
                if (sessSensitive) {
                    cacheKey = cacheKey + Scope.Session.current().toString();
                }
                if (schemeSensitive) {
                    cacheKey += request.secure;
                }
                if (langSensitive) {
                    cacheKey += Lang.get();
                }
            }
            request.args.put("rythm-urlcache-key", cacheKey);
            request.args.put("rythm-urlcache-actionMethod", actionMethod);
            Result result = (Result) play.cache.Cache.get(cacheKey);
            if (null == result) return;
            if (!(result instanceof Cache4.CacheResult)) {
                result = new Cache4.CacheResult(cacheKey, result);
            }
            throw result;
        }
    }

    @Override
    public void onActionInvocationResult(Result result) {
        if (result instanceof Cache4.CacheResult) {
            // it's already a cached result
            return;
        }
        if (result instanceof Redirect) {
            Redirect r = (Redirect) result;
            if (r.code != Http.StatusCode.MOVED) {
                // not permanent redirect, don't cache it
                return;
            }
        }
        if (result instanceof NotFound || result instanceof play.mvc.results.Error) {
            // might recover later, so don't cache it
            return;
        }
        Object o = Http.Request.current().args.get("rythm-urlcache-key");
        if (null == o) return;
        String cacheKey = o.toString();
        Method actionMethod = (Method) Http.Request.current().args.get("rythm-urlcache-actionMethod");
        String duration = actionMethod.getAnnotation(Cache4.class).value();
        if (S.isEmpty(duration)) duration = "1h";
        if (duration.startsWith("cron.")) {
            duration = Play.configuration.getProperty(duration, "1h");
        }
        if ("forever".equals(duration)) {
            duration = "99999d";
        }
        play.cache.Cache.set(cacheKey, new Cache4.CacheResult(cacheKey, result), duration);
    }

    @Override
    public String getMessage(String locale, Object key, Object... args) {
        String value = null;
        if (key == null) {
            return "";
        }
        Map<String, Properties> locales = Messages.locales;
        String k = key.toString();
        if (locales.containsKey(locale)) {
            value = locales.get(locale).getProperty(k);
        }
        if (value == null) {
            int pos = locale.indexOf('_');
            if (pos > -1) {
                locale = locale.substring(0, pos);
                value = locales.get(locale).getProperty(k);
            }
            if (value == null) {
                value = Messages.defaults.getProperty(key.toString());
            }
        }
        if (value == null) {
            value = key.toString();
        }

        return Messages.formatString(value, args);
    }

    private static RythmEngine engine() {
        return engine;
    }

    // ----- render interfaces ---------

    /**
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#render(String, Object...)
     */
    public static String render(String template, Object... args) {
        return engine().render(template, args);
    }

    /**
     * @param file
     * @param args
     * @return render result
     * @see RythmEngine#render(java.io.File, Object...)
     */
    public static String render(File file, Object... args) {
        return engine().render(file, args);
    }

    public static String render(VirtualFile file, Object... args) {
        return engine().render(file.getRealFile(), args);
    }

    /**
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#substitute(String, Object...)
     */
    public static String substitute(String template, Object... args) {
        return engine().substitute(template, args);
    }

    /**
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#substitute(java.io.File, Object...)
     */
    public static String substitute(File template, Object... args) {
        return engine().substitute(template, args);
    }

    /**
     * @param template
     * @param obj
     * @return render result
     * @see RythmEngine#toString(String, Object)
     */
    public static String toString(String template, Object obj) {
        return engine().toString(template, obj);
    }

    /**
     * @param obj
     * @return render result
     * @see RythmEngine#toString(Object)
     */
    public static String toString(Object obj) {
        return engine().toString(obj);
    }

    /**
     * @param obj
     * @param option
     * @param style
     * @return render result
     * @see RythmEngine#toString(Object, org.rythmengine.toString.ToStringOption, org.rythmengine.toString.ToStringStyle)
     */
    public static String toString(Object obj, ToStringOption option, ToStringStyle style) {
        return engine().toString(obj, option, style);
    }

    /**
     * @param obj
     * @param option
     * @param style
     * @return render result
     * @see RythmEngine#commonsToString(Object, org.rythmengine.toString.ToStringOption, org.apache.commons.lang3.builder.ToStringStyle)
     */
    public static String commonsToString(Object obj, ToStringOption option, org.apache.commons.lang3.builder.ToStringStyle style) {
        return engine().commonsToString(obj, option, style);
    }

    /**
     * Alias of {@link #renderString(String, Object...)}
     *
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#renderString(String, Object...)
     */
    public static String renderStr(String template, Object... args) {
        return engine().renderString(template, args);
    }

    /**
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#renderString(String, Object...)
     */
    public static String renderString(String template, Object... args) {
        return engine().renderString(template, args);
    }

    /**
     * @param template
     * @param args
     * @return render result
     * @see RythmEngine#renderIfTemplateExists(String, Object...)
     */
    public static String renderIfTemplateExists(String template, Object... args) {
        return engine().renderIfTemplateExists(template, args);
    }

}
