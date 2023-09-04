import java.sql.*;

/**
 * Класс Main - в данном классе создается соединение с БД и запуск основного класса
 * @author Pushkarenko
 *
 */
public class Main {
    private Connection c = null;
    private Statement stmt = null;
    private static  final String USER = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/testadminDB";
    private static final String PASSWORD = "12345678";

    public static void main(String[] args)  {
        try(Connection c = DriverManager.getConnection(URL,USER, PASSWORD); Statement stmt = c.createStatement()){
            Thread cleverbankthread = new Thread(new CleverBank(c,stmt));
            Thread endmonth = new Thread(new EndMonth());
            Thread setpercent = new Thread(new SetPercent(c));
            cleverbankthread.start();
            endmonth.start();
            setpercent.start();
            cleverbankthread.join();
            endmonth.interrupt();
            setpercent.interrupt();
        }
        catch (SQLException | InterruptedException ex){
            System.out.println("Невозможно подключитьбся к БД...");
        }
    }
}
