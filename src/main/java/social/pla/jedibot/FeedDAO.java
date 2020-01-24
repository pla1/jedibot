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
            "id int generated always as identity, " +
            "url varchar(2056), " +
            "label varchar(256), " +
            "title varchar(1024), " +
            "description varchar(5000), " +
            "media_url varchar(2056), " +
            "uploaded_media_url varchar(2056), " + "uploaded_media_id varchar(256), " +
            "updated varchar(50), " +
            "log_time timestamp, " +
            "primary key (id) " +
            ")";

    public static void main(String[] args) {
        FeedDAO dao = new FeedDAO();
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
            Feed feed = new Feed();
            feed.setUrl("https://www.youtube.com/feeds/videos.xml?channel_id=UCV9WtB_q5sJfe3Rev5PWy-Q");
            feed = dao.populateWithLatestRssEntry(feed);
            System.out.println(Utils.toString(feed));
            System.exit(0);
        }
        if (false) {
            dao.createTable();
            dao.add("http://hackerpublicradio.org/hpr_rss.php", Main.Literals.hpr.name());
            dao.add("https://xkcd.com/atom.xml", Main.Literals.xkcd.name());
            dao.add("https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss", Main.Literals.nasa.name());
            System.exit(0);
        }
        if (true) {
            ArrayList<Feed> list = dao.get();
            for (Feed feed : list) {
                feed = dao.populateWithLatestRssEntry(feed);
                dao.uploadMedia(feed);
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
                            "(url, label, log_time) " +
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
                    "label = ?, " +
                    "description = ?, " +
                    "media_url = ?, " +
                    "uploaded_media_id = ?, " +
                    "uploaded_media_url = ?, " +
                    "title = ? " +
                    "where id = ?");
            int i = 1;
            ps.setString(i++, feed.getUrl());
            ps.setString(i++, feed.getLabel());
            ps.setString(i++, feed.getDescription());
            ps.setString(i++, feed.getMediaUrl());
            ps.setString(i++, feed.getUploadedMedialId());
            ps.setString(i++, feed.getUploadedMedialUrl());
            ps.setString(i++, feed.getTitle());
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


    public Feed populateWithLatestRssEntry(Feed feed) {
        try {
            Document document = Jsoup.connect(feed.getUrl())
                    .parser(Parser.xmlParser())
                    .timeout(1000 * 3)
                    .get();
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
            //    System.out.format("Node name: %s value: %s\n", nodeName, text);
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
                    String link = child.attr("href");
                    feed.setMediaUrl(link);
                }
                if (Main.Literals.enclosure.name().equals(nodeName)) {
                    String mediaUrl = child.attr("url");
                    feed.setMediaUrl(mediaUrl);
                }
                if (Main.Literals.summary.name().equals(nodeName)) {
                    Document docSummary = Jsoup.parse(text);
                    Element imageElement = docSummary.select("img").first();
                    String url = imageElement.attr("src");
                    feed.setMediaUrl(url);
                    String description = imageElement.attr("title");
                    feed.setDescription(description);
                }
            }
            if (feed.getMediaUrl().contains("youtube.com")) {
                feed.setUrl(feed.getMediaUrl());
                feed.setMediaUrl(null);
            }
        } catch (IOException e) {
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
                    "from subscriber as a " +
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
        feed.setLabel(rs.getString(Main.Literals.label.name()));
        feed.setDescription(rs.getString(Main.Literals.description.name()));
        feed.setUploadedMedialId(rs.getString("uploaded_media_id"));
        feed.setUploadedMedialUrl(rs.getString("uploaded_media_url"));
        feed.setMediaUrl(rs.getString("media_url"));
        feed.setTitle(rs.getString(Main.Literals.title.name()));
        feed.setUpdated(Main.Literals.updated.name());
        Timestamp timestamp = rs.getTimestamp("log_time");
        feed.setLogTimeDisplay(Utils.getFullDateAndTime(timestamp));
        feed.setLogTimeMilliseconds(Utils.getLong(timestamp));
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
