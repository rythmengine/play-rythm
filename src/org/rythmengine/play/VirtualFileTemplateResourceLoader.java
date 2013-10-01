package org.rythmengine.play;

import org.rythmengine.RythmEngine;
import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.resource.ResourceLoaderBase;
import org.rythmengine.resource.TemplateResourceBase;
import org.rythmengine.resource.TemplateResourceManager;
import play.Play;
import play.libs.IO;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implement a VirtualFile based resource loader
 */
public class VirtualFileTemplateResourceLoader extends ResourceLoaderBase {

    public static VirtualFileTemplateResourceLoader instance = new VirtualFileTemplateResourceLoader();

    public static class VirtualFileTemplateResource extends TemplateResourceBase {

        private static final long serialVersionUID = -4307922939957393745L;

        private VirtualFile file;

        VirtualFileTemplateResource(VirtualFile file) {
            if (null == file) throw new NullPointerException();
            this.file = file;
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
                VirtualFileTemplateResource that = (VirtualFileTemplateResource) obj;
                return (that.file == this.file) || that.file.equals(this.file);
            }
            return false;
        }
    }

    public static boolean isValid(VirtualFile file) {
        return (null != file) && file.exists() && !file.isDirectory() && file.getRealFile().canRead();
    }

    private VirtualFile loadFromPath_(String path) {
        VirtualFile vf;
        if (path.indexOf("module:") != -1) vf = VirtualFile.fromRelativePath(path);
        else {
            vf = Play.getVirtualFile(path);
            if (!isValid(vf)) {
                if (!path.startsWith("/")) path = "/" + path;
                if (!isValid(vf) && !path.startsWith(RythmPlugin.templateRoot)) {
                    String path0 = RythmPlugin.templateRoot + path;
                    vf = Play.getVirtualFile(path0);
                }
            }
        }
        return vf;
    }

    @Override
    public String getResourceLoaderRoot() {
        return RythmPlugin.templateRoot;
    }

    @Override
    protected RythmEngine getDefaultEngine() {
        return RythmPlugin.engine;
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
    public void scan(TemplateResourceManager manager) {
        //if (null == JobsPlugin.executor) return;
        final AtomicInteger failed = new AtomicInteger(0);
        for (VirtualFile vf : Play.templatesPath) {
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
        for (VirtualFile vf : dir.list()) {
            scan(vf, manager, failed);
        }
    }

    private void load(final VirtualFile file, final TemplateResourceManager manager, final AtomicInteger failed) {
//        new Job() {
//            @Override
//            public void doJob() throws Exception {
//                VirtualFileTemplateResource resource = new VirtualFileTemplateResource(file);
//                if (!resource.isValid()) return;
//                try {
//                    RythmPlugin.info("preloading %s ...", resource.getKey());
//                    String path = RythmTemplateLoader.templatePath(file);
//                    if (null == path) return;
//                    Template t = RythmTemplateLoader.cachedTemplate(path);
//                    if (null != t) return;
//                    manager.resourceLoaded(resource, false);
//                    RythmTemplateLoader.createTemplate(file, path);
//                } catch (Exception e) {
//                    failed.incrementAndGet();
//                }
//            }
//        }.now();

        VirtualFileTemplateResource resource = new VirtualFileTemplateResource(file);
        if (!resource.isValid()) return;
        try {
            RythmPlugin.info("preloading %s ...", resource.getKey());
            String path = RythmTemplateLoader.templatePath(file);
            if (null == path) return;
            Template t = RythmTemplateLoader.cachedTemplate(path);
            if (null != t) return;
            manager.resourceLoaded(resource, false);
            RythmTemplateLoader.createTemplate(file, path);
        } catch (Exception e) {
            failed.incrementAndGet();
        }
    }
}
