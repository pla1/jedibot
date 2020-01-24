package social.pla.jedibot;

import com.google.gson.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.zone.ZoneRulesProvider;
import java.util.*;

public class Utils {
    public static final String SYMBOL_SPEAKER = "\uD83D\uDD0A";
    public static final String SYMBOL_PEACE = "\u262E";
    public static final String SYMBOL_THINKING = "\uD83E\uDD14";
    public static final String SYMBOL_THUMBSUP = "\uD83D\uDC4D";
    public static final String SYMBOL_THUMBSDOWN = "\uD83D\uDC4E";


    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (true) {
            Utils.printTable("subscription");
            System.exit(0
            );
        }
        if (false) {
            Connection connection = Utils.getConnection();
            Utils.close(connection);
            System.exit(0);
        }
        if (false) {
            System.out.println(Utils.listZoneIds());
            System.exit(0);
        }
        if (false) {
            String urlString = "http://hackerpublicradio.org/eps/hpr2991.ogg";
            URL url = new URL(urlString);
            URL newUrl = Utils.getRedirect(url);
            System.out.format("%s\n", newUrl);
            System.exit(0);
        }
        if (false) {
            String urlString = "http://hackerpublicradio.org/eps/hpr2991.ogg";
            File file = Utils.downloadAudio(urlString);
            System.out.format("%s\n", file.getAbsolutePath());
            System.exit(0);

        }

