import java.time.LocalDate;
import java.time.YearMonth;
/**
 * Класс EndMonth - в данном классе выполняется проверка последний день месяца текущей даты
 * @author Pushkarenko
 *
 */
public class EndMonth implements Runnable{
    @Override
    public void run() {
      YearMonth yearMonth = YearMonth.now();
      String endmonth = yearMonth.atEndOfMonth().toString();
      try{
            while (!Thread.currentThread().isInterrupted()){
                Thread.sleep(30000);
                LocalDate currentdate = LocalDate.now();
                String  currentmonth = currentdate.toString();
                //проверка даты текущего дня на последний день месяца
                //если текущий день последний, то выставляем флаг начисления процентов и меняем параметр последнего дня на следующий месяц
                if(endmonth.equals(currentmonth)){
                    CleverBank.setPercent = true;
                    yearMonth = yearMonth.plusMonths(1);
                    endmonth = yearMonth.atEndOfMonth().toString();
                }
            }
        }
        catch (InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }

}
