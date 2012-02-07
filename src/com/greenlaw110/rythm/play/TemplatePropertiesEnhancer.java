package com.greenlaw110.rythm.play;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javassist.ClassPath;
import javassist.NotFoundException;
import play.classloading.enhancers.PropertiesEnhancer;

import com.greenlaw110.rythm.Rythm;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
class TemplatePropertiesEnhancer extends PropertiesEnhancer {
    TemplatePropertiesEnhancer() {
        this.classPool.removeClassPath(new ApplicationClassesClasspath());
        this.classPool.appendClassPath(new ClassPath() {
            @Override
            public InputStream openClassfile(String className) throws NotFoundException {
                return new ByteArrayInputStream(Rythm.engine.classes.getByClassName(className).enhancedByteCode);
            }

            @Override
            public URL find(String className) {
                if (Rythm.engine.classes.getByClassName(className) != null) {
                    String cname = className.replace('.', '/') + ".class";
                    try {
                        // return new File(cname).toURL();
                        return new URL("file:/ApplicationClassesClasspath/" + cname);
                    } catch (MalformedURLException e) {
                    }
                }
                return null;
            }

            @Override
            public void close() {
            }
        });
    }
}
