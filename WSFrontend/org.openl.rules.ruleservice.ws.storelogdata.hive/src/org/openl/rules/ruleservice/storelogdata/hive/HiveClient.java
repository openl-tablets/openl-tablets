package org.openl.rules.ruleservice.storelogdata.hive;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HiveClient {

    private static String driverClass = "org.apache.hive.jdbc.HiveDriver";

    public static void main(String args[]) throws SQLException {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException exception) {

            exception.printStackTrace();
            System.exit(1);
        }
        Connection connection = DriverManager.getConnection("jdbc:hive2://192.168.1.70:10001");

        Statement statement = connection.createStatement();

        String table = "CUSTOMER";
        try {
            statement.execute("DROP TABLE " + table);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        try {
            statement.execute("CREATE TABLE " + table + " (ID INT, NAME STRING, ADDR STRING)");
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        String sql = "SHOW TABLES '" + table + "'";
        System.out.println("Executing Show table: " + sql);
        ResultSet result = statement.executeQuery(sql);
        if (result.next()) {
            System.out.println("Table created is :" + result.getString(1));
        }

        sql = "INSERT INTO CUSTOMER (id,name,addr) VALUES (1, 'Tanya', '1 List' )";
        System.out.println("Inserting table into customer: " + sql);

        try {
            statement.executeUpdate(sql);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        sql = "INSERT INTO CUSTOMER VALUES (3,'Tanya','1 List' )";
        System.out.println("Inserting table into customer: " + sql);

        try {
            statement.executeUpdate(sql);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        sql = "SELECT * FROM " + table;
        System.out.println("Running: " + sql);
        result = statement.executeQuery(sql);
        while (result.next()) {
            System.out.println("Id=" + result.getString(1));
            System.out.println("Name=" + result.getString(2));
            System.out.println("Address=" + result.getString(3));
        }
        result.close();

        statement.close();

        connection.close();

    }
}
