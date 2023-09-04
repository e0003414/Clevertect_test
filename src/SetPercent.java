import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;
/**
 * Класс SetPercent - в данном классе выполняется начисление процентов по счетам клиентов в последний день месяца
 * @author Pushkarenko
 *
 */
public class SetPercent implements Runnable{
    private  static Connection connection = null;

    public SetPercent(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try{
           // проверка в интервале 30 сек признака установки флага начисления процентов
            while (!Thread.currentThread().isInterrupted()){
                Thread.sleep(30000);
                if(CleverBank.setPercent){
                  CleverBank.setPercent = false;
                  synchronized (connection) {
                      setDepositPercent();
                  }
                }
            }
        }
        catch (InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }

    // Метод начисления процентов по счетам
    private static void setDepositPercent(){
        try(FileReader fis = new FileReader("percent.yml"); Scanner sc = new Scanner(fis)){
            try{
                String sql = null;
                int percent = Integer.parseInt(sc.nextLine());
                connection.setAutoCommit(false);
                Statement st = connection.createStatement();
                sql = "UPDATE deposit SET balance = balance + balance * " + percent + " / 100" + ";";
                st.executeUpdate(sql);
            }
            catch (Exception ex){
            }
        }
        catch (IOException ex){
        }
    }
}
