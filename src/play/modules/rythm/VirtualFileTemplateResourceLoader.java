package play.modules.rythm;

import com.greenlaw110.rythm.RythmEngine;
import com.greenlaw110.rythm.internal.compiler.TemplateClass;
import com.greenlaw110.rythm.resource.ITemplateResource;
import com.greenlaw110.rythm.resource.ITemplateResourceLoader;
import com.greenlaw110.rythm.resource.TemplateResourceBase;
import com.greenlaw110.rythm.runtime.ITag;
import play.Logger;
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
        return file.exists() && file.getRealFile().canRead();
    }

    @Override
    public ITemplateResource load(String path) {
        if (path.contains(".svn")) return null; // definitely we don't want to load anything inside there
        if (RythmPlugin.defaultEngine == RythmPlugin.EngineType.system) {
            // by default use groovy template unless it's in the white list
            if (!RythmTemplateLoader.whiteListed(path)) return null;
        } else {
            // by default use rythm template unless it's in the black list
            if (RythmTemplateLoader.blackListed(path)) return null;
        }

        VirtualFile file = VirtualFile.fromRelativePath(path);
        if (null == file) {
            Logger.error("BAD PATH: %s", path);
            return null;
        }
        return isValid(file) ? new VirtualFileTemplateResource(file) : null;
    }

    @Override
    public void tryLoadTag(String tagName) {
        RythmEngine engine = RythmPlugin.engine;
        if (engine.tags.containsKey(tagName)) return;
        tagName = tagName.replace('.', '/');
        final String[] suffixes = {
                ".html",
                ".json",
                ".tag"
        };
        tagName = RythmPlugin.tagRoot + "/" + tagName;
        VirtualFile tagFile = null;
        for (String suffix: suffixes) {
            String name = tagName + suffix;
            tagFile = Play.getVirtualFile(name);
            if (null != tagFile && tagFile.getRealFile().canRead()) {
                try {
                    VirtualFileTemplateResource tr = new VirtualFileTemplateResource(tagFile);
                    TemplateClass tc = engine.classes.getByTemplate(tr.getKey());
                    if (null == tc) {
                        tc = new TemplateClass(tr, engine);
                        ITag tag = (ITag)tc.asTemplate();
                        if (null != tag) engine.registerTag(tag);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

}
