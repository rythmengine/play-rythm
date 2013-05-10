package org.rythmengine.play;

import play.exceptions.TemplateException;
import play.templates.Template;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 14/02/12
 * Time: 6:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TemplateParseException extends TemplateException {
    public TemplateParseException(Template template, Integer lineNumber, String message) {
        super(template, lineNumber, message);
    }

    @Override
    public String getErrorTitle() {
        return String.format("Template parse error");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not parse : <strong>%s</strong>", getTemplate().name, getMessage());
    }
}
