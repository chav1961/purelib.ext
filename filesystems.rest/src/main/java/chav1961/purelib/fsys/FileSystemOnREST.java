package chav1961.purelib.fsys;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import javax.ws.rs.*;
import javax.xml.bind.annotation.XmlRootElement;

import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.fsys.interfaces.DataWrapperInterface;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.fsys.interfaces.RestMethodWrapperInterface;
import chav1961.purelib.fsys.interfaces.RestMethodWrapperInterface.RequestMethod;

@XmlRootElement
@Path("/")
public class FileSystemOnREST extends AbstractFileSystem {
	private static final Map<Class<?>,Class<?>>	PRIMITIVE2WRAPPERS = new HashMap<Class<?>,Class<?>>(){{}};
	private static final Set<Class<?>>			SUPPORTED_WRAPPERS = new HashSet<Class<?>>(){{}};

	private final Map<String,RestMethodWrapperInterface>	paths = new HashMap<String,RestMethodWrapperInterface>();
	private final URI	restDir; 
	
	public FileSystemOnREST(final URI location) throws IOException {
		final URI		packageString = URI.create(location.getSchemeSpecificPart());
		final String[]	packageList = packageString.getPath().split("\\;");
		final Package[]	packages = new Package[packageList.length];
		
		for (int index = 0; index < packages.length; index++) {
			if ((packages[index] = Package.getPackage(packageList[index])) == null) {
				throw new IllegalArgumentException("Package ["+packageList[index]+"] is not found in the class loader");
			}
		}
		this.restDir = location;
		
//		for (Class<?> cl : Utils.loadClasses(packages)) {
//			if (cl.isAnnotationPresent(Path.class)) {
//				System.err.println("Item="+cl.getName());
//				for (Method m : cl.getMethods()) {
//					if (m.isAnnotationPresent(GET.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.GET,paths);
//					}
//					if (m.isAnnotationPresent(POST.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.POST,paths);
//					}
//					if (m.isAnnotationPresent(PUT.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.PUT,paths);
//					}
//					if (m.isAnnotationPresent(DELETE.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.DELETE,paths);
//					}
//					if (m.isAnnotationPresent(HEAD.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.HEAD,paths);
//					}
//					if (m.isAnnotationPresent(OPTIONS.class)) {
//						processMethod(cl.getAnnotation(Path.class).value(),m,RestMethodWrapperInterface.RequestMethod.OPTIONS,paths);
//					}
//				}
//			}
//		}
	}
	
	private FileSystemOnREST(final FileSystemOnREST another) {
		this.restDir = another.restDir;
		this.paths.putAll(another.paths);
	}
	
	@Override
	public FileSystemInterface clone() {
		return new FileSystemOnREST(this);
	}

	@Override
	public DataWrapperInterface createDataWrapper(URI actualPath) throws IOException {
		return new RestWrapper(paths,actualPath);
	}
	

