package org.rythmengine.play;

import play.data.validation.Validation;
import play.mvc.Scope;
import play.templates.FastTags;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 29/01/12
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
@FastTags.Namespace("")
public class FastRythmTags {

    public static class authenticityToken extends FastRythmTag {
        @Override
        public void call(__ParameterList params, __Body body) {
            p("<input type=\"hidden\" name=\"authenticityToken\" value=\"" + Scope.Session.current().getAuthenticityToken() + "\">");
        }
    }

    public static class authenticityTokenValue extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            p(Scope.Session.current().getAuthenticityToken());
        }
    }

    public static class error extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            if (params.size() == 0) {
                throw new RuntimeException("Please specify the error key");
            }
            String key = params.getDefault().toString();
            play.data.validation.Error error = Validation.error(key);
            if (error != null) {
                Object field = params.size() > 1 ? params.getByPosition(1) : null;
                if (null == field) {
                    p(error.message());
                } else {
                    p(error.message(field.toString()));
                }
            }
        }
    }

    public static class errorClass extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            if (params.size() == 0) {
                throw new RuntimeException("Please specify the error key");
            }
            if (Validation.hasError(params.getDefault().toString())) {
                String clsStr = 1 < params.size() ? params.getByPosition(1).toString() : "hasError";
                p(clsStr);
            }
        }
    }

    /**
     * @Deprecated use errors instead
     */
    @Deprecated
    public static class errorList extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            RythmPlugin.warn("@errorList() is deprecated, use @errors() instead");
            errors e = new errors();
            e.__setBuffer(__getBuffer());
            e.call(params, body);
        }
    }

    public static class errors extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            String field = params.size() > 0 ? params.getDefault().toString() : null;
            List<play.data.validation.Error> errors = null == field ? play.data.validation.Validation.errors() : play.data.validation.Validation.errors(field);
            int count = errors.size();
            for (int i = 0; i < count; ++i) {
                body.__setProperty("error", errors.get(i));
                body.__setProperty("error_index", i+1);
                body.__setProperty("error_isLast", (i+1) == count);
                body.__setProperty("error_isFirst", i == 0);
                body.__setProperty("error_parity", (i+1)%2==0?"even":"odd");
                body.render(__getBuffer());
            }
        }
    }

//    public static class i18n extends FastRythmTag {
//        @Override
//        protected void call(ParameterList params, Body body) {
//        }
//    }
//
    public static class jsAction extends FastRythmTag {
        @Override
        protected void call(__ParameterList params, __Body body) {
            String action = params.getDefault().toString();
            //String url = new ActionBridge(false).invokeMethod(action)
            p("function(options) {var pattern = '")
                .p(params.getDefault().toString().replace("&amp;", "&"))
                .p("'; for(key in options) { pattern = pattern.replace(':'+key, options[key]); } return pattern }");
        }
    }
}