        System.exit(0);
    }

    public static URL getUrl(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static File downloadImage(String urlString) {
        System.out.format("Download: %s\n", urlString);
        try {
            URL url = getRedirect(new URL(urlString));
            BufferedImage image = ImageIO.read(url);
            File outputFile = File.createTempFile(Main.Literals.nasaImageOfTheDay.name(), ".jpg");
            ImageIO.write(image, "jpg", outputFile);
            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFullDateAndTime(Timestamp timestamp) {
        java.util.Date date = (java.util.Date) timestamp;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd, yyyy hh:mm:ss a");
        return sdf.format(timestamp);
    }

    public static long getLong(Timestamp timestamp) {
        if (timestamp == null) {
            return 0;
        }
        return timestamp.getTime();
    }

    public static void dropTableIfExists(Connection connection, String tableName) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(String.format("drop table %s", tableName));
            System.out.format("Table %s dropped.\n", tableName);
        } catch (Exception e) {
            System.out.format("Table \"%s\" not dropped. %s.\n", tableName, e.getLocalizedMessage());
        } finally {
            Utils.close(statement);
        }
    }

    public static URL getRedirect(URL url) throws IOException {
        HttpURLConnection connection = null;
        String location = url.toString();
        for (; ; ) {
            url = new URL(location);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            String redirectLocation = connection.getHeaderField("Location");
            System.out.format("New location: %s\n", redirectLocation);
            if (redirectLocation == null) break;
            location = redirectLocation;
        }
        return new URL(location);
    }

    public static String readFileToString(String fileName) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(bufferedReader);
        }
        return "";
    }

    public static void printTable(String tableName) {
        Connection connection = null;
        Statement statement = null;
        ResultSet rs;
        try {
            connection = Utils.getConnection();
            statement = connection.createStatement();
            rs = statement.executeQuery(String.format("select * from %s", tableName));
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsmd.getColumnName(i);
                String columnType = rsmd.getColumnTypeName(i);
                System.out.format("%s %s\n", columnName, columnType);
            }
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                  System.out.format("%s\t", rs.getString(i));
                }
                System.out.format("\n");
            }
        } catch (Exception e) {
            System.out.format("Table \"%s\" not dropped. %s.\n", tableName, e.getLocalizedMessage());
        } finally {
            Utils.close(statement);
        }
    }

    public static String listZoneIds() {
        StringBuilder sb = new StringBuilder();
        Set<String> list = ZoneRulesProvider.getAvailableZoneIds();
        ArrayList<String> zoneIDs = new ArrayList<String>(list);
        Collections.sort(zoneIDs);
        sb.append(String.format("%d zone IDs\n", list.size()));
        for (String zoneId : zoneIDs) {
            sb.append(zoneId).append("\n");
        }
        return sb.toString();
    }

    public static File downloadMedia(String urlString) {
        OutputStream outputStream = null;
        int position = urlString.lastIndexOf(".");
        String fileSuffix = "UNKNOWN";
        if (position > 0) {
            fileSuffix = urlString.substring(position);
        }
        try {
            URL url = getRedirect(new URL(urlString));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            InputStream inputStream = urlConnection.getInputStream();
            File outputFile = File.createTempFile("media_download_", fileSuffix);
            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            System.out.format("Downloaded %s to %s.\n", urlString, outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(outputStream);
        }
        return null;
    }

    public static File downloadAudio(String urlString) {
        OutputStream outputStream = null;
        try {
            URL url = getRedirect(new URL(urlString));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            InputStream inputStream = urlConnection.getInputStream();
            File outputFile = File.createTempFile(Main.Literals.hprLatestEpisode.name(), ".mp3");
            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(outputStream);
        }
        return null;
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data, String boundary) throws IOException {
        ArrayList<byte[]> byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
                .getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
            if (entry.getValue() instanceof Path) {
                Path path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byte[] fileBytes = Files.readAllBytes(path);
                //      System.out.format("Files.readAllBytes %d length.\n%s", fileBytes.length, new String(fileBytes));
                byteArrays.add(fileBytes);
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, true);
    }

    public static JsonObject uploadMedia(File file) {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.get(1);
        String urlString = String.format("https://%s/api/v1/media", application.getInstanceName());
        System.out.format("Upload image %s\n", urlString);
        if (!file.exists()) {
            System.out.format("File: \"%s\" does not exist.\n", file.getAbsolutePath());
            return null;
        }
        HttpClient client = HttpClient.newBuilder().build();
        Map<Object, Object> data = new LinkedHashMap<>();
        data.put(Main.Literals.access_token.name(), application.getAccessToken());
        data.put(Main.Literals.description.name(), String.format("%s uploaded by JediBot.", file.getAbsolutePath()));
        data.put(Main.Literals.file.name(), Paths.get(file.getAbsolutePath()));
        String boundary = new BigInteger(256, new Random()).toString();
        HttpRequest request = null;
        HttpResponse<String> response = null;
        try {
            request = HttpRequest.newBuilder()
                    .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                    .POST(ofMimeMultipartData(data, boundary))
                    .uri(URI.create(urlString))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        int responseStatusCode = response.statusCode();
        if (responseStatusCode == 413) {
            System.out.format("File %s is too large. Size is %s.\n", file.getAbsolutePath(), Utils.humanReadableByteCount(file.length()));
            return null;
        }
        JsonElement jsonElement = JsonParser.parseString(response.body());
        return jsonElement.getAsJsonObject();
    }

    public static URI getUri(String urlString) {
        URI uri = null;
        try {
            uri = new URL(urlString).toURI();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    public static String getProperty(JsonElement jsonElement, String propertyName) {
        if (jsonElement == null) {
            return "";
        }
        JsonElement property = jsonElement.getAsJsonObject().get(propertyName);
        if (property != null && !property.isJsonNull()) {
            return property.getAsString();
        }
        return "";
    }

    public static void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        String fileName = "/etc/social.pla.jedibot.properties";
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(fileName));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.format("Properties file not found: %s.\n", fileName);
            return null;
        }
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        String url = String.format("jdbc:derby:%s", properties.getProperty("databaseName"));
        return DriverManager.getConnection(url, properties);
    }

    public static void close(Object... objects) {
        for (Object object : objects) {
            if (object != null) {
                try {
                    boolean closed = false;
                    if (object instanceof RandomAccessFile) {
                        RandomAccessFile randomAccessFile = (RandomAccessFile) object;
                        randomAccessFile.close();
                        closed = true;
                    }
                    if (object instanceof AudioInputStream) {
                        AudioInputStream audioInputStream = (AudioInputStream) object;
                        audioInputStream.close();
                        closed = true;
                    }
                    if (object instanceof Clip) {
                        Clip clip = (Clip) object;
                        clip.close();
                        closed = true;
                    }
                    if (object instanceof FileChannel) {
                        FileChannel fileChannel = (FileChannel) object;
                        fileChannel.close();
                        closed = true;
                    }
                    if (object instanceof java.io.BufferedOutputStream) {
                        BufferedOutputStream bufferedOutputStream = (BufferedOutputStream) object;
                        bufferedOutputStream.close();
                        closed = true;
                    }
                    if (object instanceof java.io.StringWriter) {
                        StringWriter stringWriter = (StringWriter) object;
                        stringWriter.close();
                        closed = true;
                    }
                    if (object instanceof java.sql.Statement) {
                        Statement statement = (Statement) object;
                        statement.close();
                        closed = true;
                    }
                    if (object instanceof java.io.FileReader) {
                        FileReader fileReader = (FileReader) object;
                        fileReader.close();
                        closed = true;
                    }
                    if (object instanceof java.sql.ResultSet) {
                        ResultSet rs = (ResultSet) object;
                        rs.close();
                        closed = true;
                    }
                    if (object instanceof java.sql.PreparedStatement) {
                        PreparedStatement ps = (PreparedStatement) object;
                        ps.close();
                        closed = true;
                    }
                    if (object instanceof java.sql.Connection) {
                        Connection connection = (Connection) object;
                        connection.close();
                        closed = true;
                    }
                    if (object instanceof java.io.BufferedReader) {
                        BufferedReader br = (BufferedReader) object;
                        br.close();
                        closed = true;
                    }
                    if (object instanceof Socket) {
                        Socket socket = (Socket) object;
                        socket.close();
                        closed = true;
                    }
                    if (object instanceof PrintStream) {
                        PrintStream printStream = (PrintStream) object;
                        printStream.close();
                        closed = true;
                    }
                    if (object instanceof ServerSocket) {
                        ServerSocket serverSocket = (ServerSocket) object;
                        serverSocket.close();
                        closed = true;
                    }
                    if (object instanceof Scanner) {
                        Scanner scanner = (Scanner) object;
                        scanner.close();
                        closed = true;
                    }
                    if (object instanceof InputStream) {
                        InputStream inputStream = (InputStream) object;
                        inputStream.close();
                        closed = true;
                    }
                    if (object instanceof OutputStream) {
                        OutputStream outputStream = (OutputStream) object;
                        outputStream.close();
                        closed = true;
                    }
                    if (object instanceof Socket) {
                        Socket socket = (Socket) object;
                        socket.close();
                        closed = true;
                    }
                    if (object instanceof PrintWriter) {
                        PrintWriter pw = (PrintWriter) object;
                        pw.close();
                        closed = true;
                    }
                    if (!closed) {
                        System.out.format("Object not closed. Object type not defined in this close method. Name: %s Stack: %s\n", object.getClass().getName(), getClassNames());
                    }
                } catch (Exception e) {
                    System.out.println(e.getLocalizedMessage());
                }
            }
        }
    }

    public static long getLong(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static boolean isBlank(String s) {
        return (s == null || s.trim().length() == 0);
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static boolean isJsonObject(JsonElement jsonElement) {
        if (jsonElement == null) {
            return false;
        }
        try {
            jsonElement.getAsJsonObject();
            return true;
        } catch (java.lang.IllegalStateException e) {
            return false;
        }
    }

    public static String urlEncodeComponent(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }


    public static boolean write(String outputFileName, String text) {
        File file = new File(outputFileName);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            FileChannel fileChannel = fileOutputStream.getChannel();
            FileLock fileLock = fileChannel.lock();
            fileOutputStream.write(text.getBytes());
            fileOutputStream.flush();
            fileLock.release();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            close(fileOutputStream);
        }
    }

    public static boolean write20200121(String outputFileName, String text) {
        RandomAccessFile outputFile = null;
        FileChannel fileChannel = null;
        try {
            outputFile = new RandomAccessFile(outputFileName, "rw");
            fileChannel = outputFile.getChannel();
            FileLock fileLock = fileChannel.lock();
            System.out.format("%s lock valid: %s lock shared: %s\n", outputFileName, fileLock.isValid(), fileLock.isShared());
            outputFile.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(fileChannel, outputFile);
        }
        return false;
    }

    public static String getClassNames() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StringBuilder classNames = new StringBuilder();
        for (StackTraceElement e : stackTraceElements) {
            classNames.append(e.getClassName()).append(", ");
        }
        if (classNames.toString().endsWith(", ")) {
            classNames.delete(classNames.length() - 2, classNames.length());
        }
        return classNames.toString();
    }


    public static String ping(String ipAddress) {
        int timeoutSeconds = 3;
        String command = String.format("ping -c 1 -W %d %s", timeoutSeconds, ipAddress);
        BufferedReader bufferedReader = null;
        StringBuilder output = new StringBuilder();
        boolean success = false;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command);
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.indexOf("received, 0% packet loss") != -1) {
                    success = true;
                }
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            output.append(e.getLocalizedMessage());
        } finally {
            close(bufferedReader);
        }
        if (success) {
            output.insert(0, String.format("%s ", Utils.SYMBOL_THUMBSUP));
        } else {
            output.insert(0, String.format("%s ", Utils.SYMBOL_THUMBSDOWN));
        }
        return output.toString();
    }

    public static String toString(Object object) {
        Class thisClass = object.getClass();
        Method[] methods = thisClass.getMethods();
        ArrayList<Pair> arrayList = new ArrayList<>();
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            if (methodName.startsWith("get") || methodName.startsWith("is")) {
                String label = methodName;
                String value = null;
                try {
                    value = methods[i].invoke(object).toString();
                } catch (Exception e) {
                }
                arrayList.add(new Pair(label, value));
            }
        }
        Collections.sort(arrayList);
        StringBuilder sb = new StringBuilder(256);
        sb.append(thisClass.getName());
        sb.append("\n");
        for (Pair pair : arrayList) {
            sb.append(pair.getName());
            sb.append(" = ");
            sb.append(pair.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

}
