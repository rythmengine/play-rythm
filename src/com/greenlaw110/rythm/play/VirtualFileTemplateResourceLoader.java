package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.exception.ParseException;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.resource.ITemplateResourceLoader;
import com.greenlaw110.rythm.resource.TemplateResourceBase;
import com.greenlaw110.rythm.runtime.ITag;
import play.Play;
import play.libs.IO;
import play.vfs.VirtualFile;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualFileTemplateResourceLoader implements ITemplateResourceLoader{

    public static VirtualFileTemplateResourceLoader instance = new VirtualFileTemplateResourceLoader();

    public static class VirtualFileTemplateResource extends TemplateResourceBase {
        
        private static final long serialVersionUID = -4307922939957393745L;
        private String tagName;
        
        private VirtualFile file;

        VirtualFileTemplateResource(VirtualFile file) {
            this.file = file;
            String tagRoot = RythmPlugin.tagRoot;
            if (!tagRoot.startsWith("/")) tagRoot = '/' + tagRoot;
            String filePath = file.relativePath();
            filePath = filePath.replaceFirst("\\{.*\\}", ""); // strip off module prefix
            if (filePath.startsWith(tagRoot)) {
                String tagName = filePath.substring(tagRoot.length() + 1);
                while (tagName.startsWith("/") || tagName.startsWith("\\")) {
                    tagName = tagName.substring(1);
                }
                tagName = tagName.replace('\\', '.');
                tagName = tagName.replace('/', '.');
                int dot = tagName.lastIndexOf(".");
                this.tagName = tagName.substring(0, dot);
            }
        }

        @Override
        protected long defCheckInterval() {
            return 100;
        }

        @Override
        protected long lastModified() {
            return file.lastModified();
        }

        @Override
        protected String reload() {
            return IO.readContentAsString(file.inputstream());
        }

        @Override
        public String getKey() {
            return file.relativePath();
        }

        @Override
        public boolean isValid() {
            return VirtualFileTemplateResourceLoader.isValid(file);
        }

        @Override
        public String getSuggestedClassName() {
            return path2CN(file.relativePath().replaceFirst("\\{.*\\}", ""));
        }

        @Override
        public String tagName() {
            return tagName;
        }

    }

    public static boolean isValid(VirtualFile file) {
        return (null != file) && file.exists() && file.getRealFile().canRead();
    }
    
    private VirtualFile loadFromPath_(String path) {
        VirtualFile vf = null;
        if (path.indexOf("module:") != -1) vf = VirtualFile.fromRelativePath(path);
        else {
            vf = Play.getVirtualFile(path);
            if (!isValid(vf) && path.startsWith("/")) {
                // try to attach template home and tag home
                if (!path.startsWith(RythmPlugin.templateRoot)) {
                    String path0 = RythmPlugin.templateRoot + path;
                    vf = Play.getVirtualFile(path0);
                }
                if (!isValid(vf) && !path.startsWith(RythmPlugin.tagRoot)) {
                    String path0 = RythmPlugin.tagRoot + path;
                    vf = Play.getVirtualFile(path0);
                }
            }
        }
        return vf;
    }

    @Override
    public ITemplateResource load(String path) {
        VirtualFile vf = loadFromPath_(path);
        if (!isValid(vf) && path.indexOf("module:") == -1) {
            // try to see if it's package.class style
            // change a packaged name into a file path name
            path = path.replace(".", "/");
            // but not for suffix
            int pos = path.lastIndexOf("/");
            String path0 = path;
            path = path0.substring(0, pos) + "." + path0.substring(pos + 1);
            vf = loadFromPath_(path);
        }
        if (!isValid(vf)) return null;
        return load(vf);
    }
    
    public ITemplateResource load(VirtualFile file) {
        String path = file.relativePath();
        if (path.contains(".svn")) return null; // definitely we don't want to load anything inside there
        if (RythmPlugin.defaultEngine == RythmPlugin.EngineType.system) {
            // by default use groovy template unless it's in the white list
            if (!RythmTemplateLoader.whiteListed(path)) return null;
        } else {
            // by default use rythm template unless it's in the black list
            if (RythmTemplateLoader.blackListed(path)) return null;
        }
        
        return new VirtualFileTemplateResource(file);
    }

    @Override
    public void tryLoadTag(String tagName) {
//Logger.info(">>> try to load tag: %s", tagName);
        RythmEngine engine = RythmPlugin.engine;
        if (engine.tags.containsKey(tagName)) return;
//Logger.info(">>> try to load tag: %s, tag not found in engine registry, continue loading", tagName);
        String origName = tagName;
        tagName = tagName.replace('.', '/');
        final String[] suffixes = {
                ".html",
                ".json",
                ".tag"
        };
        String[] roots = {RythmPlugin.tagRoot, RythmPlugin.templateRoot};
        String tagName0 = tagName;
        for (String root: roots) {
            tagName = root + "/" + tagName0;
            VirtualFile tagFile = null;
            for (String suffix: suffixes) {
                String name = tagName + suffix;
                tagFile = Play.getVirtualFile(name);
                if (null != tagFile && tagFile.getRealFile().canRead()) {
    //Logger.info(">>> try to load tag: %s, tag file found: %s", tagName, tagFile);
                    try {
                        VirtualFileTemplateResource tr = new VirtualFileTemplateResource(tagFile);
                        TemplateClass tc = engine.classes.getByTemplate(tr.getKey());
                        if (null == tc) {
                            tc = new TemplateClass(tr, engine);
                        }
    //Logger.info(">>> try to load tag: %s, Template class found: %s", tagName, tc);
                        ITag tag = (ITag)tc.asTemplate();
                        if (null != tag) {
    //Logger.info(">>> try to load tag: %s, tag found!!!", tagName);
                            engine.registerTag(origName, tag);
    //Logger.info(">>> try to load tag: %s, tag registered!!!", tagName);
                            return;
                        }
    //Logger.info(">>> try to load tag: %s, tag find found: %s", tagName, tagFile);
                    } catch (Exception e) {
    //Logger.error(e, ">>> error loading tag: %s", tagName);
                        if (e instanceof ParseException) {
                            throw (ParseException)e;
                        }
                        // ignore
                    }
                }
            }
        }
    }

}
