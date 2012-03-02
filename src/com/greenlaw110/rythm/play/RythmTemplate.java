package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.exception.CompileException;
import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.exception.RythmException;
import com.greenlaw110.rythm.internal.compiler.ClassReloadException;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.template.ITemplate;
import play.Logger;
import play.Play;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.UnexpectedException;
import play.templates.Template;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class RythmTemplate extends Template {
    
    private TemplateClass tc;

    RythmTemplate(ITemplateResource resource) {
        if (null == resource) throw new NullPointerException();
        tc = new TemplateClass(resource, RythmPlugin.engine, true);
        name = resource.getKey();
        source = tc.templateResource.asTemplateContent();
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
            engine().classLoader.detectChange(tc);
        } catch (ClassReloadException e) {
            RythmPlugin.debug("restart rythm engine to reload changed template...");
            engine().restart(e);
            refresh(forceRefresh);
        } catch (ParseException e) {
            TemplateInfo t = handleRythmException(e);
            throw new TemplateParseException(t, e);
        } catch (CompileException e) {
            TemplateInfo t = handleRythmException(e);
            throw new TemplateCompilationException(t, t.lineNo, e.originalMessage);
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
        refresh();
        //if (tc.isValid) tc.compile();
    }

    private static final ThreadLocal<Integer> refreshCounter = new ThreadLocal<Integer>();
    @Override
    protected String internalRender(Map<String, Object> args) {
        try {
            ITemplate t = tc.asTemplate();
            t.setRenderArgs(args);
            String s = t.render();
            if (!RythmPlugin.engine.isProdMode()) {
                refreshCounter.set(0);
            }
            return s;
        } catch (RythmException e) {
            Throwable cause = e.getCause();
            if (null != cause && cause instanceof ClassCastException) {
                return handleClassCastException((ClassCastException)cause, args);
            }
            TemplateInfo t = handleRythmException(e);
            throw new TemplateExecutionException(t, t.lineNo, e.errorMessage, e);
        } catch (ClassCastException e) {
            return handleClassCastException(e, args);
        } catch (Exception e) {
            throw new TemplateExecutionException(this, -1, e.getMessage(), e);
        }
    }
    
    String handleClassCastException(ClassCastException e, Map<String, Object> args) {
        Integer I = refreshCounter.get();
        if (null == I || I < 2) {
            if (null == I) refreshCounter.set(1);
            else refreshCounter.set(++I);
            if (Logger.isDebugEnabled()) RythmPlugin.debug("ClassCastException detected, force refresh template class and continue...");
            tc.refresh(true);
            return internalRender(args);
        } else {
            refreshCounter.set(0);
            throw new UnexpectedException("Too many ClassCastException encountered, please restart Play", e);
        }
    }
    
    static TemplateInfo handleRythmException(RythmException e) {
        int line = e.templatelineNumber;
        TemplateInfo t;
        if (-1 == line) {
            line = e.javaLineNumber;
            t = new TemplateInfo(e.getTemplateName(), e.getJavaSource(), line);
        } else {
            t = new TemplateInfo(e.getTemplateName(), e.getTemplateSource(), line);
        }
        return t;
    }

    @Override
    public String getName() {
        return tc.getKey();
    }
}
