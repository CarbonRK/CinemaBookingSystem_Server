package Server;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.concurrent.locks.ReentrantLock;

public class DBFilms {

    private Connection connect;
    private Statement statement;
    private ReentrantLock lock;

    public DBFilms() {
        String[] sql = new String[3];
        sql[0] = "CREATE TABLE IF NOT EXISTS FILMS " +
                "(f_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " f_name TEXT NOT NULL, " +
                " f_description TEXT NOT NULL, " +
                " f_image BLOB NOT NULL) ";
        sql[1] = "CREATE TABLE IF NOT EXISTS DATES " +
                "(d_day TEXT NOT NULL, " +
                " d_time TEXT NOT NULL, " +
                " d_f_id DATE NOT NULL, " +
                " d_s_id DATE NOT NULL) ";
        sql[2] = "CREATE TABLE IF NOT EXISTS TICKETS " +
                "(t_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                " t_place INTEGER NOT NULL," +
                " t_owner TEXT NOT NULL, " +
                " d_day TEXT NOT NULL, " +
                " d_time TEXT NOT NULL, " +
                " f_name TEXT NOT NULL, " +
                " c_mail TEXT NOT NULL) ";

        try {
            Class.forName("org.sqlite.JDBC");
            this.connect = DriverManager.getConnection("jdbc:sqlite:Films.db");
            this.statement = connect.createStatement();
            for (String command: sql) {
                statement.executeUpdate(command);
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("DB_films not found: " + ex.getMessage());
        } catch (SQLException ex) {
            System.out.println("SQL_films error: " + ex.getMessage());
        }
        this.lock = new ReentrantLock();
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
        java.sql.ResultSet tmp = null;
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

    public String getFilms(){
        String[] commands;
        String sql;

        sql = "SELECT f_name, f_description FROM FILMS;";
        commands = new String[]{"films ",sql,"f_name","f_description"};

        return getCommonData(commands);
    }

    public String getPlaces(String day, String time, String f_name){
        String[] commands;
        String sql;

        sql = "SELECT t_place FROM TICKETS WHERE d_day='"+day+"' AND d_time='"+time+"' AND f_name='"+f_name+"'";
        commands = new String[]{"places ",sql,"t_place"};

        return getCommonData(commands);
    }

    public void disconnect() {
        try {
            statement.close();
            connect.close();
        } catch (SQLException ex) {
            System.out.println("disconnect_films error: " + ex.getMessage());
        }
    }

    public String getFilmsDates(String filmName){
        String[] commands;
        String sql;

        sql = "SELECT d_day, d_time FROM DATES INNER JOIN FILMS ON FILMS.f_id = DATES.d_f_id WHERE FILMS.f_name='"+filmName+"'";
        commands = new String[]{"dates ",sql,"d_day","d_time"};

        return getCommonData(commands);
    }

    public void deleteTicket(String id){
        String sql;

        sql = "DELETE FROM TICKETS WHERE t_id='"+id+"'";
        executeUpdate(sql);
    }

    public String getTickets(String email){
        String[] commands;
        String sql;

        sql = "SELECT t_id,t_place,t_owner,d_day,d_time,f_name FROM TICKETS WHERE c_mail='"+email+"'";
        commands = new String[]{"tickets ",sql,"t_id","f_name","t_place","t_owner","d_day","d_time"};

        return getCommonData(commands);
    }

    public String getLastTicketIndex(){
        String sql;
        String[] commands;

        sql = "SELECT t_id FROM TICKETS WHERE t_id = (SELECT MAX(t_id)  FROM TICKETS);";
        commands = new String[]{"lsatID ", sql, "t_id"};

        return getCommonData(commands);
    }

    public void makeOrder(String email, String place, String owner, String day, String time, String filmName){
        String sql = "INSERT INTO TICKETS (t_place, t_owner, d_day, d_time, f_name, c_mail) VALUES " +
                "('"+place+"','"+owner+"','"+day+"','"+time+"','"+filmName+"','"+email+"')";

        executeUpdate(sql);
    }

    private String getCommonData(String[] data){
        String sql, respond;
        StringBuilder allData;
        ResultSet dataInfo;

        respond = data[0];
        sql = data[1];
        allData = new StringBuilder(respond);
        dataInfo = executeQuery(sql);
        try {
            while (dataInfo.next()){
                for (int i=2 ; i< data.length; i++){
                    allData.append(dataInfo.getString(data[i])).append(" ");
                }
            }
            dataInfo.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allData.toString();
    }

    public String getFilmsImage(int id)  {
        int charVal;
        String selectSQL;
        StringBuilder stringBuilder;
        ResultSet rs;

        selectSQL = "SELECT f_image FROM FILMS WHERE f_id='" + id + "'";
        stringBuilder = new StringBuilder("image ");
        rs = executeQuery(selectSQL);
        try {
            if (rs.next()) {
                InputStream input = rs.getBinaryStream("f_image");
                while ((charVal = input.read()) > -1) {
                    stringBuilder.append(charVal).append(',');
                }
            }
            rs.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

}
