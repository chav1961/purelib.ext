package chav1961.purelib.ext.fsys.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.ext.fsys.git.internal.PureLibClient;
import chav1961.purelib.fsys.AbstractFileSystem;
import chav1961.purelib.fsys.interfaces.DataWrapperInterface;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.streams.interfaces.JsonSaxHandler;

public class FileSystemOnGitHub extends AbstractFileSystem {
	static {
		PureLibClient.registerInPureLib();
	}
	
	private final URI	rootPath;
	/**
	 * <p>This constructor is an entry for the SPI service only. Don't use it in any purposes</p> 
	 */
	public FileSystemOnGitHub(){
		rootPath = null;
	}
	
	/**
	 * <p>Create the file system for the given directory.  
	 * @param rootPath root directory for the file system. Need be absolute URI with the schema 'file', for example <code>'file://./c:/mydir'</code>
	 * @throws IOException if any exception was thrown
	 */
	public FileSystemOnGitHub(final URI rootPath) throws IOException {
		if (rootPath == null) {
			throw new IllegalArgumentException("Root path can't be null");
		}
		else if (!rootPath.isAbsolute()) {
			throw new IllegalArgumentException("Root path ["+rootPath+"] is not absolute URI or not contains scheme");
		}
		else if (!rootPath.getScheme().equals("github")) {
			throw new IllegalArgumentException("Root path ["+rootPath+"] not contains 'github:' as scheme");
		}
		else {
			this.rootPath = rootPath.normalize();
		}
	}
	
	private FileSystemOnGitHub(final FileSystemOnGitHub another) {
		super(another);
		this.rootPath = another.rootPath;
	}

	@Override
	public FileSystemInterface clone() {
		return new FileSystemOnGitHub(this);
	}

	@Override
	public DataWrapperInterface createDataWrapper(final URI actualPath) throws IOException {
		return new GitHubDataWrapper(actualPath,URI.create(rootPath.getSchemeSpecificPart()));
	}
	
	private static class GitHubDataWrapper implements DataWrapperInterface {
		private final URI	wrapper;
		
		public GitHubDataWrapper(final URI wrapper, final URI rootPath) {
			this.wrapper = URI.create(rootPath.toString()+wrapper.toString()).normalize();
		}

		@Override
		public URI[] list(final Pattern pattern) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void mkDir() throws IOException {
			throw new IOException("Attempt to create directory on read-only filesystem");
		}

		@Override
		public void create() throws IOException {
			throw new IOException("Attempt to create file on read-only filesystem");
		}

		@Override
		public void setName(String name) throws IOException {
			throw new IOException("Attempt to rename entity on read-only filesystem");
		}

		@Override
		public void delete() throws IOException {
			throw new IOException("Attempt to delete entity on read-only filesystem");
		}

		@Override
		public OutputStream getOutputStream(boolean append) throws IOException {
			throw new IOException("Attempt to write file on read-only filesystem");
		}

		@Override
		public InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Object> getAttributes() throws IOException {
//			final Map<String, Object>	info = Utils.mkMap(ATTR_SIZE, !isDirectory ? content.getBytes().length : 0
//												, ATTR_NAME, node.getNodeName()
//												, ATTR_LASTMODIFIED, 0
//												, ATTR_DIR, isDirectory
//												, ATTR_EXIST, true
//												, ATTR_CANREAD, true
//												, ATTR_CANWRITE, false
//											);
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void linkAttributes(Map<String, Object> attributes) throws IOException {
		}

		@Override
		public boolean tryLock(String path, boolean sharedMode) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void lock(String path, boolean sharedMode) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unlock(String path, boolean sharedMode) throws IOException {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public boolean canServe(URI uriSchema) {
		return false;
	}

	@Override
	public FileSystemInterface newInstance(URI uriSchema) throws EnvironmentException {
		return null;
	}
}
