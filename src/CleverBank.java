import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Класс CleverBank - основной класс проекта, в нем выполняются операции со счетами клиентов Clever-Bank
 * @author Pushkarenko
 *
 */
public class CleverBank implements Runnable{
    private Connection c = null;
    private Statement stmt = null;
    private static volatile long countCheck = 0;
    public static volatile boolean setPercent = false;
    public CleverBank(Connection c, Statement stmt){
        this.c = c;
        this.stmt = stmt;
    }

    @Override
    public void run() {
            int choice = -1;
            try {
                c.setAutoCommit(false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            System.out.println("\n\t\tДобро пожаловать в Clever-Bank!\n");
            BufferedReader reader = new BufferedReader(new InputStreamReader((System.in)));
            while (true) {
                if (choice == 5) {
                    return;
                }
                System.out.println("Выберите операцию:" +
                        "\n\t1 - Пополнить счет" +
                        "\n\t2 - Снятие со счета" +
                        "\n\t3 - Перевод другому клиенту" +
                        "\n\t4 - Выписка по счету" +
                        "\n\t5 - Выйти");
                try {
                    choice = Integer.parseInt(reader.readLine());
                    switch (choice) {
                        case 1:
                            topUpDeposit(stmt, reader, c);
                            break;
                        case 2:
                            withdrawDeposit(stmt, reader, c);
                            break;
                        case 3:
                            transferToAnotherClient(stmt, reader, c);
                            break;
                        case 4:
                            accountInfo(stmt, reader, c);
                            break;
                        case 5:
                            System.out.println("\nСпасибо за сотрудничество с Clever-Bank!\n\t Да прибудет с Вами сила...");
                            reader.close();
                            c.close();
                            stmt.close();
                            break;
                        default:
                            System.out.println("Выбран недопустимый пункт...");
                            break;
                    }
                } catch (Exception ex) {
                    System.out.println("Произошла ошибка...");
                    return;
                }
            }
    }

    // Метод пополнения счета
    public static void topUpDeposit(Statement stmt, BufferedReader reader, Connection c) {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try{
            // Ввод номера счета и суммы пополнения
            String sql;
            System.out.println("\t\t**********Пополнение счета**********");
            System.out.println("Введите номер счета:");
            int id  = Integer.parseInt(reader.readLine());
            System.out.println("Введите сумму пополнения:");
            float sum = Float.parseFloat(reader.readLine());
            sum = Math.abs(sum);
            sql =  "SELECT * FROM deposit WHERE id_deposit = " + id + ";";
            ResultSet rs = stmt.executeQuery(sql);
            // Проверка наличия счета в базе и выполнение операции пополнения
            if(rs.next()) {
                int bank_id = rs.getInt("bank_id");
                String currency = rs.getString("currency");
                boolean enable = rs.getBoolean("enable");
                if(!enable){
                    //устанавливаем признак захвата строки БД
                    sql = "UPDATE deposit SET enable = true  WHERE id_deposit = " + id + ";";
                    stmt.executeUpdate(sql);
                    c.commit();
                    //пополняем счет
                    sql = "UPDATE deposit SET balance = balance + " + sum + " WHERE id_deposit = " + id + ";";
                    stmt.executeUpdate(sql);
                    c.commit();
                    System.out.println("\tОперация пополнения счета №" + id + " выполнена успешно!\n");
                    //сбрасываем признак захвата строки
                    sql = "UPDATE deposit SET enable = false  WHERE id_deposit = " + id + ";";
                    stmt.executeUpdate(sql);
                    c.commit();
                    rs.close();
                // Заполнение информации об транзакции
                    String date_transaction = dateFormat.format(new Date());
                   sql = "INSERT INTO transaction(id_input, id_output, bank_id_input, bank_id_output, type_operation, sum, date_transaction) VALUES("
                        + id + "," + id + "," + bank_id + "," + bank_id + "," + 1 + "," + sum + "," + '\'' + date_transaction + '\'' + ");";
                    stmt.executeUpdate(sql);
                    c.commit();
                 // формирование чека
                    createCheck(bank_id, bank_id, id, id, sum, "Пополнение",currency, c);
                }
                else{
                    System.out.println("В данным момент счет №" + id + "пополняется другим клиентом. Повторите операцию пополнения счета...");
                    rs.close();
                }
            }
            else {
                System.out.println("Счет не найден...\n\n");
                rs.close();
            }

        }
        catch (Exception ex){
            System.out.println("Введены неккоректные данные суммы или счета...");
        }
    }

    //Метод снятия средств со счета
    public static void withdrawDeposit(Statement stmt, BufferedReader reader, Connection c){

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try{
            // Ввод номера счета и суммы снятия
            String sql;
            System.out.println("\t\t**********Снятие средств со счета**********");
            System.out.println("Введите номер счета:");
            int id  = Integer.parseInt(reader.readLine());
            System.out.println("Введите сумму снятия:");
            float sum = Float.parseFloat(reader.readLine());
            sum = Math.abs(sum);
            sql =  "SELECT * FROM deposit WHERE id_deposit = " + id + ";";
            ResultSet rs = stmt.executeQuery(sql);
            // Проверка наличия счета в базе и выполнение операции снятия
            if(rs.next()) {
                float deposit_sum = rs.getFloat("balance");
                int bank_id = rs.getInt("bank_id");
                String currency = rs.getString("currency");
                boolean enable = rs.getBoolean("enable");
                if (!enable) {
                    //устанавливаем признак захвата строки БД
                    sql = "UPDATE deposit SET enable = true  WHERE id_deposit = " + id + ";";
                    stmt.executeUpdate(sql);
                    c.commit();
                    // проверка наличия необходимой суммы на счете и выполнение списывания со счета
                    if (sum <= deposit_sum) {
                        sql = "UPDATE deposit SET balance = balance - " + sum + " WHERE id_deposit = " + id + ";";
                        stmt.executeUpdate(sql);
                        c.commit();
                        System.out.println("\tОперация снятия средств счета №" + id + " выполнена успешно!\n");
                        //сбрасываем признак захвата строки БД
                        sql = "UPDATE deposit SET enable = false  WHERE id_deposit = " + id + ";";
                        stmt.executeUpdate(sql);
                        c.commit();
                        rs.close();
                        // Заполнение информации об транзакции
                        String date_transaction = dateFormat.format(new Date());
                        sql = "INSERT INTO transaction(id_input, id_output, bank_id_input, bank_id_output, type_operation, sum, date_transaction) VALUES("
                                + id + "," + id + "," + bank_id + "," + bank_id + "," + 2 + "," + sum + "," + '\'' + date_transaction + '\'' + ");";
                        stmt.executeUpdate(sql);
                        c.commit();
                        // Формирование чека
                        createCheck(bank_id, bank_id, id, id, sum, "Снятие",currency, c);
                    } else {
                        System.out.println("На счету недостаточно средств...\n\n");
                        rs.close();
                    }
                }
                else {
                    System.out.println("В данным момент счет №" + id + "пополняется другим клиентом. Повторите операцию снятия средств со счета...");
                }
            }
            else {
                System.out.println("Счет не найден...\n");
                rs.close();
            }
        }
        catch (Exception ex){
            System.out.println("Введены неккоректные данные суммы или счета...");
        }
    }

    //Метод перевода средств другому клиенту
    public static void transferToAnotherClient(Statement stmt, BufferedReader reader, Connection c){
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try{
            // Ввод номеров счетов и суммы перевода
            String sql;
            System.out.println("\t\t**********Перевод другому клиенту**********");
            System.out.println("Введите номер счета отправителя:");
            int id_otpr  = Integer.parseInt(reader.readLine());
            System.out.println("Введите номер счета получателя:");
            int id_pol  = Integer.parseInt(reader.readLine());
            System.out.println("Введите сумму пополнения:");
            float sum = Float.parseFloat(reader.readLine());
            sum = Math.abs(sum);
             //Формирование выборки по введенным счетам
            sql =  "SELECT * FROM deposit WHERE id_deposit = " + id_otpr + " OR  " + "id_deposit = " + id_pol + ";";
            ResultSet rs= stmt.executeQuery(sql);
            int bank_id_otpr = -1, bank_id_pol = -1;
            String currency = null;
            boolean enable_pol = true;
            while(rs.next()) {
                if (rs.getInt("id_deposit") == id_otpr) {
                    bank_id_otpr = rs.getInt("bank_id");
                }
                if(rs.getInt("id_deposit") == id_pol) {
                    bank_id_pol = rs.getInt("bank_id");
                    currency = rs.getString("currency");
                    enable_pol = rs.getBoolean("enable");
                }
            }
            // Проверка наличия счетов в базе и выполнение операции пополнения
            if(bank_id_otpr != -1 && bank_id_pol != -1) {
                if(!enable_pol){
                    //устанавливаем признак захвата счета получателя
                    sql = "UPDATE deposit SET enable = true  WHERE id_deposit = " + id_pol + ";";
                    stmt.executeUpdate(sql);
                    c.commit();
                    //пополняем счет получателя
                     sql = "UPDATE deposit SET balance = balance + " + sum + " WHERE id_deposit = " + id_pol + ";";
                     stmt.executeUpdate(sql);
                     c.commit();
                     System.out.println("\tОперация пополнения счета №" + id_pol + " выполнена успешно!\n");
                     //сбрасываем признак захвата строки счета получателя
                     sql = "UPDATE deposit SET enable = false  WHERE id_deposit = " + id_pol +";";
                     stmt.executeUpdate(sql);
                     c.commit();
                     rs.close();
                     // Заполнение информации об транзакции
                     String date_transaction = dateFormat.format(new Date());
                     sql = "INSERT INTO transaction(id_input, id_output, bank_id_input, bank_id_output, type_operation, sum, date_transaction) VALUES("
                                + id_otpr + "," + id_pol + "," + bank_id_otpr + "," + bank_id_pol + "," + 3 + "," + sum + "," + '\'' + date_transaction + '\'' + ");";
                     stmt.executeUpdate(sql);
                     c.commit();
                     // Формирование чека
                     createCheck(bank_id_otpr, bank_id_pol,id_otpr, id_pol,sum,"Перевод", currency,c);
                    }
                else{
                    System.out.println("В данным момент с одним из счетов №" + id_otpr + " и " + id_pol + "выполняются операции. Повторите операцию пополнения счета...");
                    rs.close();
                }
            }
            else {
                System.out.println("Счет не найден...\n");
                rs.close();
            }
        }
        catch (Exception ex){
            System.out.println("Введены неккоректные данные суммы или счета...");
        }
    }

    // метод формирования информации по счету для выписки
    public static void accountInfo(Statement stmt, BufferedReader reader, Connection c){
            try{
                String sql;
                System.out.println("\t\t**********Формирование выписки по счету**********");
                System.out.println("Введите номер счета:");
                int id_deposit  = Integer.parseInt(reader.readLine());
                //Получаем выборку по номеру счета
                sql = "SELECT * FROM deposit WHERE id_deposit = " + id_deposit + ";";
                ResultSet rs = stmt.executeQuery(sql);
                // Обработка выборки для получения информации согласно шаблона
                if(rs.next()) {
                    String format = ".txt";
                    int id_client = rs.getInt("id_client");
                    float balance = rs.getFloat("balance");
                    Date open_date = rs.getDate("open_date");
                    String currency = rs.getString("currency");
                    System.out.println("Ввод начальной даты периода (формат <ЧИСЛО>.<МЕСЯЦ>.<ГОД>, например 01.01.2000):");
                    String start_date = reader.readLine();
                    System.out.println("Ввод конечной даты периода (формат <ЧИСЛО>.<МЕСЯЦ>.<ГОД>, например 01.01.2000):");
                    String end_date = reader.readLine();
                    System.out.println("Выберите формат выписки: \n1 - *.txt \n2 - *.pdf\nПо умолчанию стоит формат *.txt");
                    String id_format = reader.readLine();
                    // в проекте для простоты используется формат *.txt
                    if (id_format.equals("2")) {
                       // format = ".pdf";
                    }
                    createInfo(id_client, id_deposit,open_date,currency, start_date, end_date, balance, format, c);
                }
                else{
                    System.out.println("Счет не найден...\n");
                }
            }
            catch (Exception ex){
                System.out.println("Введены неккоректные данные счета или периода...");
            }
    }

    // метод формирования чеков
    public static void createCheck(int bank_id_otpr, int bank_id_pol, int id_output, int id_input, float
            sum, String type, String currency, Connection c){
        countCheck++;
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String separator = File.separator;
        String bank_otpr = null, bank_pol = null;
        // создание файла с именем номера чека и заполнение информации в нем
        try(FileWriter fileWriter = new FileWriter("check" + separator + countCheck + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "_"
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));Statement st = c.createStatement()) {
            // Поиск названия банка отправителя по id
            ResultSet findbankotpr = st.executeQuery("SELECT * FROM bank WHERE bank_id = " + bank_id_otpr + ";");
            if(findbankotpr.next()){
                bank_otpr = findbankotpr.getString("name");
                bank_otpr = bank_otpr.replaceAll("\n", "");
            }
            // Поиск названия банка получателя по id
            ResultSet findbankpol = st.executeQuery("SELECT * FROM bank WHERE bank_id = " + bank_id_pol + ";");
            if(findbankpol.next()){
                bank_pol = findbankpol.getString("name");
                bank_pol = bank_pol.replaceAll("\n", "");
            }
            fileWriter.write("------------------------------------------------\n");
            fileWriter.write("|"+ "                Банковский ЧЕК                " + "|\n");
            fileWriter.write((String.format("| Чек %40s |\n", countCheck)));
            fileWriter.write(String.format("| %s %33s |\n",currentDate, currentTime));
            fileWriter.write(String.format("| Тип транзакции %29s |\n",type));
            fileWriter.write(String.format("| Банк отправителя %29s\n", bank_otpr + " |"));
            fileWriter.write(String.format("| Банк получателя %30s\n", bank_pol + " |"));
            fileWriter.write(String.format("| Счет отправителя %27s |\n", id_output));
            fileWriter.write(String.format("| Счет получателя %28s |\n", id_input));
            fileWriter.write(String.format("| Сумма: %33.2f %s |\n", sum, currency));
            fileWriter.write("|______________________________________________|\n");
        }
        catch (Exception ex){
            System.out.println("Невозможно создать чек");
        }
    }

    // метод заполнения информации по операциям по счету
    public static void createInfo(int id_client,  int id_deposit, Date open_date,String currency, String start_date, String end_date, float balance, String format, Connection c) throws Exception {
        // Формирование необходмой информации для создания выписки согласно шаблона
        String pattern = "dd.MM.yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH.mm"));
        String client_name = null;
        String date_deposit = dateFormat.format(open_date);
        String period = start_date + " - " + end_date;
        String ostatok = String.format("%.2f %s",balance, currency);
        String filename = "order" + File.separator + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "_" +  LocalTime.now().format(DateTimeFormatter.ofPattern("HHmm")) + format;
        Statement stmt = c.createStatement();
        ResultSet client_info = stmt.executeQuery("SELECT * FROM client WHERE id_client = " + id_client + ";");
        if(client_info.next()) {
            client_name = client_info.getString("last_name") + " " + client_info.getString("first_name") + " " +  client_info.getString("surname");
        }
       // Формирование выборки по заданному временному диапазону
        String sql = "SELECT * FROM transaction WHERE id_output = " + id_deposit + " AND date_transaction BETWEEN " +  '\'' + start_date + '\'' + " AND " + '\'' + end_date + '\'' + ";";
       ResultSet transaction_info = stmt.executeQuery(sql);
       FileWriter writer = new FileWriter(filename);
        //заполняем информацию о счете
        System.out.print("                          Выписка                    \n");
        writer.write("                          Выписка                    \n");
        System.out.print("                         Clever-Bank                    \n");
        writer.write("                         Clever-Bank                    \n");
        System.out.print((String.format("Клиент %24s", "| ")));
        writer.write(String.format("Клиент %24s", "| "));
        System.out.println(client_name);
        writer.write(client_name + "\n");
        System.out.print(String.format("Счет %26s", "| "));
        writer.write(String.format("Счет %26s", "| "));
        System.out.println(id_deposit);
        writer.write(id_deposit + "\n");
        System.out.print(String.format("Валюта %27s\n", "| " + currency));
        writer.write(String.format("Валюта %27s\n", "| " + currency));
        System.out.print(String.format("Дата открытия %27s\n", "| " + date_deposit));
        writer.write(String.format("Дата открытия %27s\n", "| " + date_deposit));
        System.out.print(String.format("Период %47s\n", "| " + period));
        writer.write(String.format("Период %47s\n", "| " + period));
        System.out.print(String.format("Дата и время формирования %22s\n", "| "+ currentDate + ", " +currentTime));
        writer.write(String.format("Дата и время формирования %22s\n", "| "+ currentDate + ", " +currentTime));
        System.out.print(String.format("Остаток %23s", "| "));
        writer.write(String.format("Остаток %23s", "| "));
        System.out.println(String.format("%.2f %s", balance, currency));
        writer.write(String.format("%.2f %s\n", balance, currency));
        System.out.println("   Дата    |         Примечание                    |  Сумма");
        writer.write("   Дата    |         Примечание                    |  Сумма\n");
        System.out.print("-----------------------------------------------------------\n");
        writer.write("-----------------------------------------------------------\n");
       // обработка полученной выборки
        while (transaction_info.next()){
           int deposit_otpr = transaction_info.getInt("id_input");
           int type_operation = transaction_info.getInt("type_operation");
           float sum = transaction_info.getFloat("sum");
           String sum_oper = null;
           String date_transaction = dateFormat.format(transaction_info.getDate("date_transaction"));
           String operacion = null;
           if(type_operation == 1){
               sum_oper = String.format("%.2f %s", sum, currency);
               operacion = "Пополнение";
           }
           if(type_operation == 2) {
               sum_oper = String.format("-%.2f %s", sum,currency);
               operacion = "Снятие средств";
           }
           if(type_operation == 3){
               Statement st = c.createStatement();
               int id_client_otpr = 0;
               sum_oper = String.format("%.2f %s", sum,currency);
               // поиск идентификатора клиента-отправителя  согласно счету
               ResultSet findId = st.executeQuery("SELECT * FROM deposit WHERE id_deposit = " + deposit_otpr + ";");
               if(findId.next()) {
                   id_client_otpr = findId.getInt("id_client");
               }
               // Получение фамилии клиента-отправителя
               ResultSet findName = st.executeQuery("SELECT * FROM client WHERE id_client = " + id_client_otpr + ";");
               if(findName.next()){
                   String lastname = findName.getString("last_name");
                   operacion = "Пополнение от " + lastname;
               }
           findId.close();
           findName.close();
           }
           //заполнение информации по операциям по счету согласно шаблона
           System.out.print(String.format("%s | %-37s %s",date_transaction, operacion, "| "));
            writer.write(String.format("%s | %-37s %s",date_transaction, operacion, "| "));
           System.out.println(sum_oper);
            writer.write(sum_oper + "\n");
       }
        writer.write("\n");
        writer.close();
    }
}