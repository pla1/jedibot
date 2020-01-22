package social.pla.jedibot;

import java.sql.Connection;
import java.sql.Statement;

public class FediverseInstanceDAO {
    private final String SQL_CREATE_TABLE = "create table fediverse_instance (" +
            "id int generated always as identity, " +
            "application_id varchar(100), " +
            "client_id varchar(100), " +
            "client_secret varchar(100), " +
            "instance_name varchar(100), " +
            "created timestamp " +
            ")";
    public static void main(String[] args) {
        FediverseInstanceDAO dao = new FediverseInstanceDAO();
        dao.createTable();
        System.exit(0);
    }

    private void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            statement.execute("drop table fediverse_instance");
            statement.execute(SQL_CREATE_TABLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(statement, connection);
        }
    }
}
