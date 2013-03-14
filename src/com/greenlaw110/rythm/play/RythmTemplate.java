package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.exception.CompileException;
import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.exception.RythmException;
import com.greenlaw110.rythm.extension.ICodeType;
import com.greenlaw110.rythm.internal.compiler.ClassReloadException;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.template.TemplateBase;
import com.greenlaw110.rythm.utils.TextBuilder;
import play.Logger;
import play.Play;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.mvc.results.Result;
import play.templates.Template;

import java.util.Locale;
import java.util.Map;

import static play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.*;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class RythmTemplate extends Template {

    private TemplateClass tc;
    private ICodeType codeType;

    RythmTemplate(ITemplateResource resource) {
        if (null == resource) throw new NullPointerException();
        tc = new TemplateClass(resource, RythmPlugin.engine, true);
        name = resource.getKey().toString();
        source = tc.templateResource.asTemplateContent();
        codeType = resource.codeType();
    }

    private RythmEngine engine() {
        return RythmPlugin.engine;
    }

    static class TemplateInfo extends Template {
        @Override
        public void compile() {
        }
        @Override
        protected String internalRender(Map<String, Object> args) {
            return null;
        }

        TemplateInfo(String name, String source, int lineNo) {
            this.source = source;
            this.lineNo = lineNo;
            this.name = name;
        }

        public int lineNo = -1;
    }

    void refresh() {
        refresh(false);
    }

    void refresh(boolean forceRefresh) {
        if (!forceRefresh && engine().isProdMode()) return;
        try {
            engine().classLoader().detectChange(tc);
        } catch (ClassReloadException e) {
            RythmPlugin.debug("restart rythm engine to reload changed template...");
            engine().restart(e);
            refresh(forceRefresh);
        } catch (RythmException e) {
            handleRythmException(e);
        } catch (RuntimeException e) {
            throw new UnexpectedException(String.format("Unknown error when refreshing rythm template: %s", tc.getKey()), e);
        }
        if (!tc.isValid) {
            RythmTemplateLoader.cache.remove(getName());
        } else {
            source = tc.templateResource.asTemplateContent();
        }
    }

    public boolean isValid() {
        return tc.isValid;
    }

    @Override
    public void compile() {
        if (RythmPlugin.precompiling()) {
            try {
                refresh();
                tc.asTemplate();
            } catch (Throwable e) {
                RythmPlugin.error(e, "Error precompiling template");
            }
        } else {
            refresh();
            tc.asTemplate();
        }
         
        //if (tc.isValid) tc.compile();
    }

    private static final ThreadLocal<Integer> refreshCounter = new ThreadLocal<Integer>();
    @Override
    protected String internalRender(Map<String, Object> args) {
        boolean isActionCallAllowed = false;
        try {
            isActionCallAllowed = isActionCallAllowed();
        } catch (NullPointerException e) {
            // rendering mail from an non action handler thread
            initActionCall();
            isActionCallAllowed = true;
        }
        try {
            RythmPlugin.engine.renderSettings.init(codeType, new Locale(Lang.get()));
            if (Logger.isTraceEnabled()) RythmPlugin.trace("prepare template to render");
            TemplateBase t = (TemplateBase)tc.asTemplate();
            if (Logger.isTraceEnabled()) RythmPlugin.trace("about to set render args");
            t.__setRenderArgs(args);
            // allow invoke controller method without redirect
            if (!isActionCallAllowed) initActionCall();
            // moved to RythmPlugin.beforeActionInvocation()
//            if (!RythmPlugin.isActionCall()) {
//                TagContext.init();
//            }
            if (Logger.isTraceEnabled()) RythmPlugin.trace("about to execute template");
            String s = t.render();
            if (!RythmPlugin.engine.isProdMode()) {
                refreshCounter.set(0);
            }
            if (Logger.isTraceEnabled()) RythmPlugin.trace("render completed");
            return s;
        } catch (RythmException e) {
            handleRythmException(e);
            return null; // honestly you will never arrive here
        } catch (Result e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateExecutionException(this, -1, e.getMessage(), e);
        } finally {
            if (!isActionCallAllowed) stopActionCall();
        }
    }

    private static enum _ErrType {parsing, compilation, runtime}
    static void handleRythmException(RythmException e) {
        _ErrType errType = _ErrType.runtime;
        if (e instanceof ParseException)  errType = _ErrType.parsing;
        else if (e instanceof CompileException) errType = _ErrType.compilation;
        
        if (Play.mode.isDev()) {
            TextBuilder tb = new TextBuilder();
            tb.p("rythm ").p(errType).p(" exception captured on [").p(e.getTemplateName()).p("]: ").pn(e.originalMessage);
            tb.pn(e.templateSourceInfo());
            tb.pn(e.javaSourceInfo());
            Logger.error(tb.toString());
        }
        int line = e.templateLineNumber;
        TemplateInfo t;
        if (-1 == line) {
            line = e.javaLineNumber;
            t = new TemplateInfo(e.getTemplateName(), e.getJavaSource(), line);
        } else {
            t = new TemplateInfo(e.getTemplateName(), e.getTemplateSource(), line);
        }

        switch (errType) {
            case parsing: throw new TemplateParseException(t, t.lineNo, e.originalMessage);
            case compilation: throw new TemplateCompilationException(t, t.lineNo, e.originalMessage);
            default: throw new TemplateExecutionException(t, t.lineNo, e.originalMessage, e);
        }
    }

    @Override
    public String getName() {
        return tc.getKey().toString();
    }
}
