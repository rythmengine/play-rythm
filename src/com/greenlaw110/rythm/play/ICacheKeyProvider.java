package com.greenlaw110.rythm.play;

import play.i18n.Lang;
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

    String getKey(boolean sessionSensitive, boolean schemeSensitive, boolean langSensitive);

    public static class Default implements ICacheKeyProvider {
        protected final Http.Request req() {
            Http.Request req = Http.Request.current();
            if (null == req) {
                throw new IllegalStateException("HTTP.Request expected");
            }
            return req;
        }
        protected final Scope.Params params() {
            return Scope.Params.current();
        }
        protected final Scope.Session session() {
            return Scope.Session.current();
        }
        @Override
        public String getKey(boolean sessionSensitive, boolean schemeSensitive, boolean langSensitive) {
            Http.Request req = req();
            String key = "rythm" + req.url + req.querystring;
            if (sessionSensitive) {
                key += session().getId();
            }
            if (schemeSensitive) {
                key += req.secure ? "1" : "0";
            }
            if (langSensitive) {
                key += Lang.get();
            }
            return key;
        }
    };
}
