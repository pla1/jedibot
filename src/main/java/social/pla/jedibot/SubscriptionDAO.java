package social.pla.jedibot;

import java.sql.*;
import java.util.ArrayList;

public class SubscriptionDAO {
    private final String SQL_CREATE_TABLE = "create table subscription (" +
            "id int generated always as identity (START WITH 1, INCREMENT BY 1), " +
            "user_name varchar(1024), " +
            "feed_id int, " +
            "log_time timestamp, " +
            "primary key (feed_id, user_name), " +
            "constraint feed_fk " +
            "foreign key (feed_id) " +
            "references feed (id) " +
            "ON DELETE CASCADE ON UPDATE RESTRICT " +
            ")";

    public static void main(String[] args) {
        SubscriptionDAO dao = new SubscriptionDAO();
        if (false) {
            dao.createTable();
        }
        if (false) {
            Feed feed = new Feed();
            feed.setId(1);
            dao.add(feed, "pla");
            ArrayList<Subscription> list = dao.getByFeedId(1);
            System.out.format("%d subscriptions\n", list.size());
            FeedDAO feedDAO = new FeedDAO();
            feed = feedDAO.get("hpr");
            System.out.println(Utils.toString(feed));
            feedDAO.delete(feed);
            list = dao.getByFeedId(1);
            System.out.format("%d subscriptions\n", list.size());
        }
        System.exit(0);
    }

    private void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            Utils.dropTableIfExists(connection, "subscription");
            statement.execute(SQL_CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
    }

    public Subscription get(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Subscription subscription = new Subscription();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscription where user_name = ? and feed_id = ?");
            int i = 1;
            ps.setString(i++, user);
            ps.setInt(i++, feed.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                return transfer(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return subscription;
    }

    public String getDisplay(String accountName) {
        FeedDAO feedDAO = new FeedDAO();
        ArrayList<Feed> feeds = feedDAO.getFromUser(accountName);
        if (feeds.isEmpty()) {
            return String.format("%s isn't subscribed to any feeds. Try something like: subscribe xkcd", accountName);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("You are subscribed to: ");
            String comma = "";
            for (Feed f : feeds) {
                sb.append(comma);
                sb.append(f.getLabel());
                comma = ", ";
            }
            sb.append(".");
            return sb.toString();
        }
    }

    public Subscription add(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Subscription subscription = new Subscription();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("insert into subscription " +
                    "(user_name, feed_id, log_time) " +
                    "values(?,?,current_timestamp) ", PreparedStatement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, user);
            ps.setInt(i++, feed.getId());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return get(rs.getInt(1));
            }

        } catch (Exception e) {
            System.out.format("%s\n", e.getLocalizedMessage());
        } finally {
            Utils.close(rs, ps, connection);
        }
        return subscription;
    }

    public boolean delete(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        Subscription subscription = new Subscription();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("delete from subscription where  user_name = ? and feed_id = ?");
            int i = 1;
            ps.setString(i++, user);
            ps.setInt(i++, feed.getId());
            int rowsDeleted = ps.executeUpdate();
            System.out.format("Subscription rows deleted: %d for user %s feed id: %d\n", rowsDeleted, user, feed.getId());
            return rowsDeleted == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, connection);
        }
        return false;
    }

    private Subscription transfer(ResultSet rs) throws SQLException {
        Subscription subscription = new Subscription();
        subscription.setFeedId(rs.getInt("feed_id"));
        subscription.setFound(true);
        subscription.setId(rs.getInt("id"));
        subscription.setUser(rs.getString("user_name"));
        Timestamp timestamp = rs.getTimestamp("log_time");
        subscription.setLogTimeDisplay(Utils.getFullDateAndTime(timestamp));
        subscription.setLogTimeMilliseconds(Utils.getLong(timestamp));
        return subscription;
    }

    public ArrayList<Subscription> getByFeedId(int feedId) {
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscription where feed_id = ?");
            ps.setInt(1, feedId);
            rs = ps.executeQuery();
            while (rs.next()) {
                subscriptions.add(transfer(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return subscriptions;
    }


    public Subscription get(int id) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscription where id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return transfer(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return new Subscription();
    }

}