	private static void processMethod(final String path, final Method m, final RequestMethod rm, final Map<String, RestMethodWrapperInterface> paths) {
		final String[]		mimeProduces = m.isAnnotationPresent(Produces.class) ? m.getAnnotation(Produces.class).value() : new String[]{"text/plain"};
		final String[]		mimeConsumes = m.isAnnotationPresent(Consumes.class) ? m.getAnnotation(Consumes.class).value() : mimeProduces;
		final Parameter[]	parm = m.getParameters();
		final String[]		names = new String[parm.length];
		final Annotation[]	source = new Annotation[parm.length];
		final String[]		defaults = new String[parm.length];
		final boolean[]		encoded = new boolean[parm.length];
		
		for (int index = 0; index < parm.length; index++) {
			int	detected = 0;
			
			if (parm[index].isAnnotationPresent(CookieParam.class)) {
				source[index] = parm[index].getAnnotation(CookieParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(FormParam.class)) {
				source[index] = parm[index].getAnnotation(FormParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(HeaderParam.class)) {
				source[index] = parm[index].getAnnotation(HeaderParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(MatrixParam.class)) {
				source[index] = parm[index].getAnnotation(MatrixParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(PathParam.class)) {
				source[index] = parm[index].getAnnotation(PathParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(QueryParam.class)) {
				source[index] = parm[index].getAnnotation(QueryParam.class);
				detected++;
			}
			if (parm[index].isAnnotationPresent(DefaultValue.class)) {
				defaults[index] = parm[index].getAnnotation(DefaultValue.class).value();
			}
			if (parm[index].isAnnotationPresent(Encoded.class)) {
				encoded[index] = true;
			}
			names[index] = parm[index].getName();
			if (detected == 0 && defaults[index] == null) {
				throw new IllegalArgumentException("Class ["+m.getDeclaringClass()+"] method ["+m.getName()+"] parameter ["+parm[index].getName()+"] doesn't have neither source annotations nor default value");
			}
			else if (detected > 1) {
				throw new IllegalArgumentException("Class ["+m.getDeclaringClass()+"] method ["+m.getName()+"] parameter ["+parm[index].getName()+"] has more than one source annotaion");
			}
		}

		final Class<?>	returned = m.getReturnType();
		
		if (returned != Void.class && returned != String.class && !returned.isAnnotationPresent(XmlRootElement.class)) {
			throw new IllegalArgumentException("Class ["+m.getDeclaringClass()+"] method ["+m.getName()+"] return type ["+returned.getName()+"] need be void, String or any class annotated with @XmlRootElement");
		}
		else {
			paths.put(path,new MethodWrapper(URI.create(path),m,rm,mimeConsumes,mimeProduces,names,source,defaults,encoded));
		}
	}	

	private static class RestWrapper implements DataWrapperInterface {
		private final Map<String,RestMethodWrapperInterface>	paths;
		private final URI										actualPath;
		
		RestWrapper(final Map<String,RestMethodWrapperInterface> paths, final URI actualPath){
			this.paths = paths;			this.actualPath = actualPath;
		}

		@Override
		public URI[] list(final Pattern pattern) throws IOException {
			final Set<String>		collection = new HashSet<String>();
			final String			pathStart = actualPath.toString(); 
			
			for (String item : paths.keySet()) {
				if (item.startsWith(pathStart)) {
					final String	name = item.substring(pathStart.length()).split("\\/")[0];
					
					if (pattern.matcher(name).matches()) {
						collection.add(name);
					}
				}
			}
			final URI[]		result = new URI[collection.size()];
			int				index = 0;
			
			for (String item : collection) {
				result[index++] = URI.create(pathStart+'/'+item);
			}
				
			return result;
		}

		@Override
		public void mkDir() throws IOException {
			throw new IOException("Can't create directories in the REST file system");
		}

		@Override
		public void create() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setName(String name) throws IOException {
			throw new IOException("Can't rename entites in the REST file system");
		}

		@Override
		public void delete() throws IOException {
			// TODO Auto-generated method stub
			if ((Boolean)getAttributes().get(ATTR_DIR)) {
				throw new IOException("Can't delete directories in the REST file system");
			}
			else {
				
			}
		}

		@Override
		public OutputStream getOutputStream(boolean append) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Object> getAttributes() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void linkAttributes(Map<String, Object> attributes) throws IOException {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private static class MethodWrapper implements RestMethodWrapperInterface {
		private final URI				path;
		private final Method			method;
		private final RequestMethod		rm;
		private final String[]			sourceMime, producedMime;
		private final List<ParmDesc>	parm = new ArrayList<ParmDesc>();
		
		MethodWrapper(final URI path, final Method method, final RequestMethod rm, final String[] sourceMime, final String[] producedMime, final String[] parmName, final Annotation[] source, final String[] defaults, final boolean[] encoded) {
			this.path = path;		this.method = method;
			this.rm = rm;			this.sourceMime = sourceMime;
			this.producedMime = producedMime;
			
			for (int index = 0; index < source.length; index++){
				final int 	now = index;
				
				parm.add(new ParmDesc(){
					@Override 
					public ExtractMethod getExtractMethod() {
						if (source[now] instanceof CookieParam) {
							return ExtractMethod.Cookie;					
						}
						else if (source[now] instanceof FormParam) {
							return ExtractMethod.Form;					
						}
						else if (source[now] instanceof HeaderParam) {
							return ExtractMethod.Header;					
						}
						else if (source[now] instanceof MatrixParam) {
							return ExtractMethod.Matrix;					
						}
						else if (source[now] instanceof PathParam) {
							return ExtractMethod.Path;					
						}
						else if (source[now] instanceof QueryParam) {
							return ExtractMethod.Query;					
						}
						else {
							throw new UnsupportedOperationException("");
						}
					}

					@Override public String getName() {return parmName[now];}
					@Override public String getValue() {return defaults[now];}
					@Override public void setValue(String value) {defaults[now] = value;}
				});
			}
		}

		@Override public URI getPath() {return path;}

		@Override
		public boolean canProcess(final URI path) {
			if (path == null) {
				throw new IllegalArgumentException("Path can't be null");
			}
			else {
				final String[]	path1 = getPath().toString().split("\\/"), path2 = path.toString().split("\\/");
				
				if (path1.length != path2.length) {
					return false;
				}
				else {
					for (int index = 0; index < path1.length; index++) {
						if (!path1[index].startsWith("{") && !path1[index].equals(path2[index])) {
							return false;
						}
					}
					return true;
				}
			}
		}

		@Override public String[] sourceMime() {return sourceMime;}
		
		@Override 
		public boolean canpPocess(final String mime) {
			if (mime == null || mime.isEmpty()) {
				throw new IllegalArgumentException("MIME string can't be null or empty");
			}
			else {
				for (String item : sourceMime) {
					if (mime.equals(item)) {
						return true;
					}
				}
				return false;
			}
		}

		@Override public String[] producesMime() {return producedMime;}
		@Override public RequestMethod getMethod() {return rm;}

		@Override
		public boolean canProcess(final RequestMethod method) {
			if (method == null) {
				throw new IllegalArgumentException("Requrest method can't be null");
			}
			else {
				return getMethod() == method;
			}
		}

		@Override public Iterable<ParmDesc> parameters() {return parm;}

		@Override
		public InputStream invoke(final Object instance, final String sourceMime) throws IOException {
			final Class[]	parmClass = method.getParameterTypes();
			final Object[]	parmValues = new Object[parmClass.length]; 
			
			try{for (int index = 0; index < parm.size(); index++) {
					if (parmClass[index].isPrimitive()) {
						parmClass[index] = PRIMITIVE2WRAPPERS.get(parmClass[index]);
					}
					if (SUPPORTED_WRAPPERS.contains(parmClass[index])) {
						parmValues[index] = parmClass[index].getMethod("valueOf",String.class).invoke(null,parm.get(index).getValue());
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			return null;
		}		
	}

	@Override
	public boolean canServe(URI uriSchema) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FileSystemInterface newInstance(URI uriSchema) throws EnvironmentException {
		// TODO Auto-generated method stub
		return null;
	}
}
