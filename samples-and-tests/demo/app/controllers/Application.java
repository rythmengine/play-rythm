package controllers;

import com.greenlaw110.rythm.play.UseRythmTemplateEngine;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Controller;
import play.mvc.Util;

import java.util.Random;

public class Application extends Controller {

    @Util
    public static void index() {
        String title = "Hello";
        render(title);
    }

    public static void bug404xml() {
        Exception result = new Exception("404.xml");
        render("errors/404.xml", result);
    }

    public static void testDefaultEscape() {
        String body = "<h1>This is header one</h1>";
        render(body);
    }

    public static void testRenderSection() {
        render();
    }

    public static void testCache() {
        int val = new Random().nextInt(100);
        render(val);
    }

    public static void testTagCache() {
        int var = new Random().nextInt();
        int val = 100;
        render(val, var);
    }

    public static void testLoadTagByRelativePath() {
        render();
    }

    public static void testLoadTagUsingImportPath() {
        render();
    }

    /**
     * Freewind report _inits.html cannot be called from other template
     */
    public static void test_Inits() {
        render();
    }

    public static void timestamp() {
        long timestamp = System.currentTimeMillis();
        render(timestamp);
    }

    public static void testActionCall() {
        render();
    }

    public static void testFastTag() {
        render();
    }

    public static void testInclude() {
        render();
    }

    public static void testBreakContinue() {
        render();
    }

    public static void testInvoke() {
        render();
    }

    public static void testExtendsWithParams() {
        render();
    }

    public static void testExtendsWithOutParams() {
        render();
    }

    public static void callExtendedTag(){
        render();
    }

    public static void escapeTagInvocation() {
        render();
    }

    public static void testTagInvokeCallBack() {
        render();
    }

    public static void testAssign() {
        render();
    }

    public static void testChain() {
        render();
    }

    public static void testMisc() {
        render();
    }

    public static void testSimple() {
        render();
    }

}