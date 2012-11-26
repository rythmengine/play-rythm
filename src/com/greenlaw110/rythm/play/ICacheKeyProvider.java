package com.greenlaw110.rythm.play;

import org.bouncycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 20/11/12
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ICacheKeyProvider {
    String getKey();
    public static class Default implements ICacheKeyProvider {
        protected final Http.Request req() {
            return Http.Request.current();
        }
        protected final Scope.Params params() {
            return Scope.Params.current();
        }
        @Override
        public String getKey() {
            Http.Request req = Http.Request.current();
            if (null != req) {
                return "rythm-urlcache:" + req.url + req.querystring;
            } else {
                throw new IllegalStateException("HTTP.Request expected");
            }
        }
    };
}
