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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    private static BufferedReader console;
    public final String BLANK = "";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean debug = true;
    private Logger logger;
    private File jsonLoggerFile;
    private WebSocket webSocket;
    private int sleepInterval = 30;
    private final long MINUTES_RSS_FEED_INTERVAL = 30;
    private final long ZERO_INITIAL_DELAY = 0;
    private final FeedDAO feedDAO = new FeedDAO();
    private final SubscriptionDAO subscriptionDAO = new SubscriptionDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final String TAG = this.getClass().getCanonicalName();

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
        logger = getLogger();
        Application application = applicationDAO.get(1);
        if (!application.isFound()) {
            console = new BufferedReader(new InputStreamReader(System.in));
            while (!application.isFound()) {
                application = createApp(application);
            }
        }
        System.out.format("Using instance: %s as %s\n", application.getInstanceName(), whoami());
        WorkerRssFeeds worker = new WorkerRssFeeds();
        ScheduledFuture scheduledFuture = scheduler.scheduleAtFixedRate(worker, ZERO_INITIAL_DELAY, MINUTES_RSS_FEED_INTERVAL, TimeUnit.MINUTES);
    }

    private void setupWebsocket() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        Application application = applicationDAO.get(1);
        String urlString = String.format("wss://%s/api/v1/streaming/?stream=user&access_token=%s",
                application.getInstanceName(), application.getAccessToken());
        System.out.format("Streaming URL: %s\n", urlString);
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

    private Application createApp(Application application) {
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
            return application;
        }
        //   System.out.format("%s\n", jsonObject.toString());
        String urlOauthDance = String.format("https://%s/oauth/authorize?scope=%s&response_type=code&redirect_uri=%s&client_id=%s\n",
                instance, Utils.urlEncodeComponent("write read follow push"), Utils.urlEncodeComponent(jsonObject.get("redirect_uri").getAsString()), jsonObject.get("client_id").getAsString());
        System.out.format("Go to %s", urlOauthDance);
        String token = ask("Paste the token and press ENTER.");
        if (token == null || token.trim().length() < 20) {
            System.out.format("Token \"%s\" doesn't look valid. Try again.\n", token);
            return application;
        }
        urlString = String.format("https://%s/oauth/token", instance);
        params = new JsonObject();
        params.addProperty(Literals.client_id.name(), jsonObject.get(Literals.client_id.name()).getAsString());
        params.addProperty(Literals.client_secret.name(), jsonObject.get(Literals.client_secret.name()).getAsString());
        params.addProperty(Literals.grant_type.name(), Literals.authorization_code.name());
        params.addProperty(Literals.code.name(), token);
        params.addProperty(Literals.redirect_uri.name(), jsonObject.get(Literals.redirect_uri.name()).getAsString());
        application.setClientId(jsonObject.get(Literals.client_id.name()).getAsString());
        application.setClientSecret(jsonObject.get(Literals.client_secret.name()).getAsString());
        application.setApplicationId(jsonObject.get(Literals.id.name()).getAsString());
        JsonObject outputJsonObject = postAsJson(Utils.getUrl(urlString), params.toString());
        application.setAccessToken(outputJsonObject.get(Literals.access_token.name()).getAsString());
        application.setRefreshToken(Utils.getProperty(outputJsonObject, Literals.refresh_token.name()));
        application.setUser(Utils.getProperty(outputJsonObject, Literals.me.name()));
        application.setInstanceName(instance);
        application = applicationDAO.add(application);
        System.out.format("Added. You are now %s\n", whoami());
        return application;
    }

    private String whoami() {
        Application application = applicationDAO.get(1);
        String urlString = String.format("https://%s/api/v1/accounts/verify_credentials",
                application.getInstanceName());
        JsonElement jsonElement = getJsonElement(urlString);
        return String.format("%s %s", Utils.getProperty(jsonElement, Literals.username.name()), Utils.getProperty(jsonElement, Literals.url.name()));
    }

    private JsonElement getJsonElement(String urlString) {
        return getJsonElement(urlString, false);
    }

    private JsonElement getJsonElement(String urlString, boolean ignoreExceptions) {
        URL url = Utils.getUrl(urlString);
        HttpsURLConnection urlConnection;
        Application application = applicationDAO.get(1);
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            String authorization = String.format("Bearer %s", application.getAccessToken());
            urlConnection.setRequestProperty("Authorization", authorization);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
        System.out.format("postAsJson URL: %s JSON: \n%s\n", url.toString(), json);
        HttpsURLConnection urlConnection;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        JsonObject jsonObject = null;
        Application application = applicationDAO.get(1);
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", "Jediverse CLI");
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod(Literals.POST.name());
            String authorization = String.format("Bearer %s", application.getAccessToken());
            urlConnection.setRequestProperty("Authorization", authorization);
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
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            jsonObject = gson.fromJson(isr, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonObject();
        } finally {
            Utils.close(inputStream, outputStream);
        }
        return jsonObject;
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
        ArrayList<JsonElement> uploadedMediaIds = new ArrayList<>();
        String text = getText(statusJe);
        System.out.format("Handling message from %s: \"%s\". %s\n", Utils.getProperty(accountJe, Literals.acct.name()), text, new Date());
        text = clean(text);
        if (Utils.isBlank(text)) {
            System.out.format("Text not found in message %s\n", jsonElement);
        }
        String output = null;
        if (Literals.help.name().equalsIgnoreCase(text)) {
            output = String.format("Help response goes here. %s", new Date());
        }
        if (Literals.date.name().equalsIgnoreCase(text)) {
            output = String.format("%s", new Date());
        }
        if (Literals.help.name().equalsIgnoreCase(text)) {
            output = help();
        }
        String[] words = text.split("\\s+");
        if (Literals.pla.name().equalsIgnoreCase(accountName)) {
            if (text.equalsIgnoreCase(Literals.quit.name())) {
                System.out.format("Received quit request from PLA. %s\n", new Date());
                System.exit(0);
            }
            int i = 0;
            if (words.length == 4 &&
                    words[i++].equals("add") &&
                    words[i++].equals("feed")) {
                String urlString = words[i++];
                String label = words[i++];
                if (!urlString.startsWith("http")) {
                    output = String.format("%s should start with http. %s - Example: add feed https://xkcd.com/atom.xml xkcd", urlString, Utils.SYMBOL_THINKING);
                } else {
                    Feed feed = feedDAO.get(urlString, label);
                    if (feed.isFound()) {
                        output = String.format("Feed already exists with URL: %s and label: %s.", urlString, label);
                    } else {
                        feed = feedDAO.add(urlString, label);
                        output = String.format("Feed added to table: %s. Will update the feed next.", feed.isFound());
                        WorkerRssFeeds worker = new WorkerRssFeeds();
                        worker.start();
                    }
                }
            }
        }
        if (words.length == 1) {
            Feed feed = feedDAO.get(text);
            if (feed.isFound()) {
                postMessage(feed, Utils.getProperty(statusJe, Literals.id.name()), null);
                return;
            }
            if (Literals.ping.name().equalsIgnoreCase(words[0])) {
                output = "You need to provide an IP address or host name to ping. Example: ping 8.8.8.8";
            }
            if ("subscriptions".equals(text)) {
                ArrayList<Feed> feeds = feedDAO.getFromUser(accountName);
                if (feeds.isEmpty()) {
                    output = String.format("%s isn't subscribed to any feeds. Try something like: subscribe xkcd", accountName);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("You are subscribed to: ");
                    String comma = "";
                    for (Feed f : feeds) {
                        sb.append(comma);
                        sb.append(f.getLabel());
                        comma = ", ";
                    }
                    sb.append(".");
                    output = sb.toString();
                }
            }
        }
        if (words.length == 2) {
            if (Literals.ping.name().equalsIgnoreCase(words[0])) {
                output = Utils.ping(words[1]);
            }
            if (Literals.date.name().equalsIgnoreCase(words[0])) {
                try {
                    output = ZonedDateTime.now(ZoneId.of(words[1])).toString();
                } catch (ZoneRulesException e) {
                    output = String.format("%s\n\nValid zone IDs - https://paste.ubuntu.com/p/thZHPZkdrd/", e.getLocalizedMessage());
                }
            }
            if ("subscribe".equalsIgnoreCase(words[0])) {
                Feed feed = feedDAO.get(words[1]);
                if (feed.isFound()) {
                    Subscription subscription = subscriptionDAO.get(feed, accountName);
                    if (subscription.isFound()) {
                        output = String.format("You are already subscribed to %s.", words[1]);
                    } else {
                        subscription = subscriptionDAO.add(feed, accountName);
                        if (subscription.isFound()) {
                            output = String.format("You are now subscribed to %s", words[1]);
                        } else {
                            output = String.format("Subscription to %s failed.", words[1]);
                        }
                    }
                } else {
                    output = String.format("Feed with label: \"%s\" not found.", words[1]);
                }
            }
            if ("unsubscribe".equalsIgnoreCase(words[0])) {
                Feed feed = feedDAO.get(words[1]);
                if (feed.isFound()) {
                    Subscription subscription = subscriptionDAO.get(feed, accountName);
                    if (!subscription.isFound()) {
                        output = String.format("You are not subscribed to %s.", words[1]);
                    } else {
                        boolean deleted = subscriptionDAO.delete(feed, accountName);
                        if (deleted) {
                            output = String.format("You have been unsubscribed from %s.", words[1]);
                        } else {
                            output = String.format("Unsubscribed from %s failed.", words[1]);
                        }
                    }
                } else {
                    output = String.format("Feed with label: \"%s\" not found.", words[1]);
                }
            }
        }
        if (Utils.isBlank(output)) {
            output = String.format("Don't know how to respond to \"%s\". %s", text, Utils.SYMBOL_THINKING);
        }
        postStatus(output, Utils.getProperty(statusJe, Literals.id.name()), uploadedMediaIds);
    }

    private String help() {
        StringBuilder sb = new StringBuilder();
        sb.append("Commands you can send to this bot: help, ping, date, subscribe, unsubscribe, subscriptions, ");
        ArrayList<Feed> feeds = feedDAO.get();
        String comma = "";
        for (Feed feed : feeds) {
            sb.append(comma);
            sb.append(feed.getLabel());
            comma = ", ";
        }
        sb.append("\n\nYou can subscribe to a feed like: subscribe xkcd");
        sb.append("\n\nTo unsubscribe from a feed: unsubscribe xkcd");
        sb.append(".\n\nCommands are case-sensitive.");
        return sb.toString();
    }

    private void postMessage(Feed feed, String inReplyToId, String userName) {
        String text;
        if (feed.getUrl().contains("youtube.com")) {
            text = String.format("%s\n\n%s",
                    feed.getTitle(),
                    feed.getUrl());
        } else {
            text = String.format("%s\n\n%s\n\n%s",
                    feed.getTitle(),
                    feed.getDescription(),
                    feed.getUrl());
        }
        if (Utils.isNotBlank(userName)) {
            text = String.format("%s %s", userName, text);
        }
        ArrayList<JsonElement> mediaList = new ArrayList<>();
        if (Utils.isNotBlank(feed.getUploadedMedialId())) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(Literals.id.name(), feed.getUploadedMedialId());
            mediaList.add(jsonObject);
        }
        postStatus(text, inReplyToId, mediaList);
    }

    private void postStatus(String text, String inReplyToId, ArrayList<JsonElement> mediaArrayList) {
        System.out.format("Post in reply to: %s Text: %s\n", inReplyToId, text);
        Application application = applicationDAO.get(1);
        String urlString = String.format("https://%s/api/v1/statuses", application.getInstanceName());
        JsonObject params = new JsonObject();
        params.addProperty(Literals.status.name(), text);
        params.addProperty(Literals.visibility.name(), Literals.direct.name());
        if (mediaArrayList != null && !mediaArrayList.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            for (JsonElement jsonElement : mediaArrayList) {
                jsonArray.add(Utils.getProperty(jsonElement, Literals.id.name()));
            }
            params.add(Literals.media_ids.name(), jsonArray);
        }
        if (Utils.isNotBlank(inReplyToId)) {
            params.addProperty("in_reply_to_id", inReplyToId);
        }
        JsonObject jsonObject = postAsJson(Utils.getUrl(urlString), params.toString());
        System.out.format("Status posted: %s\n", jsonObject);
    }

    public enum Literals {
        id, me, client_name, scopes, website, grant_type, access_token, refresh_token,
        redirect_uri, redirect_uris, client_id, client_secret, code, content, type, status,
        url, notification, event, payload, acct, POST, mention, help, quit, username,
        visibility, title, media_ids, file, description, authorization_code, account,
        date, pla, ping, link, enclosure, nasaImageOfTheDay, nasa, pubDate, hpr,
        hprLatestEpisode, updated, summary, xkcd, direct, label,
        item, entry
    }

    class WorkerRssFeeds extends Thread {

        @Override
        public void run() {
            ArrayList<Feed> feeds = feedDAO.get();
            System.out.format("%s running at %s. %d feeds to check.\n", TAG, new Date(), feeds.size());
            int quantityChanged = 0;
            for (Feed feed : feeds) {
                String title = feed.getTitle();
                feed = feedDAO.populateWithLatestRssEntry(feed);
                if (title == null || !title.equalsIgnoreCase(feed.getTitle())) {
                    if (Utils.isNotBlank(feed.getMediaUrl())) {
                        feed = feedDAO.uploadMedia(feed);
                    }
                    feed = feedDAO.update(feed);
                    String userName = "@pla";
                    postMessage(feed, null, userName);
                    quantityChanged++;
                    // TODO in the future we'll do subscription notifications here.
                }
            }
            System.out.format("%s running at %s. %d out of  %d feeds changed.\n",
                    TAG, new Date(), quantityChanged, feeds.size());
        }
    }

    private class WebSocketListener implements WebSocket.Listener {
        private final String stream;
        private StringBuilder sb = new StringBuilder();

        WebSocketListener(String stream) {
            this.stream = stream;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (last) {
                sb.append(data);
                System.out.format("Web socket mesage: \"%s\".\n", sb.toString());
                if (sb.toString().trim().length() == 0) {
                    System.out.format("Websocket message is blank. %s\n", new Date());
                } else {
                    JsonElement messageJsonElement = JsonParser.parseString(sb.toString());
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
                }
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
