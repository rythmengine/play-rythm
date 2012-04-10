package controllers;

import com.greenlaw110.rythm.play.UseRythmTemplateEngine;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        String title = "Hello";
        render(title);
    }

    public static void callTemplate() {
        render();
    }

    public static void testDefaultEscape() {
        String body = "<h1>This is the header one</h1>";
        render(body);
    }

    public static void testRenderSection() {
        render();
    }
}