package krystal.framework.tomcat;

import jakarta.servlet.http.HttpServlet;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Define properties through convenient {@link TomcatProperties#builder()} method.
 * <dl>
 *     <dt><b><i>baseDir</i></b></dt><dd>Root dir for all other Tomcat references.<br /><code>Default: <i>{@link KrystalFramework#getExposedDirPath() exposedDirPath}/tomcat</i></code></dd>
 *     <dt><b><i>hostName</i></b></i></b></dt><dd><code>Default: <i>localhost</i></code></dd>
 *     <dt><b><i>port</i></b></dt><dd><code>Default: <i>8080</i></code></dd>
 *     <dt><b><i>servletsRootContext</i></b></dt><dd>Root mapping for default context. <br /><code>Default: <i>/api</i></code></dd>
 *     <dt><b><i>servlets</i></b></dt><dd>The list of {@link KrystalHttpServlet}s to be added to the default context created.</dd>
 *     <dt><b><i>webappsDir</i></b></dt><dd>Root directory to store web applications in.<br /><code>Default: <i>/webapps</i></code></dd>
 *     <dt><b><i>appName</i></b></dt><dd>Defines the top level root context for the web application, added by providing <i>appSrc</i>. Any whitespaces and slash characters will be replaced with "_". <br /><code>Default: /app<i></i></code></dd>
 *     <dt><b><i>appSrc</i></b></dt><dd>When provided, the {@link TomcatFactory} loads web application's content from given source - if <code>.war</code> file -> will unpack into <code><i>./baseDir/webappsDir/appName/</i></code> or  if the path to dir already present in the file system -> will be addressed directly.</dd>
 *     <dt><b><i>appSrcAsCollection</i></b></dt><dd>If <code><i>true</i></code> and <code><i>appSrc</i></code> is a directory, it will import <code>.war</code> files content from it and treat it's top-level sub-dirs as web-apps directly.</dd>
 * </dl>
 */
@Builder
@Getter
public class TomcatProperties {
	
	@Builder.Default private String baseDir = Tools.concatAsURIPath(KrystalFramework.getExposedDirPath(), "tomcat");
	@Builder.Default private String hostName = "localhost";
	@Builder.Default private int port = 8080;
	@Builder.Default private String servletsRootContext = "/api";
	@Singular private List<HttpServlet> servlets;
	@Builder.Default private String webappsDir = "webapps";
	@Builder.Default private String appName = "/app";
	private String appSrc;
	private boolean appSrcAsCollection;
	
}