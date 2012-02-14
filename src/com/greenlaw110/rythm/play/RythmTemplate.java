package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.exception.CompileException;
import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.exception.RythmException;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.template.ITemplate;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateExecutionException;
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
    
    private static class TemplateInfo extends Template {
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
        
        private int lineNo = -1; 
    }

    void refresh() {
        refresh(false);
    }

    void refresh(boolean forceRefresh) {
        if (!forceRefresh && engine().isProdMode()) return;
        RythmPlugin.info(">>> refreshing [%s]...", tc.name());
        try {
            engine().classLoader.detectChange(tc);
        } catch (ParseException e) {
            throw new TemplateParseException(this, e);
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

    @Override
    protected String internalRender(Map<String, Object> args) {
        try {
            ITemplate t = tc.asTemplate();
            t.setRenderArgs(args);
            return t.render();
        } catch (RythmException e) {
            TemplateInfo t = handleRythmException(e);
            throw new TemplateExecutionException(t, t.lineNo, e.errorMessage, e);
        } catch (Exception e) {
            throw new TemplateExecutionException(this, 0, e.getMessage(), e);
        }
    }
    
    private static TemplateInfo handleRythmException(RythmException e) {
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
