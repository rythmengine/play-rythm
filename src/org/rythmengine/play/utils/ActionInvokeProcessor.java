package org.rythmengine.play.utils;

import org.rythmengine.internal.IExpressionProcessor;
import org.rythmengine.internal.Token;
import org.rythmengine.utils.S;
import org.rythmengine.utils.TextBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 28/03/12
 * Time: 8:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActionInvokeProcessor implements IExpressionProcessor {
    @Override
    public String process(String exp, Token token) {
        String s = S.stripBrace(exp);
        if (s.indexOf("(") > 0 && s.startsWith("controllers.")) {
            String action = s.replaceFirst("controllers.", "");
            int pos = action.indexOf("(");
            action = action.substring(0, pos);
            TextBuilder tb = new TextBuilder();
            tb.p("new org.rythmengine.internal.IExpressionProcessor.IResult(){\n" +
                    "\tpublic String get() { \n" +
                    "\torg.rythmengine.play.RythmPlugin.setActionCallFlag(); \n" +
                    "\tplay.mvc.Http.Request.current().action=\"");
            tb.p(action);
            tb.p("\";\n").p(s).p(";return null;\n\t}\n}.get()");
            return tb.toString();
        }
        return null;
    }
}
