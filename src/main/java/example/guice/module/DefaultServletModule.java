/**
 * 
 */
package example.guice.module;

import org.apache.wicket.protocol.http.WebApplication;

import com.jolira.wicket.guicier.GuicierServletModule;

import example.WicketApplication;

/**
 * @author gena
 *
 */
public class DefaultServletModule extends GuicierServletModule {

	/* (non-Javadoc)
	 * @see com.jolira.wicket.guicier.GuicierServletModule#getWebApplicationClass()
	 */
	@Override
	protected Class<? extends WebApplication> getWebApplicationClass() {
		return WicketApplication.class;
	}

}
