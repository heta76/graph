package heta.example.gui;
/** Разбор чисел из полей ввода (точка и запятая). */
public final class NumberParsing {
    private NumberParsing() {
    }
    public static double parseDouble(String raw) {
        if (raw == null) {
            throw new NumberFormatException("пустое значение");
        }
        String s = raw.trim().replace(',', '.');
        if (s.isEmpty()) {
            throw new NumberFormatException("пустое значение");
        }
        return Double.parseDouble(s);
    }
}