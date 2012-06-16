/**
 * 
 */
package example.servlet;

import com.google.inject.Stage;
import com.jolira.wicket.guicier.GuicierServletConfig;

/**
 * @author gena
 *
 */
public class WicketGuicierServletConfig extends GuicierServletConfig {

	/* (non-Javadoc)
	 * @see com.jolira.wicket.guicier.GuicierServletConfig#getConfigurationType()
	 */
	@Override
	protected Stage getConfigurationType() {

		return Stage.PRODUCTION;
	}

}
