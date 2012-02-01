package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.template.ITemplate;
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
        tc = new TemplateClass(resource, RythmPlugin.engine);
        name = resource.getKey();
    }

    private RythmEngine engine() {
        return RythmPlugin.engine;
    }

    void refresh() {
        if (engine().isProdMode()) return;
        engine().classLoader.detectChange(tc);
        if (!tc.isValid) {
            RythmTemplateLoader.cache.remove(getName());
        }
    }

    public boolean isValid() {
        return tc.isValid;
    }
    
    @Override
    public void compile() {
        refresh();
        if (!tc.isValid) tc.compile();
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
