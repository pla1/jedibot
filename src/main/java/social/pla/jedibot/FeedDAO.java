package social.pla.jedibot;

import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class FeedDAO {
    private final String SQL_CREATE_TABLE = "create table feed (" +
            "id int generated always as identity (START WITH 1, INCREMENT BY 1), " +
            "channel_title varchar(1024), " +
            "channel_description varchar(2056), " +
            "channel_url varchar(2056), " +
            "url varchar(2056), " +
            "feed_url varchar(2056), " +
            "label varchar(256), " +
            "title varchar(1024), " +
            "description varchar(5000), " +
            "media_url varchar(2056), " +
            "uploaded_media_url varchar(2056), " +
            "uploaded_media_id varchar(256), " +
            "updated varchar(50), " +
            "log_time timestamp, " +
            "primary key (id), " +
            "constraint label_uc unique (label) " +
            ")";

    public static void main(String[] args) {
        FeedDAO dao = new FeedDAO();
        if (false) {
            Feed feed = dao.get("mkbhd");
            feed = dao.populateWithLatestRssEntry(feed);
            System.out.println(Utils.toString(feed));
            System.exit(0);
        }
        if (false) {
            Feed feed = dao.get("LibreLounge");
            boolean deleted = dao.delete(feed);
            System.out.format("%s deleted %s\n", feed.getLabel(), deleted);
            System.exit(0);
        }
        if (false) {
            String[] labels = {"nasa"};
            for (String label : labels) {
                Feed feed = dao.get(label);
                if (feed.isFound()) {
                    dao.populateWithLatestRssEntry(feed);
                    System.out.println(Utils.toString(feed));
                }
            }
            System.exit(0);
        }
        if (false) {
            dao.createTable();
            System.exit(0);
        }
        if (true) {
            String user = "pla";
            ArrayList<Feed> list = dao.getFromUser(user);
            for (Feed feed : list) {
                System.out.println(Utils.toString(feed));
            }
            System.out.format("%d feeds for %s\n", list.size(), user);
            System.exit(0);
        }
        if (false) {
            ArrayList<Feed> list = dao.get();
            for (Feed feed : list) {
                System.out.println(Utils.toString(feed));
            }
            System.out.format("%d feeds\n", list.size());
            System.exit(0);
        }
        if (false) {

            String[] urlStrings = {"https://www.youtube.com/feeds/videos.xml?channel_id=UCV9WtB_q5sJfe3Rev5PWy-Q",
                    "https://www.nasa.gov/rss/dyn/Houston-We-Have-a-Podcast.rss",
                    "https://xkcd.com/atom.xml",
                    "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss",
                    "http://www.androidbuffet.com/feed/oggcast",
                    "https://librelounge.org/atom-feed.xml"
            };
            for (String urlString : urlStrings) {
                Feed feed = new Feed();
                feed.setUrl(urlString);
                feed = dao.populateWithLatestRssEntry(feed);
                System.out.println(Utils.toString(feed));
            }
            System.exit(0);
        }
        if (false) {
            ArrayList<Feed> list = dao.get();
            for (Feed feed : list) {
                feed = dao.populateWithLatestRssEntry(feed);
                //  dao.uploadMedia(feed);
                System.out.println(Utils.toString(feed));
            }
            System.out.format("%d feeds\n", list.size());
            System.exit(0);
        }
        if (false) {
            System.out.println(Utils.toString(dao.get(Main.Literals.hpr.name())));
            System.exit(0);
        }
        if (false) {
            Feed feed = dao.get(Main.Literals.xkcd.name());
            feed = dao.populateWithLatestRssEntry(feed);
            dao.update(feed);
            System.out.println(Utils.toString(feed));
            System.exit(0);
        }

    }

    private void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            Utils.dropTableIfExists(connection, "subscription");
            Utils.dropTableIfExists(connection, "feed");
            statement.execute(SQL_CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
    }

    public Feed uploadMedia(Feed feed) {
        File file = Utils.downloadMedia(feed.getMediaUrl());
        JsonObject jsonObject = Utils.uploadMedia(file);
        feed.setUploadedMedialId(Utils.getProperty(jsonObject, Main.Literals.id.name()));
        feed.setUploadedMedialUrl(Utils.getProperty(jsonObject, Main.Literals.url.name()));
        return feed;
    }

    public Feed add(String urlString, String label) {
        Feed feed = get(urlString, label);
        if (feed.isFound()) {
            return feed;
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("insert into feed " +
                            "(feed_url, label, log_time) " +
                            "values(?,?, current_timestamp)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, urlString);
            ps.setString(i++, label);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                feed.setId(rs.getInt(1));
                feed.setFound(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, rs, connection);
        }
        return feed;
    }

    public Feed update(Feed feed) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("update feed " +
                    "set url = ?, " +
                    "feed_url = ?, " +
                    "channel_url = ?, " +
                    "channel_title = ?, " +
                    "channel_description = ?, " +
                    "label = ?, " +
                    "description = ?, " +
                    "media_url = ?, " +
                    "uploaded_media_id = ?, " +
                    "uploaded_media_url = ?, " +
                    "title = ?, " +
                    "updated = ? " +
                    "where id = ?");
            int i = 1;
            ps.setString(i++, feed.getUrl());
            ps.setString(i++, feed.getFeedUrl());
            ps.setString(i++, feed.getChannelUrl());
            ps.setString(i++, feed.getChannelTitle());
            ps.setString(i++, feed.getChannelDescription());
            ps.setString(i++, feed.getLabel());
            ps.setString(i++, feed.getDescription());
            ps.setString(i++, feed.getMediaUrl());
            ps.setString(i++, feed.getUploadedMedialId());
            ps.setString(i++, feed.getUploadedMedialUrl());
            ps.setString(i++, feed.getTitle());
            ps.setString(i++, feed.getUpdated());
            ps.setInt(i++, feed.getId());
            int rowsUpdated = ps.executeUpdate();
            System.out.format("%d rows updated for feed: %s\n", rowsUpdated, feed);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, rs, connection);
        }
        return feed;
    }

    public boolean delete(String label) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("delete from feed where label = ?");
            ps.setString(1, label);
            int feedsDeleted = ps.executeUpdate();
            return feedsDeleted == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, connection);
        }
        return false;
    }

    public boolean delete(Feed feed) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("delete from feed where id = ?");
            ps.setInt(1, feed.getId());
            int feedsDeleted = ps.executeUpdate();
            Utils.close(ps);
            /*
            ps = connection.prepareStatement("delete from subscription where feed_id = ?");
            ps.setInt(1, feed.getId());
            int subscriptionsDeleted = ps.executeUpdate();
            System.out.format("%d feed rows deleted and %d subscriptions deleted for feed: %s\n",
                    feedsDeleted, subscriptionsDeleted, feed);

             */
            return feedsDeleted == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, connection);
        }
        return false;
    }

    public Feed populateWithLatestRssEntry(Feed feed) {
        System.out.format("Populate feed %s\n", feed);
        try {
            org.jsoup.Connection.Response response = Jsoup.connect(feed.getFeedUrl()).execute();
            Document document = Jsoup.parse(response.body()).parser(Parser.xmlParser());
/*
             document = Jsoup.connect(feed.getUrl())
                    .parser(Parser.xmlParser())
                    .timeout(1000 * 5)
                    .get();

             */
            Element channelElement = document.getElementsByTag("channel").first();
            if (channelElement != null) {
                feed.setChannelTitle(channelElement.getElementsByTag("title").first().text());
                feed.setChannelDescription(channelElement.getElementsByTag("description").first().text());
                feed.setChannelUrl(channelElement.getElementsByTag("link").first().text());
            }
            if (Utils.isBlank(feed.getChannelUrl())) {
                Element linkElement = document.selectFirst("link");
                if (linkElement != null) {
                    feed.setChannelUrl(linkElement.attr("href"));
                }
            }
            if (Utils.isBlank(feed.getChannelUrl())) {
                feed.setChannelUrl(channelElement.getElementsByTag("link").first().text());
            }
            if (Utils.isBlank(feed.getChannelTitle())) {
                feed.setChannelTitle(document.selectFirst("title").text());
            }

            Elements elements = document.getElementsByTag(Main.Literals.entry.name());
            if (elements.size() == 0) {
                elements = document.getElementsByTag(Main.Literals.item.name());
            }
            if (elements.size() == 0) {
                System.out.format("No entries found for feed URL: %s. Feed not updated with latest RSS entry.\n", feed.getUrl());
                return feed;
            }
            Element e = elements.get(0);
            for (Element child : e.getAllElements()) {
                String nodeName = child.nodeName();
                String html = child.html();
                html = html.replace("<![CDATA[", "");
                html = html.replace("]]>", "");
                String text = Jsoup.parse(html).text();
             //   System.out.format("Node name: %s\ntext: %s\nhtml: %s\n", nodeName, text, html);
                if (Main.Literals.title.name().equals(nodeName)) {
                    feed.setTitle(text);
                }
                if (Main.Literals.description.name().equals(nodeName)
                        || "media:description".equals(nodeName)) {
                    feed.setDescription(text);
                }
                if (Main.Literals.updated.name().equals(nodeName) ||
                        Main.Literals.pubDate.name().equals(nodeName)) {
                    feed.setUpdated(text);
                }
                if (Main.Literals.link.name().equals(nodeName)) {
                    if (child.hasAttr("href")) {
                        String link = child.attr("href");
                        feed.setMediaUrl(link);
                    }
                    if (child.hasAttr("rel")) {
                        if (child.hasAttr("enclosure")) {
                            if (child.hasAttr("type")) {
                                if (child.attr("type").startsWith("audio")) {
                                    feed.setMediaUrl(child.attr("href"));
                                }
                            }
                        }
                    }
                }
                if (Main.Literals.enclosure.name().equals(nodeName)) {
                    String mediaUrl = child.attr("url");
                    feed.setMediaUrl(mediaUrl);
                }
                if (Main.Literals.summary.name().equals(nodeName)) {
                    Document docSummary = Jsoup.parse(text);
                    Element imageElement = docSummary.select("img").first();
                    if (imageElement != null) {
                        String url = imageElement.attr("src");
                        feed.setMediaUrl(url);
                        String description = imageElement.attr("title");
                        feed.setDescription(description);
                    } else {
                        feed.setDescription(Jsoup.parse(text).text());
                    }
                }
            }
            if (feed.getChannelDescription() != null
                    && feed.getChannelDescription().contains("Android Buffet")
                    && "Oggcast".equals(feed.getChannelTitle())) {
                feed.setChannelTitle("Android Buffet Podcast");
            }
            if (feed.getMediaUrl().contains("youtube.com")) {
                feed.setUrl(feed.getMediaUrl());
                feed.setMediaUrl(null);
            }
        } catch (IOException e) {
            System.out.format("%s failed.\n", feed.getUrl());
            e.printStackTrace();
        }
        return feed;
    }

    public ArrayList<Feed> getFromUser(String user) {
        ArrayList<Feed> feeds = new ArrayList<>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select b.* " +
                    "from subscription as a " +
                    "join feed as b " +
                    "on a.feed_id = b.id " +
                    "where user_name = ? " +
                    "order by b.label");
            ps.setString(1, user);
            rs = ps.executeQuery();
            while (rs.next()) {
                feeds.add(transfer(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return feeds;
    }

    public ArrayList<Feed> get() {
        ArrayList<Feed> list = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            rs = statement.executeQuery("select * from feed");
            while (rs.next()) {
                list.add(transfer(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
        return list;
    }

    private Feed transfer(ResultSet rs) throws SQLException {
        Feed feed = new Feed();
        feed.setId(rs.getInt(Main.Literals.id.name()));
        feed.setUrl(rs.getString(Main.Literals.url.name()));
        feed.setFeedUrl(rs.getString("feed_url"));
        feed.setLabel(rs.getString(Main.Literals.label.name()));
        feed.setDescription(rs.getString(Main.Literals.description.name()));
        feed.setUploadedMedialId(rs.getString("uploaded_media_id"));
        feed.setUploadedMedialUrl(rs.getString("uploaded_media_url"));
        feed.setMediaUrl(rs.getString("media_url"));
        feed.setTitle(rs.getString(Main.Literals.title.name()));
        feed.setUpdated(rs.getString(Main.Literals.updated.name()));
        Timestamp timestamp = rs.getTimestamp("log_time");
        feed.setLogTimeDisplay(Utils.getFullDateAndTime(timestamp));
        feed.setLogTimeMilliseconds(Utils.getLong(timestamp));
        feed.setChannelDescription(rs.getString("channel_description"));
        feed.setChannelTitle(rs.getString("channel_title"));
        feed.setChannelUrl(rs.getString("channel_url"));
        feed.setFound(true);
        return feed;
    }

    public Feed get(String urlString, String label) {
        Feed feed = new Feed();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from feed where url = ? and label = ?");
            ps.setString(1, urlString);
            ps.setString(2, label);
            rs = ps.executeQuery();
            while (rs.next()) {
                feed = transfer(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return feed;
    }

    public Feed get(String label) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from feed where label = ?");
            ps.setString(1, label);
            rs = ps.executeQuery();
            while (rs.next()) {
                return transfer(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return new Feed();
    }
}
