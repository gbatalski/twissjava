package example;

import example.models.Timeline;
import example.models.Tweet;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is the typical twitter page. A form for submitting a 140-character
 *  tweet, and all the tweets that user has made and tweets from everyone
 *  that he is following.
 */
public class Userline extends HomePage {
    private String username;
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

        Timeline timeline = getTimeline(username, nextpage);
        List<Tweet> tweets;//timeline.getView();

        if (null == timeline) {
            tweets = new ArrayList<Tweet>(0);
        } else {
            tweets = timeline.getView();
        }

        if (tweets.size() > 0) {
            add(new ListView<Tweet>("tweetlist", tweets) {
                @Override
                public void populateItem(final ListItem<Tweet> listitem) {
                    listitem.add(new Link("link") {
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
                @Override
                public void populateItem(final ListItem<String> listitem) {
                    listitem.add(new Link("link") {
                        @Override
                        public void onClick() {
                        }
                    }.add(new Label("tuname","")));
                    listitem.add(new Label("tbody", listitem.getModel().getObject()));
                }
            }).setVersioned(false);
            add(new WebMarkupContainer("pagedown") {
                @Override
                public boolean isVisible() {
                    return false;
                }
            });
        }
    }

    private class PageForm extends Form {
        public PageForm(String id) {
            super(id);
        }
        @Override
        public void onSubmit() {
            PageParameters p = new PageParameters();
            p.add("nextpage", nextpage);
            setResponsePage(getPage().getClass(), p);
        }
    }

    private class TweetForm extends Form {
        private String tweetbody;

        public TweetForm(String id) {
            super(id);
            add(new TextArea("tweetbody", new PropertyModel(this,"tweetbody")));
        }
        @Override
        public void onSubmit() {
            saveTweet(new Tweet(UUID.randomUUID().toString().getBytes(), username, tweetbody));
            setResponsePage(getPage().getClass());
        }
    }
}
