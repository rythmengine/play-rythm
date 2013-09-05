package org.rythmengine.play.parsers;

import org.rythmengine.internal.IContext;
import org.rythmengine.internal.IParser;
import org.rythmengine.internal.IParserFactory;
import org.rythmengine.internal.Token;
import org.rythmengine.internal.parser.ParserBase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This parser strip groovy template's #{vertatim} and #{/verbatim} tag. The aim is to allow
 * play's default groovy compiler able to compile rythm template which is included inside #{vertatim} tag
 */
public class GroovyVerbatimTagParser implements IParserFactory {
    private static final Pattern P = Pattern.compile("(#\\{\\/?verbatim\\}\\s*\\r*\\n*).*", Pattern.DOTALL);
    @Override
    public IParser create(IContext ctx) {
        return new ParserBase(ctx) {
            @Override
            public Token go() {
                Matcher m = P.matcher(remain());
                if (m.matches()) {
                    step(m.group(1).length());
                    return new Token("", ctx());
                } else {
                    return null;
                }
            }
        };
    }

    public static void main(String[] args) {
        String s = "#{/verbatim}";
        Matcher m = P.matcher(s);
        if (m.matches()) {
            System.out.println(m.group(1));
        }
    }
}
