package org.rythmengine.play.parsers;

import com.stevesoft.pat.Regex;
import org.rythmengine.internal.IContext;
import org.rythmengine.internal.IParser;
import org.rythmengine.internal.TemplateParser;
import org.rythmengine.internal.Token;
import org.rythmengine.internal.dialect.Rythm;
import org.rythmengine.internal.parser.ParserBase;
import org.rythmengine.internal.parser.build_in.KeywordParserFactory;
import org.rythmengine.utils.S;
import play.Play;

public class ExitIfNoModuleParser extends KeywordParserFactory {

    @Override
    public PlayRythmKeyword keyword() {
        return PlayRythmKeyword.EXIT_IF_NO_MODULE;
    }

    public IParser create(final IContext ctx) {
        return new ParserBase(ctx) {
            public Token go() {
                Regex r = reg(dialect());
                if (!r.search(remain())) {
                    raiseParseException("error parsing @__exitIfNoPlayModule__, correct usage: @__exitIfNoPlayModule__(\"play-module-name\"");
                }
                step(r.stringMatched().length());
                String s = r.stringMatched(1);
                s = S.stripBraceAndQuotation(s);
                if (Play.modules.containsKey(s)) {
                    return new Token("", ctx());
                } else {
                    throw new TemplateParser.ExitInstruction();
                }
            }
        };
    }

    @Override
    protected String patternStr() {
        return "%s%s\\s*((?@()))[\\s]+";
    }

    public static void main(String[] args) {
        String s = "@__exitIfNoPlayModule__(rythm)\nabc";
        ExitIfNoModuleParser ap = new ExitIfNoModuleParser();
        Regex r = ap.reg(Rythm.INSTANCE);
        p(s, r);
    }
}
