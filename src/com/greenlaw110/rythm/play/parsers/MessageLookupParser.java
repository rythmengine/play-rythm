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
public class MessageLookupParser extends KeywordParserFactory {
    
    @Override
    public IKeyword keyword() {
        return PlayRythmKeyword._M;
    }

    @Override
    protected String patternStr() {
        return "%s%s\\s*((?@()))";
    }
    
    protected String innerPattern() {
        return "(((?@\"\")|(?@'')|[a-zA-Z_][\\w$_\\.]*)(?@())?)(,\\s*([a-zA-Z_][\\w$_\\.]*(?@())?))?";
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
                // now parse message string and parameters
                r = new Regex(innerPattern());
                if (r.search(s)) {
                    final String msgStr = r.stringMatched(1);
                    final String param = r.stringMatched(4);
                    return new CodeToken("", ctx()) {
                        @Override
                        public void output() {
                            p("p(play.i18n.Messages.get(").p(msgStr);
                            if (null != param) {
                                p(", ").p(param);
                            }
                            p(");");
                        }
                    };
                } else {
                    throw new ParseException(ctx().getTemplateName(), ctx().currentLine(), "Error parsing url reverse lookup");
                }
            }
        };
    }

    public static void main(String[] args) {
        MessageLookupParser p = new MessageLookupParser();
        Regex r = p.reg(new Rythm());
        String s = "@_m(abc)";
        if (r.search(s)) {
            System.out.println(r.stringMatched());
            s = (r.stringMatched(1));
            System.out.println(">>" + s);
            s = s.substring(1).substring(0, s.length() - 2);
            System.out.println("<<" + s);
//            s = s.substring(0, s.length() - 1);
//            System.out.println(">>" + s);
            //System.out.println(s);
            r = new Regex(p.innerPattern());
            if (r.search(s)) {
                System.out.println("1>>" + r.stringMatched(1));
                System.out.println("2>>" + r.stringMatched(2));
                System.out.println("3>>" + r.stringMatched(3));
                System.out.println("4>>" + r.stringMatched(4));
            }
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
