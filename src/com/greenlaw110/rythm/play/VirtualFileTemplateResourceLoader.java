package com.greenlaw110.rythm.play;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.extension.ICodeType;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.resource.ITemplateResourceLoader;
import com.greenlaw110.rythm.resource.TemplateResourceBase;
import com.greenlaw110.rythm.resource.TemplateResourceManager;
import com.greenlaw110.rythm.template.ITag;
import com.greenlaw110.rythm.utils.S;
import play.Play;
import play.jobs.Job;
import play.jobs.JobsPlugin;
import play.libs.IO;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 27/01/12
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualFileTemplateResourceLoader implements ITemplateResourceLoader {

    public static VirtualFileTemplateResourceLoader instance = new VirtualFileTemplateResourceLoader();

    public static class VirtualFileTemplateResource extends TemplateResourceBase {

        private static final long serialVersionUID = -4307922939957393745L;
        private String tagName;

        private VirtualFile file;

        VirtualFileTemplateResource(VirtualFile file) {
            if (null == file) throw new NullPointerException();
            this.file = file;
            this.tagName = getFullTagName(getKey());
//            String tagRoot = RythmPlugin.tagRoot;
//            if (!tagRoot.startsWith("/")) tagRoot = '/' + tagRoot;
//            String filePath = file.relativePath();
//            filePath = filePath.replaceFirst("\\{.*\\}", ""); // strip off module prefix
//            if (filePath.startsWith(tagRoot)) {
//                String tagName = filePath.substring(tagRoot.length() + 1);
//                while (tagName.startsWith("/") || tagName.startsWith("\\")) {
//                    tagName = tagName.substring(1);
//                }
//                tagName = tagName.replace('\\', '.');
//                tagName = tagName.replace('/', '.');
//                int dot = tagName.lastIndexOf(".");
//                this.tagName = tagName.substring(0, dot);
//            }
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
        
        private String key = null;

        @Override
        public String getKey() {
            if (null == key) {
                String path = file.relativePath();
                key = path.replaceFirst("\\{.*?\\}", "");
            }
            return key;
        }

        @Override
        public boolean isValid() {
            return VirtualFileTemplateResourceLoader.isValid(file);
        }

        @Override
        public String getSuggestedClassName() {
            return path2CN(file.relativePath().replaceFirst("\\{.*\\}", "")).replaceFirst("app_rythm_", "");
        }

        @Override
        public String tagName() {
            return tagName;
        }

        public boolean exists() {
            return null != file && file.exists();
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof VirtualFileTemplateResource) {
                VirtualFileTemplateResource that = (VirtualFileTemplateResource)obj;
                return (that.file == this.file) || that.file.equals(this.file);
            }
            return false;
        }
    }

    public static boolean isValid(VirtualFile file) {
        return (null != file) && file.exists() && file.getRealFile().canRead();
    }

    private VirtualFile loadFromPath_(String path) {
        VirtualFile vf;
        if (path.indexOf("module:") != -1) vf = VirtualFile.fromRelativePath(path);
        else {
            vf = Play.getVirtualFile(path);
            if (!isValid(vf)) {
                if (!path.startsWith("/")) path = "/" + path;
                // try to attach template home and tag home
//                if (!path.startsWith(RythmPlugin.templateRoot2)) {
//                    String path0 = RythmPlugin.templateRoot2 + path;
//                    vf = Play.getVirtualFile(path0);
//                }
                if (!isValid(vf) && !path.startsWith(RythmPlugin.templateRoot)) {
                    String path0 = RythmPlugin.templateRoot + path;
                    vf = Play.getVirtualFile(path0);
                }
//                if (!isValid(vf) && !path.startsWith(RythmPlugin.tagRoot)) {
//                    String path0 = RythmPlugin.tagRoot + path;
//                    vf = Play.getVirtualFile(path0);
//                }
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
            if (-1 != pos) {
                String path0 = path;
                path = path0.substring(0, pos) + "." + path0.substring(pos + 1);
            }
            vf = loadFromPath_(path);
        }
        if (!isValid(vf)) return null;
        // don't check black and white list as this is initialized from template engine side, which
        // might be very well loading a extended template which is not built into BW list
        return load(vf, false);
    }

    private VirtualFileTemplateResourceLoader.VirtualFileTemplateResource load(VirtualFile file, boolean checkBWList) {
        String path = file.relativePath();
        if (path.contains(".svn")) return null; // definitely we don't want to load anything inside there
        return new VirtualFileTemplateResource(file);
    }

    public VirtualFileTemplateResourceLoader.VirtualFileTemplateResource load(VirtualFile file) {
        return load(file, true);
    }

    @Override
    public String getFullTagName(TemplateClass tc, RythmEngine engine) {
        String key = tc.getKey().toString();
        return getFullTagName(key);
    }

    private static String getFullTagName(String key) {
        if (key.startsWith("/")) key = key.substring(1);
        if (key.startsWith(RythmPlugin.templateRoot)) {
            key = key.replace(RythmPlugin.templateRoot, "");
        }
        if (key.startsWith("/")) key = key.substring(1);
        int pos = key.lastIndexOf(".");
        if (-1 != pos) key = key.substring(0, pos);
        return key.replace('/', '.');
    }

    @Override
    public TemplateClass tryLoadTag(String tagName, RythmEngine engine, TemplateClass templateClass) {
        return tryLoadTag(tagName, engine, templateClass, true);
    }
    
    private TemplateClass tryLoadTag(String tagName, RythmEngine engine, TemplateClass templateClass, boolean processTagName) {
//Logger.info(">>> try to load tag: %s", tagName);
        if (null == engine) engine = RythmPlugin.engine;
        if (engine.hasTag(tagName)) return null; //TODO: not consistent here
//Logger.info(">>> try to load tag: %s, tag not found in engine registry, continue loading", tagName);
        //String origName = tagName;
        final List<String> suffixes = new ArrayList(Arrays.asList(new String[]{
                ".html",
                ".json",
                ".js",
                ".css",
                ".csv",
                ".tag",
                ".xml",
                ""
        }));
        ICodeType codeType = TemplateResourceBase.getTypeOfPath(engine, tagName);
        if (ICodeType.DefImpl.RAW == codeType) {
            // use caller's code type
            codeType = templateClass.codeType;
        }
        final String tagNameOrigin = tagName;
        if (processTagName) {
            boolean tagNameProcessed = false;
            while(!tagNameProcessed) {
                // process tagName to remove suffixes
                // 1. check without rythm-suffix
                for (String s: suffixes) {
                    if (tagName.endsWith(s)) {
                        tagName = tagName.substring(0, tagName.lastIndexOf(s));
                        break;
                    }
                }
                tagNameProcessed = true;
            }
        }
        tagName = tagName.replace('.', '/');
        String sfx = codeType.resourceNameSuffix();
        if (S.notEmpty(sfx) && !suffixes.get(0).equals(sfx)) {
            suffixes.remove(sfx);
            suffixes.add(0, sfx);
        }

        List<String> roots = new ArrayList<String>();
        roots.add(RythmPlugin.templateRoot);

        // call tag with import path
        if (null != templateClass.importPaths) {
            for (String s: templateClass.importPaths) {
                roots.add(RythmPlugin.templateRoot + "/" + s.replace('.', '/'));
            }
        }

        String tagName0 = tagName;
        // call tag using relative path
        String currentPath = templateClass.getKey().toString();
        int pos = currentPath.lastIndexOf("/");
        if (-1 != pos) {
            currentPath = currentPath.substring(0, pos);
            if (currentPath.startsWith("/")) currentPath = currentPath.substring(1);
            if (!currentPath.startsWith(RythmPlugin.templateRoot)) currentPath = RythmPlugin.templateRoot + "/" + currentPath;
            roots.add(currentPath);
        }

        for (String root : roots) {
            tagName = root + "/" + tagName0;
            VirtualFile tagFile = null;
            for (String suffix : suffixes) {
                String name = tagName + suffix;
                tagFile = Play.getVirtualFile(name);
                File realFile = null == tagFile ? null : tagFile.getRealFile();
                if (null != realFile && realFile.canRead() && !realFile.isDirectory()) {
                    VirtualFileTemplateResource tr = new VirtualFileTemplateResource(tagFile);
                    TemplateClass tc = engine.classes().getByTemplate(tr.getKey());
                    if (null == tc) {
                        tc = new TemplateClass(tr, engine);
                    } else if (tc.equals(templateClass)) {
                        // call self
                        return templateClass;
                    }
                    ITag tag = (ITag) tc.asTemplate();
                    if (null != tag) {
                        String fullName = getFullTagName(tc, engine);
                        tc.setFullName(fullName);
                        engine.registerTag(fullName, tag);
                        return tc;
                    }
                }
            }
        }
        return processTagName ? tryLoadTag(tagNameOrigin, engine, templateClass, false) : null;
    }

    @Override
    public void scan(String root, TemplateResourceManager manager) {
        if (true) return;
        if (null == JobsPlugin.executor) return;
        final AtomicInteger failed = new AtomicInteger(0);
        for (VirtualFile vf: Play.templatesPath) {
            if (RythmPlugin.R_VIEW_ROOT.endsWith(vf.getName())) {
                scan(vf, manager, failed);
            }
        }
    }
    
    private void scan(final VirtualFile dir, final TemplateResourceManager manager, final AtomicInteger failed) {
        if (!dir.isDirectory() && dir.getRealFile().canRead()) {
            load(dir, manager, failed);
            return;
        }
        for (VirtualFile vf: dir.list()) {
            scan(vf, manager, failed);
        }
    }
    
    private void load(final VirtualFile file, final TemplateResourceManager manager, final AtomicInteger failed) {
        new Job() {
            @Override
            public void doJob() throws Exception {
                VirtualFileTemplateResource resource = new VirtualFileTemplateResource(file);
                if (!resource.isValid()) return;
                try {
                    RythmPlugin.info("preloading %s ...", resource.getKey());
                    manager.resourceLoaded(resource);
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            }
        }.now();
    }
}
