package com.greenlaw110.rythm.play;

import play.mvc.Http;
import play.mvc.results.Result;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cache an action's result.
 *
 * <p>If a time is not specified, the results will be cached for 1 hour by default.
 *
 * <p>Example: <code>@CacheFor("1h")</code>
 *
 * <ul>Differences with play.cache.CacheFor</p>
 * <li>You can specify time using configuration name like <code>cron.xx.cache4</code></li>
 * <li>It will check <code>rythm.cache.prodOnly</code> configuration. If it's true, then cache not effect on dev mode</li>
 * <li>Will NOT run @Before and @After filters if result get from cache, however @Finally filters will still be executed</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache4 {
    /**
     * Cache time, See play.libs.Time
     *
     * In addition, it also understand time configuration start with "cron."
     *
     * A special value "forever" also accept
     *
     * @return cache expiration time
     */
    String value() default "1h";

    /**
     * @deprecated
     */
    String id() default "";

    /**
     * Define the cache key. Leave it empty if you want system to generate key from request automatically
     *
     * @return the session key provider class
     */
    Class<? extends ICacheKeyProvider> key() default ICacheKeyProvider.Default.class;

    /**
     * Whether use session data to generate key
     *
     * @deprecated use {@link #sessionSensitive()} instead 
     * @return true if use session id to generate the key
     */
    boolean useSessionData() default false;
    
    /**
     * Whether use session id to generate the cache key, meaning if the cached copy is 
     * for session specific or not
     *
     * @return true if it is session sensitive
     */
    boolean sessionSensitive() default false;
    
    
    /**
     * Whether use <tt>Lang.get()</tt> to generate the cache key, meaning if the cached copy is 
     * for language specific or not
     *
     * @return true if it is language sensitive
     */
    boolean langSensitive() default false;

    /**
     * Whether the cache key is sensitive to request.secure. When this parameter
     * is set to <code>true</code>, then the request coming from http and https channel
     * will result in different cached copy
     * 
     * default: false
     * 
     * @return true if key is http scheme sensitive
     */
    boolean schemeSensitive() default false;

    /**
     * Indicate whether cache post request. Useful for certain case, e.g. facebook always post to tab page in iframe
     * 
     * @return true if cache post request also 
     */
    boolean cachePost() default false;

    /**
     * Wrap action result so that system know whether it comes out from cache or a real execution result
     */
    public static class CacheResult extends Result {
        private Result cached;

        public CacheResult(Result result) {
            if (null == result) throw new NullPointerException();
            cached = result;
        }

        public Result getCached() {
            return cached;
        }

        @Override
        public void apply(Http.Request request, Http.Response response) {
            cached.apply(request, response);
        }
    }
}
