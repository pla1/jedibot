package social.pla.jedibot;

import com.google.gson.JsonObject;

import java.sql.*;

public class ApplicationDAO {
    private final String SQL_SELECT = "select * from application where id = ?";
    private final String SQL_INSERT = "insert into application" +
            "(application_id, client_id, client_secret, access_token, refresh_token, instance_name, user_name, create_time) " +
            "values(?,?,?,?,?,?,?,current_timestamp)";
    private final String SQL_CREATE_TABLE = "create table application (" +
            "id int generated always as identity, " +
            "application_id varchar(100), " +
            "client_id varchar(100), " +
            "client_secret varchar(100), " +
            "access_token varchar(100), " +
            "refresh_token varchar(100), " +
            "instance_name varchar(100), " +
            "user_name varchar(100), " +
            "create_time timestamp " +
            ")";

    public static void main(String[] args) {
        ApplicationDAO dao = new ApplicationDAO();
        if (false) {
            dao.createTable();
        }
        Application application = dao.get(1);
        System.out.println(Utils.toString(application));
        System.exit(0);
    }

    private Application transfer(ResultSet rs) throws SQLException {
        Application application = new Application();
        application.setId(rs.getInt(Main.Literals.id.name()));
        application.setApplicationId(rs.getString("application_id"));
        application.setClientId(rs.getString(Main.Literals.client_id.name()));
        application.setClientSecret(rs.getString(Main.Literals.client_secret.name()));
        Timestamp timestamp = rs.getTimestamp("create_time");
        application.setCreatedMilliseconds(Utils.getLong(timestamp));
        application.setCreatedDisplay(Utils.getFullDateAndTime(timestamp));
        application.setInstanceName(rs.getString("instance_name"));
        application.setUser(rs.getString("user_name"));
        application.setAccessToken(rs.getString(Main.Literals.access_token.name()));
        application.setRefreshToken(rs.getString(Main.Literals.refresh_token.name()));
        application.setFound(true);
        return application;
    }

    public Application add(Application application) {
        Connection connection = null;
        PreparedStatement ps = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            statement.execute("truncate table application");
            ps = connection.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, application.getApplicationId());
            ps.setString(i++, application.getClientId());
            ps.setString(i++, application.getClientSecret());
            ps.setString(i++, application.getAccessToken());
            ps.setString(i++, application.getRefreshToken());
            ps.setString(i++, application.getInstanceName());
            ps.setString(i++, application.getUser());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return get(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(rs, ps, statement, connection);
        }
        return new Application();
    }

    public Application get(int id) {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = Utils.getConnection();
            ps = connection.prepareStatement(SQL_SELECT);
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
        return new Application();
    }

    private void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            Utils.dropTableIfExists(connection, "application");
            statement.execute(SQL_CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
    }
}
