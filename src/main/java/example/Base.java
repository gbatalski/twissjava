package example;

import static com.google.common.collect.ImmutableList.of;
import static example.services.db.cassandra.CassandraService.SE;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;

import example.models.Timeline;
import example.models.Tweet;
import example.models.User;
import example.services.db.cassandra.CassandraService;

/**
 * Base contains both the default header/footer things for the UI as
 *   well as all the shared code for all the child controllers.
 */
public abstract class Base extends WebPage {

	private static final long serialVersionUID = -8667833998001599390L;

	final static Logger log = LoggerFactory.getLogger(Base.class);

    //CLs
    final ConsistencyLevel WCL = ConsistencyLevel.ONE;
    final ConsistencyLevel RCL = ConsistencyLevel.ONE;

    //Column Family names
    public final static String USERS = "User";
    public final static String FRIENDS = "Friends";
    public final static String FOLLOWERS = "Followers";
    public final static String MISC_COUNTS = "MiscCounts";
    public final static String TWEETS = "Tweet";
    public final static String TIMELINE = "Timeline";
    public final static String USERLINE = "Userline";

	@Inject
	public transient CassandraService cassandra;

    //UI settings
    public Base(final PageParameters parameters) {

        String condauth = "Log";
        String username = ((TwissSession)WebSession.get()).getUname();
        if (username == null) {
            condauth += "in";
        }
        else {
            condauth += "out: " + username;
        }
        add(new Label("loginout", condauth));
    }

