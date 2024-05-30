import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class VKBot {
    public static String ACCESS_TOKEN = "";
    public static String VERSION = "";

    public static void main(String[] args) throws Exception {
        loadConfig();
        while (true) {
            checkMessages();
            Thread.sleep(1000);
        }
    }

    private static void loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            ACCESS_TOKEN = prop.getProperty("access_token");
            VERSION = prop.getProperty("version");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static void checkMessages() throws Exception {
        String url = String.format("https://api.vk.com/method/messages.getConversations?access_token=%s&v=%s", ACCESS_TOKEN, VERSION);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                JSONArray items = json.getJSONObject("response").getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject lastMessage = item.getJSONObject("last_message");

                    String text = lastMessage.getString("text");
                    int userId = lastMessage.getInt("from_id");

                    String reply = "Вы сказали : " + text;

                    sendMessage(userId, reply);
                }
            }
        }
    }

    private static void sendMessage(int userId, String message) throws Exception {
        String url = "https://api.vk.com/method/messages.send";

        String body = String.format("user_id=%d&message=%s&random_id=%d&access_token=%s&v=%s",
                userId,
                URLEncoder.encode(message, StandardCharsets.UTF_8),
                System.currentTimeMillis(),
                ACCESS_TOKEN,
                VERSION);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity(body));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Message sent to user " + userId + ": " + message);
                System.out.println("Response: " + responseBody);
            }
        }
    }
}
