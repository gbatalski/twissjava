package example;

import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.OperandPolicy;
import org.scale7.cassandra.pelops.Pelops;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run the Start class.
 */
public class WicketApplication extends WebApplication {

	private static final String hostPort = "localhost:9160";

	private static final String clusterName = "Nebula Cassandra Cluster";

	private static final String keyspace = "twissjava";

	/**
	 * Constructor
	 */
	public WicketApplication() {
		Pelops.addPool("Twissjava Pool", new Cluster(	"127.0.0.1",
														9160),keyspace );

		Base.cassandra = new CassandraService(	hostPort,
												clusterName,
												keyspace);
		Base.cassandra.createColumnFamilyIfAbsent(Base.USERS);
		Base.cassandra.createColumnFamilyIfAbsent(Base.TWEETS);
		Base.cassandra.createColumnFamilyIfAbsent(Base.FOLLOWERS);
		Base.cassandra.createColumnFamilyIfAbsent(Base.FRIENDS);
		Base.cassandra.createColumnFamilyIfAbsent(Base.TIMELINE);
		Base.cassandra.createColumnFamilyIfAbsent(Base.USERLINE);
	}

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	public Class<Userline> getHomePage() {
		return Userline.class;
	}

	@Override
	public Session newSession(Request request, Response response) {
		return new TwissSession(request);
	}
}