package social.pla.jedibot;

import com.google.gson.*;
import org.jsoup.Jsoup;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletionStage;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    private static BufferedReader console;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonObject settings;
    private boolean debug = true;
    private Logger logger;
    private File jsonLoggerFile;
    private WebSocket webSocket;
    private int sleepInterval = 30;
    private final String BLANK = "";

    public Main() {
        setup();
        setupWebsocket();
        while (webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed()) {
            System.out.format("Web socket is open at %s\n", new Date());
            Utils.sleep(sleepInterval);
        }
        System.out.format("Web socket died at %s.\n", new Date());
        System.exit(0);
    }

    public static void main(String[] args) {
        System.out.format("%s\n", new Date());
        new Main();
    }

    private void setup() {
        console = new BufferedReader(new InputStreamReader(System.in));
        logger = getLogger();
        settings = Utils.getSettings();
        while (settings == null) {
            createApp();
            settings = Utils.getSettings();
        }
        System.out.format("Using instance: %s as %s\n", settings.get(Literals.instance.name()), whoami());
    }

    private void setupWebsocket() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        String urlString = String.format("wss://%s/api/v1/streaming/?stream=user&access_token=%s",
                Utils.getProperty(settings, Literals.instance.name()), Utils.getProperty(settings, Literals.access_token.name()));
        WebSocketListener webSocketListener = new WebSocketListener("user");
        webSocket = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(URI.create(urlString), webSocketListener).join();

    }

    private String ask(String prompt) {
        System.out.format("%s\n", prompt);
        return readConsole();
    }

    private String readConsole() {
        try {
            return console.readLine();
        } catch (IOException e) {
            System.out.format("%s\n", e.getLocalizedMessage());
            return "";
        }
    }

    private void createApp() {
        String prompt = "Type your instance name and press ENTER. For example: pleroma.site";
        String instance = ask(prompt);
        while (Utils.isBlank(instance)) {
            System.out.format("Instance can not be blank.\n");
            instance = ask(prompt);
        }
        JsonObject params = new JsonObject();
        params.addProperty(Literals.client_name.name(), "Jedibot");
        params.addProperty(Literals.redirect_uris.name(), "urn:ietf:wg:oauth:2.0:oob");
        params.addProperty(Literals.scopes.name(), "read write follow push");
        params.addProperty(Literals.website.name(), "https://github.com/pla1/Jedibot");
        String urlString = String.format("https://%s/api/v1/apps", instance);
        JsonObject jsonObject = postAsJson(Utils.getUrl(urlString), params.toString());
        if (!Utils.isJsonObject(jsonObject)) {
            System.out.format("Something went wrong while creating app on instance \"%s\". Try again.\n", instance);
            return;
        }
        //   System.out.format("%s\n", jsonObject.toString());
        String urlOauthDance = String.format("https://%s/oauth/authorize?scope=%s&response_type=code&redirect_uri=%s&client_id=%s\n",
                instance, Utils.urlEncodeComponent("write read follow push"), Utils.urlEncodeComponent(jsonObject.get("redirect_uri").getAsString()), jsonObject.get("client_id").getAsString());
        System.out.format("Go to %s", urlOauthDance);
        String token = ask("Paste the token and press ENTER.");
        if (token == null || token.trim().length() < 20) {
            System.out.format("Token \"%s\" doesn't look valid. Try again.\n", token);
            return;
        }
        urlString = String.format("https://%s/oauth/token", instance);
        params = new JsonObject();
        params.addProperty(Literals.client_id.name(), jsonObject.get(Literals.client_id.name()).getAsString());
        params.addProperty(Literals.client_secret.name(), jsonObject.get(Literals.client_secret.name()).getAsString());
        params.addProperty(Literals.grant_type.name(), Literals.authorization_code.name());
        params.addProperty(Literals.code.name(), token);
        params.addProperty(Literals.redirect_uri.name(), jsonObject.get(Literals.redirect_uri.name()).getAsString());
        JsonObject outputJsonObject = postAsJson(Utils.getUrl(urlString), params.toString());
        jsonObject.addProperty(Literals.access_token.name(), outputJsonObject.get(Literals.access_token.name()).getAsString());
        jsonObject.addProperty(Literals.refresh_token.name(), Utils.getProperty(outputJsonObject, Literals.refresh_token.name()));
        jsonObject.addProperty(Literals.me.name(), Utils.getProperty(outputJsonObject, Literals.me.name()));
        jsonObject.addProperty(Literals.expires_in.name(), Utils.getProperty(outputJsonObject, Literals.expires_in.name()));
        jsonObject.addProperty(Literals.created_at.name(), Utils.getProperty(outputJsonObject, Literals.created_at.name()));
        jsonObject.addProperty(Literals.instance.name(), instance);
        jsonObject.addProperty(Literals.milliseconds.name(), System.currentTimeMillis());
        String pretty = gson.toJson(jsonObject);
        Utils.write(Utils.getSettingsFileName(), pretty);
        settings = jsonObject;
        System.out.format("Added. You are now %s\n", whoami());
    }

    private String whoami() {
        String urlString = String.format("https://%s/api/v1/accounts/verify_credentials", Utils.getProperty(settings, Literals.instance.name()));
        JsonElement jsonElement = getJsonElement(urlString);
        return String.format("%s %s", Utils.getProperty(jsonElement, Literals.username.name()), Utils.getProperty(jsonElement, Literals.url.name()));
    }

    private JsonElement getJsonElement(String urlString) {
        return getJsonElement(urlString, false);
    }

    private JsonElement getJsonElement(String urlString, boolean ignoreExceptions) {
        URL url = Utils.getUrl(urlString);
        HttpsURLConnection urlConnection;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            String authorization = String.format("Bearer %s", settings.get(Literals.access_token.name()).getAsString());
            urlConnection.setRequestProperty("Authorization", authorization);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            return gson.fromJson(isr, JsonElement.class);
        } catch (IOException e) {
            if (!ignoreExceptions) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String getText(JsonElement jsonElement) {
        String content = Utils.getProperty(jsonElement, Literals.content.name());
        String text = "";
        if (Utils.isNotBlank(content)) {
            text = Jsoup.parse(content).text();
        }
        return text;
    }

    private Logger getLogger() {
        Logger logger = Logger.getLogger("JedibotJsonLog");
        if (!debug) {
            LogManager.getLogManager().reset();
            return logger;
        }
        FileHandler fh;
        try {
            jsonLoggerFile = File.createTempFile("jediverse_json_log_", ".log");
            System.out.format("JSON log file: %s\n", jsonLoggerFile.getAbsolutePath());
            fh = new FileHandler(jsonLoggerFile.getAbsolutePath());
            logger.setUseParentHandlers(false);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        logger.info(String.format("This file: %s", jsonLoggerFile.getAbsolutePath()));
        return logger;
    }

    private JsonObject postAsJson(URL url, String json) {
        //    System.out.format("postAsJson URL: %s JSON: \n%s\n", url.toString(), json);
        HttpsURLConnection urlConnection;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        JsonObject jsonObject = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", "Jediverse CLI");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod(Literals.POST.name());
            if (settings != null) {
                String authorization = String.format("Bearer %s", Utils.getProperty(settings, Literals.access_token.name()));
                urlConnection.setRequestProperty("Authorization", authorization);
                //         System.out.format("Setting authorization header: %s\n", authorization);
            }
            if (json != null) {
                urlConnection.setRequestProperty("Content-type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-length", Integer.toString(json.length()));
                outputStream = urlConnection.getOutputStream();
                outputStream.write(json.getBytes());
                outputStream.flush();
                int responseCode = urlConnection.getResponseCode();
                System.out.format("Response code: %d\n", responseCode);
            }
            urlConnection.setInstanceFollowRedirects(true);
            inputStream = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            jsonObject = gson.fromJson(isr, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(inputStream, outputStream);
        }
        return jsonObject;
    }

    private enum Literals {
        audioFileNotifications, audioFileFails, id, instance, me, milliseconds, quantity,
        browserCommand, client_name, scopes, website, grant_type, access_token, refresh_token,
        redirect_uri, redirect_uris, client_id, client_secret, code, expires_in, created_at, content, type, status,
        none, search, clear, about, blocks, context, debug, ok, url, go, notification, event, payload, acct, display_name,
        properties, local, notifications, timeline, note, tl, following, followers, lists, gc, stop, home, post, POST, DELETE, unlisted,
        follow, reblog, favourite, mention, direct, fav, reply, rep, help, quit, exit, whoami, unfav, account_ids, username,
        visibility, upload, unfollow, title, media_ids, file, description, authorization_code, followed_by, history, day, uses, name,
        ancestors, descendants, account, accounts, hashtags, statuses, media_attachments, aa, sa, da, user_count, status_count,
        domain_count, stats, registrations, version, protocols, staffAccounts, metadata, postFormats, quarantined_instances, mrf_policies, mrf_simple,
        federation, reject, report_removal, media_removal, federated_timeline_removal, banner_removal, avatar_removal, accept,
        media_nsfw, onstart, date, pla, ping
    }

    private String clean(String text) {
        text = text.replaceAll("@bot@pla.social", BLANK);
        text = text.replaceAll("@bot", BLANK);
        return text.trim();
    }

    private void handleMessage(JsonElement jsonElement) {
        if (jsonElement == null || !Utils.isJsonObject(jsonElement)) {
            System.out.format("Message is blank.\n");
            return;
        }
        String type = Utils.getProperty(jsonElement, Literals.type.name());
        if (!Literals.mention.name().equals(type)) {
            System.out.format("Not a mention. %s\n", jsonElement);
            return;
        }
        JsonElement statusJe = jsonElement.getAsJsonObject().get(Literals.status.name());
        JsonElement accountJe = jsonElement.getAsJsonObject().get(Literals.account.name());
        String accountName = Utils.getProperty(accountJe, Literals.acct.name());
        String text = getText(statusJe);
        System.out.format("Handling message: \"%s\".\n", text);
        text = clean(text);
        if (Utils.isBlank(text)) {
            System.out.format("Text not found in message %s\n", jsonElement);
        }
        String visibility = Utils.getProperty(statusJe, Literals.visibility.name());
        String output = null;
        if (Literals.help.name().equalsIgnoreCase(text)) {
            output = String.format("Help response goes here. %s", new Date());
        }
        if (Literals.date.name().equalsIgnoreCase(text)) {
            output = String.format("%s", new Date());
        }
        if (text.equalsIgnoreCase(Literals.quit.name())
                && Literals.pla.name().equalsIgnoreCase(accountName)) {
            System.out.format("Received quit request from PLA. %s\n", new Date());
            System.exit(0);
        }
        String[] words = text.split("\\s+");
        if (words.length == 2 && Literals.ping.name().equalsIgnoreCase(words[0])) {
           output = Utils.ping(words[1]);
        }
        if (Utils.isBlank(output)) {
            output = String.format("Don't know how to respond to \"%s\". %s", text, Utils.SYMBOL_THINKING);
        }
        postStatus(output, Utils.getProperty(statusJe, Literals.id.name()), visibility);
    }

    private void postStatus(String text, String inReplyToId, String visibility) {
        String urlString = String.format("https://%s/api/v1/statuses", Utils.getProperty(settings, Literals.instance.name()));
        JsonObject params = new JsonObject();
        params.addProperty(Literals.status.name(), text);
        params.addProperty(Literals.visibility.name(), visibility);
        if (Utils.isNotBlank(inReplyToId)) {
            params.addProperty("in_reply_to_id", inReplyToId);
        }
        JsonObject jsonObject = postAsJson(Utils.getUrl(urlString), params.toString());
        System.out.format("Status posted: %s\n", jsonObject.get(Literals.url.name()).getAsString());
    }

    private class WebSocketListener implements WebSocket.Listener {
        private final String stream;
        private final JsonParser jsonParser = new JsonParser();
        private StringBuilder sb = new StringBuilder();

        WebSocketListener(String stream) {
            this.stream = stream;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (last) {
                sb.append(data);
                JsonElement messageJsonElement = jsonParser.parse(sb.toString());
                logger.info(gson.toJson(messageJsonElement));
                if (Utils.isJsonObject(messageJsonElement)) {
                    String event = Utils.getProperty(messageJsonElement, Literals.event.name());
                    String payloadString = messageJsonElement.getAsJsonObject().get(Literals.payload.name()).getAsString();
                    if (Literals.notification.name().equals(event)) {
                        System.out.format("%s", Utils.SYMBOL_SPEAKER);
                    }
                    if (Utils.isNotBlank(payloadString)) {
                        JsonElement payloadJsonElement = JsonParser.parseString(payloadString);
                        if (Utils.isJsonObject(payloadJsonElement)) {
                            System.out.format("%s\n", payloadJsonElement);
                            handleMessage(payloadJsonElement);
                        }
                    } else {
                        System.out.format("Payload is blank.\n");
                    }
                }
                sb = new StringBuilder();
            } else {
                sb.append(data);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            //     System.out.format("onPing Message: %s\n", message);
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            System.out.format("onPong Message: %s\n", message);
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.format("WebSocket opened for %s stream.\n", stream);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.format("WebSocket closed. Status code: %d Reason: %s Stream: %s\n.", statusCode, reason, stream);
            webSocket.abort();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.format("WebSocket error: %s.\n", error.getLocalizedMessage());
            error.printStackTrace();
            WebSocket.Listener.super.onError(webSocket, error);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.format("Binary data received on WebSocket.\n");
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }
    }
}
