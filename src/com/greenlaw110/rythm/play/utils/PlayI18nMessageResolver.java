package com.greenlaw110.rythm.play.utils;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.extension.II18nMessageResolver;
import com.greenlaw110.rythm.template.TemplateBase;
import play.i18n.Lang;
import play.i18n.Messages;

import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 11/03/13
 * Time: 7:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class PlayI18nMessageResolver implements II18nMessageResolver {
    @Override
    public String getMessage(Locale locale, RythmEngine engine, String key) {
        if (null == locale && null != engine) {
            TemplateBase tmpl = engine.currentTemplate();
            locale = null == tmpl ? engine.locale() : tmpl.__curLocale();
        }
        String sLoc = null == locale ? Lang.get() : locale.toString();
        return Messages.getMessage(sLoc, key);
    }

    @Override
    public String getMessage(RythmEngine engine, String key, Object... args) {
        Locale locale = null;
        if (args.length > 0) {
            Object arg0 = args[0];
            if (arg0 instanceof Locale) {
                locale = (Locale)arg0;
                Object[] args0 = new Object[args.length - 1];
                System.arraycopy(args, 1, args0, 0, args.length - 1);
                args = args0;
            }
        }
        if (null == locale && null != engine) {
            TemplateBase tmpl = engine.currentTemplate();
            locale = null == tmpl ? engine.locale() : tmpl.__curLocale();
        }
        String sLoc = null == locale ? Lang.get() : locale.toString();
        return Messages.getMessage(sLoc, key, args);
    }
}
