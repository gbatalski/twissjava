package example.models;

import java.io.Serializable;


public class Tweet implements Serializable
{

	private static final long serialVersionUID = 1943849744410184266L;
	private final byte[] key;
    private final String uname;
    private final String body;

	private final Long timestamp;

	public Tweet(final byte[] key, final String uname, final String body,
			final Long timestamp) {
        this.key = key;
        this.uname = uname;
        this.body = body;
		this.timestamp = timestamp;
    }

    public byte[] getKey() {
        return key;
    }

    public String getUname() {
        return uname;
    }

    public String getBody() {
        return body;
    }

	public Long getTimestamp() {
		return timestamp;
	}
}
