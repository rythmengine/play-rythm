package controllers;

import com.greenlaw110.rythm.logger.Logger;
import com.greenlaw110.rythm.play.Cache4;
import com.greenlaw110.rythm.play.RythmPlugin;
import com.greenlaw110.rythm.play.UseRythmTemplateEngine;
import com.greenlaw110.rythm.template.JavaTagBase;
import org.apache.commons.lang3.StringUtils;
import play.Play;
import play.cache.Cache;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Util;

import java.util.Properties;
import java.util.Random;

public class Application extends Controller {
    @Before(unless = {"testCache4","cachedTS"})
    public static void enableCacheOnDev() {
        Play.configuration.setProperty("rythm.cache.prodOnly", "false");
    }

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

    public static void testDefaultLayoutContent() {
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

    public static void testInlineTag() {
        render();
    }

    @Cache4("cron.testCache4")
    public static void cachedTS(String param) {
        long ts = System.currentTimeMillis();
        renderText(String.valueOf(ts));
    }

    public static void testCache4(boolean enableCache) {
        Play.configuration.setProperty("cron.testCache4", "3s");
        if (enableCache) {
            Play.configuration.setProperty("rythm.cache.prodOnly", "false");
        } else {
            Play.configuration.setProperty("rythm.cache.prodOnly", "true");
        }
        render(enableCache);
    }

    public static void testCache4WithSessionData() {
        Properties c = Play.configuration;
        c.setProperty("cron.testCache4", "1mn");
        c.setProperty("rythm.cache.prodOnly", "false");
        String cacheTime = c.getProperty("cron.testCache4");
        render(cacheTime);
    }

    public static void setCacheTime(String time) {
        Play.configuration.setProperty("cron.testCache4", time);
        renderText(time);
    }

    public static void reset() {
        session.clear();
        response.removeCookie("rememberme");
        Cache.clear();
    }

    public static void login() {
        session.put("username", "tester");
        ok();
    }

    public static void fibonacci() {
        int max = 1000000;
        int i = 0;
        int j = 1;
        render(max, i, j);
    }
}