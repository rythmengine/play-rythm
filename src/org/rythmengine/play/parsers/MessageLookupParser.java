package org.rythmengine.play.parsers;

import com.stevesoft.pat.Regex;
import org.rythmengine.internal.IContext;
import org.rythmengine.internal.IKeyword;
import org.rythmengine.internal.IParser;
import org.rythmengine.internal.Token;
import org.rythmengine.internal.dialect.Rythm;
import org.rythmengine.internal.parser.CodeToken;
import org.rythmengine.internal.parser.ParserBase;
import org.rythmengine.internal.parser.build_in.KeywordParserFactory;
import org.rythmengine.utils.S;

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
        return "^(%s%s[\\t ]*((?@())))";
    }

    protected static String innerPattern() {
        return "(((?@\"\")|(?@())|(?@'')|[a-zA-Z_][\\w$_\\.]*)(?@())?)(.*)";
    }
    
    protected static final String groovyPattern = "^&((?@{}))"; 

    @Override
    public IParser create(IContext ctx) {
        return new ParserBase(ctx) {
            public Token go() {
                String remain = remain();
                Regex r = reg(dialect());
                String sMatched;
                String sInside;
                if (!r.search(remain)) {
                    return null;
                    // groovy pattern has no chance to get resolved because '&' does not drive parsing machine
//                    r = new Regex(groovyPattern);
//                    if (!r.search(remain)) return null;
//                    sInside = S.strip(r.stringMatched(1), "{", "}");
                } else {
                    sInside = S.stripBrace(r.stringMatched(3));
                }
                sMatched = r.stringMatched();
                step(sMatched.length());
                String s = sInside;

                // now parse message string and parameters
                r = new Regex(innerPattern());
                if (r.search(s)) {
                    String msgStr = r.stringMatched(1);
                    boolean hasQuotation = msgStr.startsWith("'") || msgStr.startsWith("\"");
                    msgStr = S.stripQuotation(msgStr);
                    String param = r.stringMatched(3);
                    if (S.isEmpty(param)) {
                        String fmt = hasQuotation ? "Messages.get(\"%s\")" : "Messages.get(%s)";
                        s = String.format(fmt, msgStr);
                    } else {
                        String fmt = hasQuotation ? "Messages.get(\"%s\" %s)" : "Messages.get(%s %s)";
                        s = String.format(fmt, msgStr, param);
                    }
                    ctx().getCodeBuilder().addImport("play.i18n.Messages", ctx().currentLine());
                    return new CodeToken(s, ctx()){
                        @Override
                        public void output() {
                            p("p(").p(s).p(");");
                            pline();
                        }
                    };
                } else {
                    raiseParseException("Error parsing message lookup");
                    return null;
                }
            }
        };
    }

    public static void main(String[] args) {
        testRythm();
        System.out.println("------------------------------------");
        testGroovy();
    }
    
    private static void testGroovy() {
        String s = "&{'abc', d}";
        Regex r = new Regex(groovyPattern);
        p(s, r);
    }

    private static void testRythm() {
        MessageLookupParser p = new MessageLookupParser();
        Regex r = p.reg(Rythm.INSTANCE);
        String s = "@msg(x, \"rythm\")";
        p(s, r);
    }
}
