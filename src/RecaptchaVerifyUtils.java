package servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

public class RecaptchaVerifyUtils {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public static void verify(String gRecaptchaResponse) throws Exception {
        if (gRecaptchaResponse == null || gRecaptchaResponse.length() == 0) {
            throw new Exception("reCAPTCHA response is missing");
        }

        URL url = new URL(VERIFY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setDoOutput(true);

        String postParams = "secret=" + RecaptchaConstants.SECRET_KEY + "&response=" + gRecaptchaResponse;
        OutputStream outStream = conn.getOutputStream();
        outStream.write(postParams.getBytes());
        outStream.flush();
        outStream.close();

        InputStream inputStream = conn.getInputStream();
        InputStreamReader reader = new InputStreamReader(inputStream);
        JsonObject jsonObject = new Gson().fromJson(reader, JsonObject.class);
        reader.close();

        if (!jsonObject.get("success").getAsBoolean()) {
            throw new Exception("reCAPTCHA verification failed: " + jsonObject.toString());
        }
    }
}
