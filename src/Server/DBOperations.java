package Server;

import java.sql.*;
import java.util.concurrent.locks.ReentrantLock;

public class DBOperations {

    private Connection connect;
    private Statement statement;
    private ReentrantLock lock;

    public DBOperations() {
        String sql = "CREATE TABLE IF NOT EXISTS CLIENTS " +
                "(c_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " c_name TEXT NOT NULL, " +
                " c_surname TEXT NOT NULL, " +
                " c_sex TEXT NOT NULL, " +
                " c_birthday DATE NOT NULL, " +
                " c_password TEXT NOT NULL, " +
                " c_mail TEXT NOT NULL) ";

        try {
            Class.forName("org.sqlite.JDBC");
            this.connect = DriverManager.getConnection("jdbc:sqlite:Clients.db");
            this.statement = connect.createStatement();
            statement.executeUpdate(sql);
        } catch (ClassNotFoundException ex) {
            System.out.println("DB_clients not found: " + ex.getMessage());
        } catch (SQLException ex) {
            System.out.println("SQL_clients error: " + ex.getMessage());
        }
        this.lock = new ReentrantLock();
    }

    public void disconnect() {
        lock.lock();
        try {
            statement.close();
            connect.close();
        } catch (SQLException ex) {
            System.out.println("disconnect_clients error: " + ex.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void executeUpdate(String sql) {
        lock.lock();
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            System.out.println("execute_update error: " + ex.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private ResultSet executeQuery(String sql) {
        ResultSet tmp = null;
        lock.lock();
        try {
            tmp = statement.executeQuery(sql);
        } catch (SQLException ex) {
            System.out.println("execute_query error: " + ex.getMessage());
        } finally {
            lock.unlock();
        }
        return tmp;
    }

    private boolean checkDuplicates(String email) {
        try {
            ResultSet result = executeQuery( "SELECT c_mail FROM CLIENTS;" );
            while (result.next())
                if (result.getString("c_mail").equals(email)) {
                    result.close();
                    System.out.println("Duplicated email");
                    return true;
                }
            result.close();
        } catch (SQLException ex) {
            System.out.println("check_duplicates error: " + ex.getMessage());
        }
        return false;
    }

    public boolean addClient(String name, String surname, String sex, String date, String password, String email) {
        if (checkDuplicates(email)) {
            return false;
        }

        String sql = "INSERT INTO CLIENTS (c_name,c_surname,c_sex,c_birthday, c_password, c_mail) VALUES " +
                "('" + name + "', '" + surname + "', '" + sex + "', '" + date + "', '" + password + "', '" + email + "' )";

        executeUpdate(sql);
        return true;
    }

    public boolean updateInfo(String email, String name, String surname, String birthday, String sex){
        String sql;

        sql = "UPDATE CLIENTS SET c_name='"+ name+"', c_surname='"+surname+"',c_birthday='"+birthday+"',c_sex='"+sex+"' WHERE c_mail='" +email+"'";
        executeUpdate(sql);
        return true;
    }

    public boolean update_password(String email, String password) {
        if (checkDuplicates(email)) {
            String sql = "UPDATE CLIENTS SET c_password='" + password + "' WHERE c_mail='" + email + "'";
            executeUpdate(sql);
            return true;
        }
        return false;
    }

    public String getInfo(String email){
        String sql;
        StringBuilder stringBuilder;

        sql = "SELECT c_name, c_surname, c_mail, c_sex, c_birthday FROM CLIENTS WHERE c_mail='" + email + "'";
        stringBuilder = new StringBuilder("info ");

        try {
            ResultSet result = executeQuery( sql );
            while (result.next()){
                stringBuilder.append(result.getString("c_name")).append(" ");
                stringBuilder.append(result.getString("c_surname")).append(" ");
                stringBuilder.append(result.getString("c_mail")).append(" ");
                stringBuilder.append(result.getString("c_sex")).append(" ");
                stringBuilder.append(result.getString("c_birthday")).append(" ");
            }
            result.close();
            return stringBuilder.toString();
        } catch (SQLException ex) {
            System.out.println("check_duplicates error: " + ex.getMessage());
            return null;
        }
    }

    public boolean deleteClient(String email){
        String sql="DELETE FROM CLIENTS WHERE c_mail='" + email+"'";

        executeUpdate(sql);
        return !checkDuplicates(email);
    }

    public boolean logIn(String email, String passwd){
        String sql = "SELECT c_password FROM CLIENTS WHERE c_mail =" + "'" + email + "'";
        try {
            ResultSet result = executeQuery( sql);
            if ((!result.next())) {
                return false;
            }
            return result.getString("c_password").equals(passwd);
        } catch (SQLException ex) {
            System.out.println("log_in error: " + ex.getMessage());
        }
        return false;
    }
}
