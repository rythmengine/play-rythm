package com.greenlaw110.rythm.play.utils;

import com.greenlaw110.rythm.internal.IExpressionProcessor;
import com.greenlaw110.rythm.internal.Token;
import com.greenlaw110.rythm.utils.S;
import com.greenlaw110.rythm.utils.TextBuilder;

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
            tb.p("new com.greenlaw110.rythm.internal.IExpressionProcessor.IResult(){\n" +
                    "\tpublic String get() { \n" +
                    "\tcom.greenlaw110.rythm.play.RythmPlugin.setActionCallFlag(); \n" +
                    "\tplay.mvc.Http.Request.current().action=\"");
            tb.p(action);
            tb.p("\";\n").p(s).p(";return null;\n\t}\n}.get()");
            return tb.toString();
        }
        return null;
    }
}
