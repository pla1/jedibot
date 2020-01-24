package social.pla.jedibot;

import java.sql.*;
import java.util.ArrayList;

public class SubscriberDAO {
    private final String SQL_CREATE_TABLE = "create table subscriber (" +
            "id int generated always as identity, " +
            "user_name varchar(1024), " +
            "feed_id int, " +
            "log_time timestamp, " +
            "primary key (id) " +
            ")";

    public static void main(String[] args) {
        SubscriberDAO dao = new SubscriberDAO();
        if (false) {
            dao.createTable();
        }

        System.exit(0);
    }

    private void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            Utils.dropTableIfExists(connection, "subscriber");
            statement.execute(SQL_CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
    }

    public Subscriber get(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Subscriber subscriber = new Subscriber();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscriber where user_name = ? and feed_id = ?");
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
        return subscriber;
    }

    public Subscriber add(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Subscriber subscriber = new Subscriber();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("insert into subscriber " +
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
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return subscriber;
    }

    public boolean delete(Feed feed, String user) {
        Connection connection = null;
        PreparedStatement ps = null;
        Subscriber subscriber = new Subscriber();
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("delete from subscriber where  user_name = ? and feed_id = ?");
            int i = 1;
            ps.setString(i++, user);
            ps.setInt(i++, feed.getId());
            int rowsDeleted = ps.executeUpdate();
            System.out.format("Subscriber rows deleted: %d for user %s feed id: %d\n", rowsDeleted, user, feed.getId());
            return rowsDeleted == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(ps, connection);
        }
        return false;
    }

    private Subscriber transfer(ResultSet rs) throws SQLException {
        Subscriber subscriber = new Subscriber();
        subscriber.setFeedId(rs.getInt("feed_id"));
        subscriber.setFound(true);
        subscriber.setId(rs.getInt("id"));
        subscriber.setUser(rs.getString("user_name"));
        Timestamp timestamp = rs.getTimestamp("log_time");
        subscriber.setLogTimeDisplay(Utils.getFullDateAndTime(timestamp));
        subscriber.setLogTimeMilliseconds(Utils.getLong(timestamp));
        return subscriber;
    }

    public ArrayList<Subscriber> getByFeedId(int feedId) {
        ArrayList<Subscriber> subscribers = new ArrayList<>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscriber where feed_id = ?");
            ps.setInt(1, feedId);
            rs = ps.executeQuery();
            while (rs.next()) {
                subscribers.add(transfer(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, connection);
        }
        return subscribers;
    }



    public Subscriber get(int id) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement("select * from subscriber where id = ?");
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
        return new Subscriber();
    }

}
