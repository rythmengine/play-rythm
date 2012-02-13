package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.exception.CompileException;
import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.template.ITemplate;
import play.exceptions.TemplateCompilationException;
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
    
    private static class JavaDumbTemplate extends Template {
        @Override
        public void compile() {
        }
        @Override
        protected String internalRender(Map<String, Object> args) {
            return null;
        }
        
        JavaDumbTemplate(String javaSource) {
            this.source = javaSource;
        }
    }

    void refresh() {
        if (engine().isProdMode()) return;
        try {
            engine().classLoader.detectChange(tc);
        } catch (ParseException e) {
            throw new TemplateParseException(this, e);
        } catch (CompileException e) {
            int line = e.templatelineNumber;
            if (-1 == line) {
                line = e.javaLineNumber;
                Template t = new JavaDumbTemplate(e.getJavaSource());
                t.name = this.getName();
                throw new TemplateCompilationException(t, line, e.errorMessage);
            } else {
                throw new TemplateCompilationException(this, line, e.errorMessage);
            }
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
        ITemplate t = tc.asTemplate();
        t.setRenderArgs(args);
        return t.render();
    }

    @Override
    public String getName() {
        return tc.getKey();
    }
}
