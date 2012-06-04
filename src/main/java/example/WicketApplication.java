package example;


import org.apache.wicket.Session;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jolira.wicket.guicier.GuicierWebApplication;

import example.services.db.cassandra.SchemaUtils;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run the Start class.
 */
public class WicketApplication extends GuicierWebApplication {


	@Inject
	private SchemaUtils schemaUtils;
	/**
	 * Constructor
	 */
	@Inject
	public WicketApplication(final Injector injector) {
		super(injector);
	}

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<Userline> getHomePage() {
		return Userline.class;
	}

	@Override
	public Session newSession(Request request, Response response) {
		return new TwissSession(request);
	}

	@Override
	protected void init() {

		super.init();
		// deploy schema if not already done yet
		schemaUtils.deploySchema();
	}


}