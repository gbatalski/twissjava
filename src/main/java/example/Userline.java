package example;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import example.models.Timeline;
import example.models.Tweet;

/**
 * This is the typical twitter page. A form for submitting a 140-character
 *  tweet, and all the tweets that user has made and tweets from everyone
 *  that he is following.
 */
public class Userline extends HomePage {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4893440430661022168L;
	private final String username;
    private Long nextpage;

    public Userline(final PageParameters parameters) {
        super(parameters);
        nextpage = parameters.get("nextpage").toLong(0);
        username = ((TwissSession) WebSession.get()).getUname();
        setup();
        if (username == null) {
            //setRedirect(true);

            setResponsePage(Publicline.class);
        }
        else {
            //setup();
        }
    }

    private void setup() {
        add(new TweetForm("poster"));

        Timeline timeline = getUserline(username, nextpage);
        List<Tweet> tweets;//timeline.getView();

        if (null == timeline) {
            tweets = new ArrayList<Tweet>(0);
        } else {
            tweets = timeline.getView();
        }

        if (tweets.size() > 0) {
            add(new ListView<Tweet>("tweetlist", tweets) {
                /**
				 * 
				 */
				private static final long serialVersionUID = -7413317851324521943L;

				@Override
                public void populateItem(final ListItem<Tweet> listitem) {
					listitem.add(new Link<String>("link") {
                        /**
						 * 
						 */
						private static final long serialVersionUID = 6663254615255592969L;

						@Override
                        public void onClick() {
                            PageParameters p = new PageParameters();
                            p.add("username", listitem.getModel().getObject().getUname());
                            setResponsePage(Publicline.class, p);
                        }
                    }.add(new Label("tuname",listitem.getModel().getObject().getUname())));
                    listitem.add(new Label("tbody", ": " + listitem.getModel().getObject().getBody()));
                }
            }).setVersioned(false);
            Long linktopaginate = timeline.getNextview();
            if (linktopaginate != null) {
                nextpage = linktopaginate;
                WebMarkupContainer pagediv = new WebMarkupContainer("pagedown");
                PageForm pager = new PageForm("pageform");
                pagediv.add(pager);
                add(pagediv);
            }
            else {
                add(new WebMarkupContainer("pagedown") {
                    /**
					 * 
					 */
					private static final long serialVersionUID = 8869323692236862225L;

					@Override
                    public boolean isVisible() {
                        return false;
                    }
                });
            }
        }
        else {
            ArrayList<String> hack = new ArrayList<String>(1);
            hack.add("There are no tweets yet. Post one!");
            add(new ListView<String>("tweetlist", hack ) {
                /**
				 * 
				 */
				private static final long serialVersionUID = -5128393223572874187L;

				@Override
                public void populateItem(final ListItem<String> listitem) {
					listitem.add(new Link<String>("link") {
                        /**
						 * 
						 */
						private static final long serialVersionUID = -3127820289077794200L;

						@Override
                        public void onClick() {
                        }
                    }.add(new Label("tuname","")));
                    listitem.add(new Label("tbody", listitem.getModel().getObject()));
                }
            }).setVersioned(false);
            add(new WebMarkupContainer("pagedown") {
                /**
				 * 
				 */
				private static final long serialVersionUID = 296155464292010568L;

				@Override
                public boolean isVisible() {
                    return false;
                }
            });
        }
    }

	private class PageForm extends Form<PageForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -2660860800827486928L;
		public PageForm(String id) {
            super(id);
        }
        @Override
        public void onSubmit() {

			setResponsePage(getPage().getClass(),
							new PageParameters().add("nextpage", nextpage));
        }
    }

	private class TweetForm extends Form<TweetForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1359967079163951398L;
		private String tweetbody;

        public TweetForm(String id) {
            super(id);
			add(new TextArea<String>(	"tweetbody",
								PropertyModel.<String> of(this, "tweetbody")));
        }
        @Override
        public void onSubmit() {
            saveTweet(new Tweet(UUID.randomUUID().toString().getBytes(), username, tweetbody));
            setResponsePage(getPage().getClass());
        }
    }
}
