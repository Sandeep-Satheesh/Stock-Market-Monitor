package utils;

public abstract class Constants {
    public static String getApiKey() {
        return API_KEY;
    }

    public static void setApiKey(String apiKey) {
        API_KEY = apiKey;
    }

    public static String API_KEY = "";
    public static final String SERVER_URL = "https://api.twelvedata.com";
}
