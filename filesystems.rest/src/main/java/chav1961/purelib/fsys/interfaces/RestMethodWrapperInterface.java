package chav1961.purelib.fsys.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface RestMethodWrapperInterface {
	public enum RequestMethod {
		GET, POST, PUT, DELETE, HEAD, OPTIONS
	}

	public enum ExtractMethod {
		Cookie, Form, Header, Matrix, Path, Query
	}	

	public interface ParmDesc {
		ExtractMethod getExtractMethod();
		String getName();
		String getValue();
		void setValue(String value);
	}
	
	URI getPath();
	boolean canProcess(URI path);

	String[] sourceMime();
	boolean canpPocess(String mime);
	
	String[] producesMime();
	
	RequestMethod getMethod();
	boolean canProcess(RequestMethod method);
	
	Iterable<ParmDesc> parameters();
	
	InputStream invoke(Object instance, String sourceMime) throws IOException;
}
