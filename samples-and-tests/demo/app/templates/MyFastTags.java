package templates;

import groovy.lang.Closure;
import play.templates.FastTags;
import play.templates.GroovyTemplate;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 21/04/12
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
@FastTags.Namespace("ft")
public class MyFastTags extends FastTags {
    public static void _foo(Map<?, ?> args, Closure body, PrintWriter out, GroovyTemplate.ExecutableTemplate template, int fromLine) {
        out.println("inside fasttags:foo(), timestamp:" + System.currentTimeMillis());
        body.call();
    }
}
