package chav1961.purelib.fsys;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.util.SVNSSLUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import chav1961.purelib.basic.Utils;
import chav1961.purelib.fsys.interfaces.DataWrapperInterface;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class FileSystemOnSvn extends AbstractFileSystem {
	public static final String			QUERY_USER = "user";
	public static final String			QUERY_SSH_KEYFILE = "sshKeyFile";
	public static final String			QUERY_SSH_PASSWORD = "sshKey";
	public static final String			QUERY_AUTH_PASSWORD = "authKey";
	
	private	final URI					rootPath;
	private final SVNRepository			repo;
	
	public FileSystemOnSvn(final URI rootPath) throws IOException {
		super();
		if (rootPath == null) {
			throw new IllegalArgumentException("Root path can't be null");
		}
		else if (rootPath.getQuery() == null || rootPath.getQuery().isEmpty()) {
			throw new IllegalArgumentException("Root path not cantains additional parameters");
		}
		else {
			this.rootPath = rootPath;
			final Properties	props = new Properties();
			
			try(final Reader 	rdr = new StringReader(rootPath.getQuery().replace('&','\n'))) {
				props.load(rdr);
			}
			
	        try{final SVNURL url = SVNURL.parseURIEncoded(rootPath.toString());
	
	            repo = SVNRepositoryFactory.create(url);
	            repo.setAuthenticationManager(createAuthManager(url,props.getProperty(QUERY_USER),new File(props.getProperty(QUERY_SSH_KEYFILE)),props.getProperty(QUERY_SSH_PASSWORD).toCharArray(),props.getProperty(QUERY_AUTH_PASSWORD).toCharArray(),new File(props.getProperty(QUERY_SSH_KEYFILE))));
	        } catch (SVNException e) {
	            throw new IOException("SVN Preparation error: "+e.getMessage());
	        }
		}
	}

	private FileSystemOnSvn(final FileSystemOnSvn another) {
		super(another);
		this.rootPath = another.rootPath;
		this.repo = another.repo;
	}

	@Override
	public boolean canServe(final String uriSchema) {
		return uriSchema.startsWith("svn");
	}
	
	@Override
	public FileSystemInterface clone() {
		return new FileSystemOnSvn(this);
	}

	@Override
	public DataWrapperInterface createDataWrapper(final URI actualPath) throws IOException {
		return new SVNDataWrapper(actualPath,rootPath,repo);
	}
	
	private static class SVNDataWrapper implements DataWrapperInterface {
		private final URI			wrapper;
		private final SVNRepository	repo; 
		
		public SVNDataWrapper(final URI wrapper, final URI rootPath, final SVNRepository repo) {
			this.wrapper = wrapper;
			this.repo = repo;
		}

		@Override
		public OutputStream getOutputStream(boolean append) throws IOException {
			return new FileOutputStream(new File(wrapper),append);
		}

		@Override
		public InputStream getInputStream() throws IOException {
	        final SVNProperties fileProperties = new SVNProperties();
	        
	        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	            repo.getFile(wrapper.toString(), -1, fileProperties, baos);
	            return new ByteArrayInputStream(baos.toByteArray());
	        } catch (SVNException ex) {
	            throw new IOException(ex.getMessage(),ex);
	        }
		}

		@Override
		public URI[] list(final Pattern pattern) throws IOException {
	        final SVNProperties fileProperties = new SVNProperties();
	        final List<URI> found = new ArrayList<URI>();

	        try{repo.getDir(wrapper.toString(), -1, fileProperties, new ISVNDirEntryHandler() {
	                @Override
	                public void handleDirEntry(SVNDirEntry ent) throws SVNException {
	                    found.add(URI.create(wrapper+"/"+ent.getName()));
	                }
	            });
	            final URI[]		returned = found.toArray(new URI[found.size()]);
	            
	            found.clear();
	            return returned;
	        } catch (SVNException ex) {    
	            throw new IOException(ex.getMessage(),ex);
	        }
		}

		@Override
		public void mkDir() throws IOException {
			throw new IOException("ReadOnly");
		}

		@Override
		public void create() throws IOException {
			throw new IOException("ReadOnly");
		}

		@Override
		public void delete() throws IOException {
			throw new IOException("ReadOnly");
		}

		@Override
		public Map<String, Object> getAttributes() throws IOException {
			try{final SVNDirEntry	info = repo.info(wrapper.toString(),-1);
				
				return Utils.mkMap(ATTR_SIZE, info.getSize(), ATTR_NAME, info.getName(), ATTR_LASTMODIFIED, info.getDate(), ATTR_DIR, info.getKind().equals(SVNNodeKind.DIR), ATTR_EXIST, true, ATTR_CANREAD, true, ATTR_CANWRITE, false);
			} catch (SVNException e) {
				throw new IOException(e.getMessage());
			}
		}

		@Override 
		public void linkAttributes(Map<String, Object> attributes) throws IOException {
			
		}
		
		@Override
		public void setName(final String name) throws IOException {
			throw new IOException("ReadOnly");
		}
	}

    private AuthManager createAuthManager(final SVNURL url, final String authUser, final File sshKeyFile, final char[] sshKeyPassword, final char[] authPassword, final File svnServerCertFile) {
        if (authUser == null || authUser.isEmpty()) {
            if (svnServerCertFile != null) {
                return new AuthManager(new SVNAuthentication[0], svnServerCertFile);
            }
            else {
                return new AuthManager(new SVNAuthentication[0]);
            }
        }
        else {
            final List<SVNAuthentication> auths = new ArrayList<>();
            
            if (sshKeyFile != null) {
                auths.add(SVNSSHAuthentication.newInstance(authUser,sshKeyFile,sshKeyPassword,22,false,url,true));
            }
            if (authPassword != null && authPassword.length > 0) {
            	
                auths.add(SVNPasswordAuthentication.newInstance(authUser,authPassword,false,url,false));
            }
            else {
                auths.add(SVNUserNameAuthentication.newInstance(authUser,false,url,false));
            }
            if (svnServerCertFile != null) {
                return new AuthManager(auths.toArray(new SVNAuthentication[auths.size()]));
            }
            else {
                return new AuthManager(auths.toArray(new SVNAuthentication[auths.size()]), svnServerCertFile);
            }
        }
    }

    private class AuthManager extends BasicAuthenticationManager {
        private final File svnServerCertFile;

        public AuthManager(final SVNAuthentication[] authentications) {
            super(authentications);
            this.svnServerCertFile = null;
        }
        
        public AuthManager(final SVNAuthentication[] authentications, final File svnServerCertFile) {
            super(authentications);
            this.svnServerCertFile = svnServerCertFile;
        }

        @Override
        public TrustManager getTrustManager(final SVNURL url) throws SVNException {
            return new X509TrustManager() {
                @Override
                public void checkClientTrusted(final X509Certificate[] xcs, final String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] xcs, final String string) throws CertificateException {
//                    if (suppressSvnCertCheck) {
//                        return;
//                    }
                    if (xcs != null) {
                        if (svnServerCertFile == null) {
                            throw new SVNSSLUtil.CertificateNotTrustedException("Can't check SVN server certificate: parameter -svnServerCertFile is not defined.");
                        }
                        FileInputStream f = null;
                        try {
                            f = new FileInputStream(svnServerCertFile);
                            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                            final Certificate cer = certificateFactory.generateCertificate(f);
                            if (Arrays.asList(xcs).contains(cer)) {
                                return;
                            }
                        } catch (CertificateException | FileNotFoundException e) {
                            throw new SVNSSLUtil.CertificateNotTrustedException("Can't load trusted certificate for SVN server: " + e.getMessage());
                        } finally {
                            if (f != null) {
                                try {
                                    f.close();
                                } catch (IOException e) {
                                    //ignore
                                }
                            }
                        }
                    }
                    throw new SVNSSLUtil.CertificateNotTrustedException("SVN server's certificate chain is not trusted by Starter");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
        }
    }



}
