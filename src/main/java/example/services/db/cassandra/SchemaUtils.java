/**
 * 
 */
package example.services.db.cassandra;

import java.io.File;
import java.security.Permission;

import org.apache.cassandra.cli.CliMain;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import example.guice.annotation.Host;
import example.guice.annotation.Port;
import example.guice.annotation.SchemaFilename;

/**
 * @author gena
 * 
 */
@Singleton
public class SchemaUtils {
	private final String host;

	private final Integer port;

	private final File schemaFile;

	@Inject
	public SchemaUtils(@Host String host, @Port Integer port,
			@SchemaFilename String schemaFile) {
		this.host = host;
		this.port = port;
		this.schemaFile = new File(this.getClass()
										.getResource("/" + schemaFile)
										.getFile());
	}

	public void deploySchema() {
		new Runnable() {
			@Override
			public void run() {
				final SecurityManager securityManager = new SecurityManager() {
					@Override
					public void checkPermission(Permission permission) {
						// If you want to forbid any kind of halt, just use
						// "exitVM"
						if (permission.getName()
										.contains("exitVM")) {
							throw new SecurityException("System.exit calls not allowed!");
						}
					}
				};
				SecurityManager savedManager = System.getSecurityManager();
				System.setSecurityManager(securityManager);
				try {

					CliMain.main(new String[] { "-h", host, "-p",
							port.toString(), "-f", schemaFile.getAbsolutePath() });
				} catch (SecurityException e) {
					// Nothing
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					System.setSecurityManager(savedManager);
				}

			}
		}.run();
	}
}
