package com.greenlaw110.rythm.play.parsers;

import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.internal.dialect.Rythm;
import com.greenlaw110.rythm.internal.parser.CodeToken;
import com.greenlaw110.rythm.internal.parser.ParserBase;
import com.greenlaw110.rythm.internal.parser.build_in.KeywordParserFactory;
import com.greenlaw110.rythm.spi.IContext;
import com.greenlaw110.rythm.spi.IKeyword;
import com.greenlaw110.rythm.spi.IParser;
import com.greenlaw110.rythm.utils.TextBuilder;
import com.stevesoft.pat.Regex;

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
        return "%s%s\\s*((?@()))";
    }
    
    protected String innerPattern() {
        return "[a-zA-Z_][\\w$_\\.]*(?@())?";
    }

    @Override
    public IParser create(IContext ctx) {
        return new ParserBase(ctx) {
            public TextBuilder go() {
                Regex r = new Regex(String.format(patternStr(), dialect().a(), keyword()));
                if (!r.search(remain())) return null;
                String s = r.stringMatched();
                step(s.length());
                s = r.stringMatched(1);
                //strip off ( and )
                s = s.substring(1);
                s = s.substring(0, s.length() - 1);
                // strip off quotation mark if there is
                if (s.startsWith("\"") || s.startsWith("'")) {
                    s = s.substring(1);
                }
                if (s.endsWith("\"") || s.endsWith("'")) {
                    s = s.substring(0, s.length() - 1);
                }
                // now parse action name and params
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
                    final String param = s;
                    return new CodeToken("", ctx()) {
                        @Override
                        public void output() {
                            p("p(new com.greenlaw110.rythm.play.utils.ActionBridge(").p(isAbsolute).p(").invokeMethod(\"").p(action).p("\", new Object[] {").p(param).p("}));");
                        }
                    };
                } else {
                    throw new ParseException(ctx().getTemplateClass(), ctx().currentLine(), "Error parsing url reverse lookup");
                }
            }
        };
    }

    public static void main(String[] args) {
        UrlReverseLookupParser p = new UrlReverseLookupParser();
        Regex r = p.reg(new Rythm());
        String s = "@_u(\"Clients.form(_._id)\") abc";
        if (r.search(s)) {
            System.out.println(r.stringMatched());
            s = (r.stringMatched(1));
            System.out.println(">>" + s);
            s = s.substring(1).substring(0, s.length() - 2);
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
