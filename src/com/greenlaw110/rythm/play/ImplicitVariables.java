package com.greenlaw110.rythm.play;

import play.Play;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ImplicitVariables {
    static abstract class Var {
        String name;
        String type;
        Var(String name, String type) {
            this.name = name;
            this.type = type;
        }
        String name() {
            return RythmPlugin.underscoreImplicitVariableName ? "_" + name : name;
        }
        abstract protected Object evaluate();
    }
    
    static Var[] vars = {
            new Var("error", "java.util.Map<String, java.util.List<play.data.validation.Error>>") {
                @Override
                protected Object evaluate() {
                    return Validation.current().errorsMap();
                }
            },
            new Var("flash", "play.mvc.Scope.Flash") {
                @Override
                protected Object evaluate() {
                    return Scope.Flash.current();
                }
            },
            new Var("lang", "java.lang.String") {
                @Override
                protected Object evaluate() {
                    return Lang.get();
                }
            },
            new Var("messages", "play.i18n.Messages") {
                @Override
                protected Object evaluate() {
                    return new Messages();
                }
            },
            new Var("params", "play.mvc.Scope.Params") {
                @Override
                protected Object evaluate() {
                    return Scope.Params.current();
                }
            },
            new Var("play", "play.Play") {
                @Override
                protected Object evaluate() {
                    return new Play();
                }
            },
            new Var("request", "play.mvc.Http.Request") {
                @Override
                protected Object evaluate() {
                    return Http.Request.current();
                }
            },
            new Var("session", "play.mvc.Scope.Session") {
                @Override
                protected Object evaluate() {
                    return Scope.Session.current();
                }
            },
            new Var("response_encoding", "java.lang.String") {
                @Override
                protected Object evaluate() {
                    return Http.Response.current().encoding;
                }
            }
    };
    
}
