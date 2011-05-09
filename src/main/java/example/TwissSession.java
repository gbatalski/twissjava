package example;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;

public class TwissSession extends WebSession {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8009346529502327012L;
	private String uname;

    public TwissSession(Request request) {
        super(request);
    }

    public String getUname() {
        return uname;
    }
    
    public void authorize(String uname){
        this.uname = uname;
    }
}
