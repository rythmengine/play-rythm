package controllers;

import com.greenlaw110.rythm.play.Cache4;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 28/04/12
 * Time: 7:56 AM
 * To change this template use File | Settings | File Templates.
 */
@With(Secure.class)
public class SecureApp extends Controller {
    @Cache4("cron.testCache4")
    public static void zoneA() {
        long time = System.currentTimeMillis();
        render(time);
    }

    @Cache4(useSessionData = true, value = "cron.testCache4")
    public static void zoneB() {
        long time = System.currentTimeMillis();
        render(time);
    }

    public static void logout() {
        session.clear();
        response.removeCookie("rememberme");
    }
}
