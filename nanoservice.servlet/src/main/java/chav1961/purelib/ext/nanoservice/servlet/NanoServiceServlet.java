package chav1961.purelib.ext.nanoservice.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import chav1961.purelib.basic.AbstractLoggerFacade;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.nanoservice.NanoServiceFactory;

public class NanoServiceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final SubstitutableProperties	settings;
	private final SubstitutableProperties	props = new SubstitutableProperties();
	private volatile NanoServiceFactory		factory = null;
	private volatile LoggerFacade			logger = null;
	
    /**
     * Default constructor. 
     */
    public NanoServiceServlet() {
    	this.settings = PureLibSettings.instance();
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
    	super.init(config);
    	
    	final Enumeration<String>	names = config.getInitParameterNames();
    	
    	while(names.hasMoreElements()) {
    		final String	name = names.nextElement();
    		
    		props.setProperty(name,config.getInitParameter(name));
    	}
    	
    	try{this.logger = new ServletLogger(getServletContext());
			this.factory = new NanoServiceFactory(logger,props);
			
			this.factory.start();
		} catch (ContentException | IOException e) {
			throw new ServletException(e.getLocalizedMessage(),e);
		}
    }
    
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	@Override
	protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		super.doPut(req, resp);
	}
	
	@Override
	protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);
	}
	
	@Override
	public void destroy() {
		try{this.factory.stop();
		} catch (IOException e) {
			logger.message(Severity.error,e,"Error on nano service stop: "+e.getLocalizedMessage());
		}
		super.destroy();
	}

	private static class ServletLogger extends AbstractLoggerFacade {
		private final ServletContext	context;
		
		private ServletLogger(final ServletContext context) {
			this.context = context;
		}
		
		@Override
		protected AbstractLoggerFacade getAbstractLoggerFacade(final String mark, final Class<?> root) {
			return new ServletLogger(context);
		}

		@Override
		protected void toLogger(final Severity level, final String text, final Throwable throwable) {
			if (throwable != null) {
		    	context.log(text,throwable);
			}
			else {
				context.log(text);
			}
		}

		@Override
		public boolean canServe(URI resource) throws NullPointerException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public LoggerFacade newInstance(URI resource) throws EnvironmentException, NullPointerException, IllegalArgumentException {
			// TODO Auto-generated method stub
			return null;
		}
	};

}
