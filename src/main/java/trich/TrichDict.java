package trich;

import java.util.*;

public class TrichDict{
    
    public static Map<Integer, String> weekDays = new HashMap<>();
    
    public static Map<Integer, String> months = new HashMap<>();
    
    static {
        System.out.println("Static TrichDict initializing");
        months.put(0, "Янв");
        months.put(1, "Фев");
        months.put(2, "Мар");
        months.put(3, "Апр");
        months.put(4, "Май");
        months.put(5, "Июн");
        months.put(6, "Июл");
        months.put(7, "Авг");
        months.put(8, "Сен");
        months.put(9, "Окт");
        months.put(10, "Ноя");
        months.put(11, "Дек");
        weekDays.put(1, "Пнд");
        weekDays.put(2, "Втр");
        weekDays.put(3, "Срд");
        weekDays.put(4, "Чтв");
        weekDays.put(5, "Птн");
        weekDays.put(6, "Суб");
        weekDays.put(0, "Вск");
    }
    
}