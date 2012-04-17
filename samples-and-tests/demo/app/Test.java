import com.greenlaw110.rythm.Rythm;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 17/04/12
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(Rythm.render("@args Exception result\n@result.getMessage()", new Exception("hello rythm")));
        Rythm.engine.cacheService.shutdown();
    }
}
