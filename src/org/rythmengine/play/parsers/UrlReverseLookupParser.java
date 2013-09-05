package org.rythmengine.play.parsers;

import org.rythmengine.internal.IContext;
import org.rythmengine.internal.IKeyword;
import org.rythmengine.internal.IParser;
import org.rythmengine.internal.Token;
import org.rythmengine.internal.compiler.TemplateClass;
import org.rythmengine.internal.dialect.Rythm;
import org.rythmengine.internal.parser.CodeToken;
import org.rythmengine.internal.parser.ParserBase;
import org.rythmengine.internal.parser.build_in.KeywordParserFactory;
import org.rythmengine.play.utils.StaticRouteResolver;
import org.rythmengine.utils.S;
import org.rythmengine.utils.TextBuilder;
import com.stevesoft.pat.Regex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 29/01/12
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class UrlReverseLookupParser extends KeywordParserFactory {

    protected boolean isAbsolute = false;

    @Override
    public IKeyword keyword() {
        return PlayRythmKeyword._U;
    }

    @Override
    protected String patternStr() {
        return "^(%s%s[ \\t]*((?@())))";
    }

    protected String innerPattern() {
        return "[a-zA-Z_][\\w$_\\.]*(?@())?";
    }

    private static final ConcurrentMap<String, String> staticRouteMap = new ConcurrentHashMap<String, String>();
    private static final ConcurrentMap<String, String> staticAbsoluteRouteMap = new ConcurrentHashMap<String, String>();

    @Override
    public IParser create(IContext ctx) {
        return new ParserBase(ctx) {
            public Token go() {
                Regex r = reg(dialect());
                if (!r.search(remain())) {
//                    if (isAbsolute) {
//                        throw new ParseException(ctx().getTemplateClass(), ctx().currentLine(), "Error parsing @fullUrl statement, correct usage: @fullUrl(Controller.action), or @fullUrl(/public/<your public assets>)");
//                    } else {
//                        throw new ParseException(ctx().getTemplateClass(), ctx().currentLine(), "Error parsing @url statement, correct usage: @url(Controller.action), or @url(/public/<your public assets>)");
//                    }
                    // allow @url expression
                    return null;
                }
                String s = r.stringMatched();
                step(s.length());
                s = r.stringMatched(3);
                s = S.stripBraceAndQuotation(s);
                if (S.isEmpty(s)) {
                    if (isAbsolute) {
                        raiseParseException("Error parsing @fullUrl() tag, controller action or static assets expected");
                    } else {
                        raiseParseException("Error parsing @url() tag, controller action or static assets expected");
                    }
                }
                // try to see if it is a static url
                Map<String, String> routeMap = isAbsolute ? staticAbsoluteRouteMap : staticRouteMap;
                String staticUrl = routeMap.get(s);
                if (null == staticUrl) {
                    try {
                        staticUrl = StaticRouteResolver.reverseWithCheck(s, isAbsolute);
                    } catch (play.exceptions.NoRouteFoundException e) {
                    }
                }
                if (null != staticUrl) {
                    routeMap.put(s, staticUrl);
                    return new Token(staticUrl, ctx());
                } else {
                    if (s.startsWith("/") || s.startsWith("\\")) {
                        raiseParseException("Static URL lookup failed: %s", s);
                    }
                    // otherwise ignore it and try controller action
                }

                // now try parse action name and params
                r = new Regex("([a-zA-Z_][\\w$_\\.]*)((?@())?)");
                if (r.search(s)) {
                    final String action = r.stringMatched(1);
                    s = r.substring(2);
                    //strip off ( and ) if there is
                    if (null == s) s = "";
                    if (s.startsWith("(")) {
                        s = s.substring(1);
                    }
                    if (s.endsWith(")")) {
                        s = s.substring(0, s.length() - 1);
                    }

                    TemplateClass tc = ctx().getTemplateClass();
                    boolean escapeXML = (!tc.isStringTemplate() && tc.templateResource.getKey().toString().endsWith(".xml"));

                    s = new TextBuilder().p("p(s().raw(new org.rythmengine.play.utils.ActionBridge(").p(isAbsolute).p(",").p(escapeXML).p(").invokeMethod(\"").p(action).p("\", new Object[] {").p(s).p("})));").toString();
                    return new CodeToken(s, ctx());
                } else {
                    raiseParseException("Error parsing url reverse lookup");
                    return null;
                }
            }
        };
    }

    public static void main(String[] args) {
        UrlReverseLookupParser p = new UrlReverseLookupParser();
        Regex r = p.reg(Rythm.INSTANCE);
        String s = "@url() abc";
        if (r.search(s)) {
            System.out.println(r.stringMatched());
            s = (r.stringMatched(3));
            System.out.println(">>" + s);
            s = s.substring(1);
            s = s.substring(0, s.length() - 1);
            System.out.println("<<" + s);
//            s = s.substring(0, s.length() - 1);
//            System.out.println(">>" + s);
            if (s.startsWith("\"") || s.startsWith("'")) {
                s = s.substring(1);
            }
            if (s.endsWith("\"") || s.endsWith("'")) {
                s = s.substring(0, s.length() - 1);
            }
            System.out.println(s);
        }

//        s = "RythmTester.test(a.boc(), 14, '3', \"aa\")";
//        //s = "getId()";
//        r = new Regex("([a-zA-Z_][\\w$_\\.]*)((?@())?)");
//        if (r.search(s)) {
//            System.out.println(r.stringMatched());
//            System.out.println(r.stringMatched(1));
//            System.out.println(r.stringMatched(2));
//            System.out.println(r.stringMatched(3));
//
//            s = r.substring(2);
//            //strip off ( and ) if there is
//            if (null == s) s = "";
//            if (s.startsWith("(")) {
//                s = s.substring(1);
//            }
//            if (s.endsWith(")")) {
//                s = s.substring(0, s.length() - 1);
//            }
//            System.out.println(s);
//        }
    }
}
