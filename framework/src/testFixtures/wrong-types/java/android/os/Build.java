package android.os;

public class Build {
    public static Object MODEL = Integer.valueOf(12345); // wrong type

    public static class VERSION {
        public static Object SDK_INT = "Ten"; // wrong type, expected Integer!
    }
}