    @Override
	public void renderHead(final IHeaderResponse response) {
		new Function<List<String>, Void>() {
			@Override
			public Void apply(List<String> input) {
				for (String css : input)
					response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(	Base.class,
																									css
																											+ ".css")));
				return null;
			}
		}.apply(of("960", "reset", "screen", "text"));
    }

    //
    // SHARED CODE
    //

    //Space-savers
    private String bToS(byte[] bytes) {
        return new String(bytes, Charset.forName("UTF-8"));
    }

    //Helpers
    private List<String> getFriendOrFollowerUnames(String COL_FAM, String uname, int count) {

        Map<String, String> map = cassandra.listColumns(uname, COL_FAM, null, count);
        return Arrays.asList(map.keySet().toArray(new String[] {}));
    }

    private Timeline getLine(String COL_FAM, String uname, String startkey, int count) {

        Map<String, String> map = cassandra.listColumns(uname, COL_FAM, startkey, count);

        if (null == map || 0 == map.size()) {
            return null;
        }

        List<String> tweetids = Arrays.asList(map.values().toArray(new String[]{}));
        List<Tweet> tweets = getTweetsForTweetids(tweetids);

        return new Timeline(tweets, Long.valueOf(String.valueOf(map.keySet().toArray()[0])));

        /*Selector selector = makeSel();
        List<Column> timeline;
        byte[] longTypeStartKey = (startkey.equals("") ? new byte[0] : NumberHelper.toBytes(Long.parseLong(startkey)));
        try {
            timeline = selector.getColumnsFromRow(uname, COL_FAM, Selector.newColumnsPredicate(longTypeStartKey,new byte[0],true,count+1), RCL);
        }
        catch (Exception e) {
            log.error("Unable to retrieve timeline for uname: " + uname);
            return null;
        }
        Long mintimestamp = null;
        if (timeline.size() > count) {
            //find min timestamp
            mintimestamp = Long.MAX_VALUE;
            Column removeme = timeline.get(0); //This cannot fail. Count is 0+, and size is thus 1+. Only needed for initialization.
            for (Column c : timeline) {
                long ctime = ByteBuffer.wrap(c.name).getLong();
                if (ctime < mintimestamp) {
                    mintimestamp = ctime;
                    removeme = c;
                }
            }
            //eject column from list after saving the timestamp
            timeline.remove(removeme);
        }
        ArrayList<String> tweetids = new ArrayList<String>(timeline.size());
        for (Column c : timeline) {
            tweetids.add(bToS(c.value));
        }
        Map<String, List<Column>> unordered_tweets = Collections.emptyMap();
        try {
            unordered_tweets = selector.getColumnsFromRows(tweetids, TWEETS, SPall(), RCL);
        }
        catch (Exception e) {
            log.error("Unable to retrieve tweets from timeline for uname: " + uname);
            return null;
        }
        //Order the tweets by the ordered tweetids
        ArrayList<Tweet> ordered_tweets = new ArrayList<Tweet>(tweetids.size());
        for (String tweetid : tweetids) {
            ordered_tweets.add(makeTweet(tweetid.getBytes(),unordered_tweets.get(tweetid)));
        }
        return new Timeline(ordered_tweets, mintimestamp);*/

        //return null;
    }


    //Data Reading
	public User getUserByUsername(String uname) {
		Args.notNull(uname, "Name");
        String password = cassandra.readColumn(uname, "password", USERS);

        if (null == password || password.equals("")) {
            return null;
        }

        return new User(uname.getBytes(), password);
    }

    public List<String> getFriendUnames(String uname) {
        return getFriendUnames(uname, 5000);
    }
    public List<String> getFriendUnames(String uname, int count) {
        return getFriendOrFollowerUnames(FRIENDS, uname, count);
    }

    public List<String> getFollowerUnames(String uname) {
        return getFollowerUnames(uname, 5000);
    }
    public List<String> getFollowerUnames(String uname, int count) {
        return getFriendOrFollowerUnames(FOLLOWERS, uname, count);
    }

    public List<User> getUsersForUnames(List<String> unames) {
        /*Selector selector = makeSel();
        ArrayList<User> users = new ArrayList<User>();
        Map<String, List<Column>> data;
        try {
            data = selector.getColumnsFromRows(unames, USERS, SPall(), RCL);
        }
        catch (Exception e) {
            log.error("Cannot get users for unames: " + unames);
            return users;
        }
        for (Map.Entry<String,List<Column>> row : data.entrySet()) {
            users.add(new User(row.getKey().getBytes(), bToS(row.getValue().get(0).value)));
        }
        return users;*/
		// TODO:
        return null;
    }

    public List<User> getFriends(String uname) {
        return getFriends(uname, 5000);
    }
    public List<User> getFriends(String uname, int count) {
        List<String> friendUnames = getFriendUnames(uname, count);
        return getUsersForUnames(friendUnames);
    }

    public List<User> getFollowers(String uname) {
        return getFollowers(uname, 5000);
    }
    public List<User> getFollowers(String uname, int count) {
        List<String> followerUnames = getFollowerUnames(uname, count);
        return getUsersForUnames(followerUnames);
    }

	public long getFriendsCount(String uname) {
		return getCounter(uname, "friends");
	}

	public long getFollowersCount(String uname) {
		return getCounter(uname, "followers");
	}

	public long getCounter(String uname, String counter) {
		return cassandra.countColumns(uname, counter, MISC_COUNTS);
        
    }


    public Timeline getTimeline(String uname) {
        return getTimeline(uname, "", 40);
    }
    public Timeline getTimeline(String uname, Long startkey) {
        String longAsStr = (startkey == null) ? "" : String.valueOf(startkey);
        return getTimeline(uname, longAsStr, 40);
    }
    public Timeline getTimeline(String uname, String startkey, int limit) {
        return getLine(TIMELINE, uname, startkey, limit);
    }

    public Timeline getUserline(String uname) {
        return getUserline(uname, "", 40);
    }
    public Timeline getUserline(String uname, Long startkey) {
        String longAsStr = (startkey == null) ? "" : String.valueOf(startkey);
        return getUserline(uname, longAsStr, 40);
    }
    public Timeline getUserline(String uname, String startkey, int limit) {
        return getLine(USERLINE, uname, startkey, limit);
    }

    public Tweet getTweet(String tweetid) {

        Map<String, String> map = cassandra.listColumns(tweetid, TWEETS);

		return new Tweet(	tweetid.getBytes(),
							map.get("uname"),
							map.get("body"));
    }

    public List<Tweet> getTweetsForTweetids(List<String> tweetids) {
		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

        for (String tweetid : tweetids) {
            tweets.add(getTweet(tweetid));
        }

        return tweets;
    }


    //Data Writing
    public void saveUser(User user) {
        cassandra.updateColumn(bToS(user.getKey()), user.getPassword(), "password", USERS);
    }
    public void saveTweet(Tweet tweet) {


        long timestamp = System.currentTimeMillis();
        String key = bToS(tweet.getKey());
        cassandra.updateColumn(key, tweet.getUname(), "uname", TWEETS);
        cassandra.updateColumn(key, tweet.getBody(), "body", TWEETS);
        cassandra.updateColumn(tweet.getUname(), key, String.valueOf(timestamp), USERLINE);
        cassandra.updateColumn("!PUBLIC!", key, String.valueOf(timestamp), USERLINE);

        ArrayList<String> followerUnames = new ArrayList<String>(getFollowerUnames(tweet.getUname()));
        for (String follower : followerUnames) {
            cassandra.updateColumn(follower, key, String.valueOf(timestamp), TIMELINE);
        }
    }

    public void addFriends(String from_uname, List<String> to_unames) {
		long timestamp = System.currentTimeMillis();
			Mutator<String> mutator = cassandra.getMutator();
			for (String uname : to_unames) {
				mutator.addInsertion(from_uname, FRIENDS,HFactory.createStringColumn(uname, String.valueOf(timestamp)) )
					.addInsertion(	uname,
									FOLLOWERS,
									HFactory.createStringColumn(from_uname,
																String.valueOf(timestamp)))
					.addCounter(from_uname,
								MISC_COUNTS,
								HFactory.createCounterColumn("friends", 1))
					.addCounter(uname,
								MISC_COUNTS,
								HFactory.createCounterColumn("followers", 1));
			}
		mutator.execute();

    }

    public void removeFriends(String from_uname, List<String> to_unames) {

		Mutator<String> mutator = cassandra.getMutator();
		for (String uname : to_unames) {
			mutator.addDeletion(from_uname, FRIENDS, uname, SE)
					.addDeletion(uname, FOLLOWERS, from_uname, SE);
			mutator.decrementCounter(from_uname, MISC_COUNTS, "friends", 1);
			mutator.decrementCounter(uname, MISC_COUNTS, "followers", 1);
		}
		mutator.execute();
    }

}