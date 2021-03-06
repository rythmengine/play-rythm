package org.rythmengine.play.utils;

import org.rythmengine.play.RythmPlugin;
import com.stevesoft.pat.Regex;
import play.Play;
import play.exceptions.NoRouteFoundException;
import play.libs.IO;
import play.mvc.Http;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.rythmengine.play.RythmPlugin.error;
import static org.rythmengine.play.RythmPlugin.warn;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 17/07/12
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class StaticRouteResolver {

    private static Map<String, String> routes = new HashMap<String, String>();
    private static List<String> urlList = new ArrayList<String>();
    private static Map<String, String> reverseRoutes = new HashMap<String, String>();

    public static String reverseWithCheck(String name, boolean absolute) {
        // play's reverse static url resolution has problem: it can't handle @url(/js/x.js) if you have /js shortcut to public/javascripts
        //if (!Router.routes.isEmpty()) return Router.reverse(file, absolute);

        VirtualFile file = Play.getVirtualFile(name);
        if (file == null || !file.exists()) {
            return verify(name, absolute);
        } else {
            return reverse(file, absolute);
        }
    }

    private static String getBaseUrl() {
        if (Http.Request.current() == null) {
            // No current request is present - must get baseUrl from config
            String appBaseUrl = Play.configuration.getProperty("application.baseUrl", "application.baseUrl");
            if (appBaseUrl.endsWith("/")) {
                // remove the trailing slash
                appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
            }
            return appBaseUrl;

        } else {
            return Http.Request.current().getBase();
        }
    }

    private static String reverse(VirtualFile file, boolean absolute) {
        String path = file.relativePath();
        path = path.substring(path.indexOf("}") + 1);
        for (String url : urlList) {
            String staticDir = routes.get(url);
            if (null == staticDir) {
                RythmPlugin.warn("url not found in routes: %s", url);
                continue;
            }
            if (!staticDir.startsWith("/")) {
                staticDir = "/" + staticDir;
            }
            if (!staticDir.equals("/") && !staticDir.endsWith("/")) {
                staticDir = staticDir + "/";
            }
            if (path.startsWith(staticDir)) {
                String to = url + path.substring(staticDir.length());
                if (to.endsWith("/index.html")) {
                    to = to.substring(0, to.length() - "/index.html".length() + 1);
                }
                if (absolute) {
                    boolean isSecure = Http.Request.current() == null ? false : Http.Request.current().secure;
                    String base = getBaseUrl();
                    to = base + to;
//                    if (!StringUtils.isEmpty(route.host)) {
//                        // Compute the host
//                        int port = Http.Request.current() == null ? 80 : Http.Request.current().get().port;
//                        String host = (port != 80 && port != 443) ? route.host + ":" + port : route.host;
//                        to = (isSecure ? "https://" : "http://") + host + to;
//                    } else {
//                        to = base + to;
//                    }
                }
                return to;
            }
        }
        throw new NoRouteFoundException(file.relativePath());
    }

    private static String verify(String name, boolean absolute) {
        String path = name;
        for (String url : urlList) {
            if (!path.startsWith(url)) continue;
            String staticDir = routes.get(url);
            if (staticDir != null) {
                if (!staticDir.startsWith("/")) {
                    staticDir = "/" + staticDir;
                }
                if (!staticDir.equals("/") && !staticDir.endsWith("/")) {
                    staticDir = staticDir + "/";
                }
                String to = path.replaceFirst(url, staticDir);
                VirtualFile vf = Play.getVirtualFile(to);
                if (null == vf || !vf.exists()) throw new NoRouteFoundException(name);
                if (to.endsWith("/index.html")) {
                    to = to.substring(0, to.length() - "/index.html".length() + 1);
                }
                if (reverseRoutes.containsKey(staticDir)) {
                    name = name.replace(url, reverseRoutes.get(staticDir));
                }
                if (absolute) {
                    boolean isSecure = Http.Request.current() == null ? false : Http.Request.current().secure;
                    String base = getBaseUrl();
//                        if (!S.isEmpty(route.host)) {
//                            // Compute the host
//                            int port = Http.Request.current() == null ? 80 : Http.Request.current().get().port;
//                            String host = (port != 80 && port != 443) ? route.host + ":" + port : route.host;
//                            to = (isSecure ? "https://" : "http://") + host + to;
//                        } else {
//                            to = base + to;
//                        }
                    return base + name;
                }
                return name;
            }
        }
        throw new NoRouteFoundException(name);
    }

    public static void loadStaticRoutes() {
        String prefix = Play.ctxPath;
        if (null == prefix || "/".equals(prefix)) prefix = "";
        parse(Play.getVirtualFile("conf/routes"), prefix);
    }

    private static void parse(VirtualFile vf, String prefix) {
        List<String> ls = IO.readLines(vf.inputstream());
        for (String s : ls) {
            s = s.trim();
            if (s.startsWith("#")) continue;
            if (s.contains("module:")) {
                importModule(s, prefix);
            }
            if (s.contains("staticDir:") || s.contains("staticFile:")) parseLine(s, prefix, vf);
        }
    }

    private static void importModule(String line, String prefix) {
        String[] sa = line.split("\\s+");
        if (!(sa.length == 3)) {
            warn("invalid route line: %s", line);
            return;
        }
        String url = sa[1];
        String newPrefix = prefix + url;
        if (newPrefix.length() > 1 && newPrefix.endsWith("/")) {
            newPrefix = newPrefix.substring(0, newPrefix.length() - 1);
        }
        String moduleName = sa[2].substring("module:".length());
        if ("*".equals(moduleName)) {
            for (String p : Play.modulesRoutes.keySet()) {
                parse(Play.modulesRoutes.get(p), newPrefix + p);
            }
        } else if (Play.modulesRoutes.containsKey(moduleName)) {
            parse(Play.modulesRoutes.get(moduleName), newPrefix);
        }
    }

    private static void parseLine(String line, String prefix, VirtualFile routesConf) {
        String[] sa = line.split("\\s+");
        if (!(sa.length == 3)) {
            warn("invalid route line: %s", line);
            return;
        }

        String url = prefix + sa[1];
        if (!url.endsWith("/")) url = url + "/";

        String path = sa[2];
        if (path.contains("staticDir:")) path = path.substring("staticDir:".length());
        else if (path.contains("staticFile:")) path = path.substring("staticFile:".length());
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        VirtualFile vf = Play.getVirtualFile(path);
        if (vf == null || !vf.exists()) {
            error("Bad routes file [%s]: staticDir[%s] not found", routesConf.relativePath(), path);
            return;
        }
        routes.put(url, vf.relativePath());
        if (!urlList.contains(url)) urlList.add(url);
    }

    public static void processVersionedRoutes() {
        Map<String, String> m = new HashMap<String, String>(routes);
        routes.clear();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("play", new Play());
        for (String k : m.keySet()) {
            String v = m.get(k);
            Regex r = new Regex(".*(%(?@{})%).*");
            if (r.search(k)) {
                StringWriter sw = new StringWriter();
                params.put("out", new PrintWriter(sw));
                String script = r.stringMatched(1);
                TemplateLoader.loadString(script).render(params);
                String s = sw.toString();
                r = new Regex("(%(?@{})%)", s);
                s = r.replaceAll(k);
                routes.put(s, v);
                if (!v.endsWith("/")) v = v + "/";
                reverseRoutes.put(v, s);
                r = new Regex("(%(?@{})%)", "");
                s = r.replaceAll(k);
                s = s.replace("//", "/");
                routes.put(s, v);
            } else {
                routes.put(k, v);
            }
        }

        List<String> l = new ArrayList<String>(urlList);
        urlList.clear();
        params.put("play", new Play());
        for (String s : l) {
            if (!urlList.contains(s)) urlList.add(s);
            Regex r = new Regex(".*(%(?@{})%).*");
            if (r.search(s)) {
                StringWriter sw = new StringWriter();
                params.put("out", new PrintWriter(sw));
                String script = r.stringMatched(1);
                TemplateLoader.loadString(script).render(params);
                String s0 = sw.toString();
                r = new Regex("(%(?@{})%)", s0);
                s0 = r.replaceAll(s);
                if (!urlList.contains(s0)) urlList.add(s0);
                r = new Regex("(%(?@{})%)", "");
                s0 = r.replaceAll(s);
                s0 = s0.replace("//", "/");
                if (!urlList.contains(s0)) urlList.add(s0);
            }
        }
    }

}
