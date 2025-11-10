package expenses.bot;

public class NormalizationHelper {
    public static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
