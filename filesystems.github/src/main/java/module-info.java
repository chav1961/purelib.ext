/**
 * <p>This module contains Pure Library extension project content.</p>
 * @author Alexander Chernomyrdin aka chav1961
 * @see <a href="http://github.com/chav1961/purelib.ext">Pure Library extensions</a> project
 * @since 0.0.4
 */
module chav1961.purelib.ext.filesystems.git {
	requires transitive java.desktop;
	requires transitive java.scripting;
	requires java.xml;
	requires java.logging;
	requires jdk.jdi;
	requires jdk.unsupported;
	requires transitive java.sql;
	requires transitive java.rmi;
	requires java.management;
	requires transitive jdk.httpserver;
	requires transitive chav1961.purelib;
	
	uses chav1961.purelib.fsys.interfaces.FileSystemInterface;
	provides chav1961.purelib.fsys.interfaces.FileSystemInterface with chav1961.purelib.ext.fsys.github.FileSystemOnGitHub;
}
