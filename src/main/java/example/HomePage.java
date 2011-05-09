package example;


import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * This page points to either Publicline (if viewer is not logged in) or
 *  Userline (for the viewer that is logged in).
 */
public class HomePage extends Base {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5258693984810768803L;

	/**
	 * Constructor that is invoked when page is invoked without a session.
	 * 
	 * @param parameters
	 */
    public HomePage(final PageParameters parameters) {
        super(parameters);
    }
}
