package de.xnonymous.api.mysql;

import com.zaxxer.hikari.HikariDataSource;
import de.xnonymous.api.mysql.utils.Data;
import de.xnonymous.api.mysql.utils.Delete;
import de.xnonymous.api.mysql.utils.Row;
import lombok.Getter;
import lombok.Setter;

import java.sql.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * This is a MySQL API!
 * You can find updates on
 * github!
 *
 * https://github.com/XNonymous-N2/MySQLAPI/
 *
 *
 * @author XNonymous
 * @see com.zaxxer.hikari.HikariDataSource
 **/

@Getter
@Setter
public class MySQL {

    private String host;
    private String password;
    private String user;
    private String db;
    private int port = 3306;

    private HikariDataSource dataSource;

    public void connect() {
        this.dataSource = new HikariDataSource();
        this.dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
        this.dataSource.setUsername(this.user);
        this.dataSource.setPassword(this.password);
    }

    public void disconnect() {
        this.dataSource.close();
    }

    public void insertData(String where, String values, Object... data) {

        String query = "INSERT INTO `" + where + "` (" + values + ") VALUES (" + Arrays.stream(data).map(s -> "?").collect(Collectors.joining(", ")) + ")";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (int i = 0; i < data.length; i++) {
                preparedStatement.setObject((i + 1), data[i]);
            }

            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void getData(String from, String filter, String where, Consumer<Data> dataConsumer) {
        if (filter == null || filter.equals(""))
            filter = "*";

        String query = "SELECT " + filter + " FROM `" + from + "`";

        if (where != null && !where.equals(""))
            query += " WHERE " + where;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            Data data = new Data();

            int stop = -1;
            while (resultSet.next()) {
                Row row = new Row();

                int i = 1;
                while (true) {
                    try {

                        if (stop != -1 && stop == i)
                            break;

                        row.addValue(metaData.getColumnName(i), resultSet.getObject(i));
                    } catch (SQLException ignored) {
                        stop = i;
                        break;
                    }
                    i++;
                }

                data.addRow(row);
            }

            statement.close();
            resultSet.close();
            dataConsumer.accept(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(String table, String value, String column) {
        delete(table, Delete.of(value, column));
    }

    public void delete(String table, Delete... deletes) {
        StringBuilder query = new StringBuilder("DELETE FROM `" + table + "` WHERE ");

        for (int i = 0; i < deletes.length; i++) {
            query.append("`").append(deletes[i].getValue()).append("`=?");
            if (deletes.length - 1 != i)
                query.append(" AND ");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(query.toString())) {

            for (int i = 0; i < deletes.length; i++) {
                ps.setObject((i + 1), deletes[i].getColumn());
            }

            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void table(String sql) {
        String a = "CREATE TABLE IF NOT EXISTS " + sql;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(a)) {
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
