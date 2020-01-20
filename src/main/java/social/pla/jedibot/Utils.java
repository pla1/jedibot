package social.pla.jedibot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class Utils {
    public static final String SYMBOL_SPEAKER = "\uD83D\uDD0A";
    public static final String SYMBOL_PEACE = "\u262E";
    public static final String SYMBOL_THINKING = "\uD83E\uDD14";
    public static final String SYMBOL_THUMBSUP = "\uD83D\uDC4D";
    public static final String SYMBOL_THUMBSDOWN = "\uD83D\uDC4E";
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.out.format("%s\n", Utils.SYMBOL_THINKING);
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

    public static JsonObject getSettings() {
        String settingsFileName = getSettingsFileName();
        FileInputStream fis = null;
        File propertyFile = new File(settingsFileName);
        if (!propertyFile.exists()) {
            return null;
        }
        try {
            fis = new FileInputStream(propertyFile);
            InputStreamReader isr = new InputStreamReader(fis);
            return gson.fromJson(isr, JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.close(fis);
        }
        return null;
    }

    public static void close(Object... objects) {
        for (Object object : objects) {
            if (object != null) {
                try {
                    boolean closed = false;
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

    public static void write(String outputFileName, String text) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(outputFileName, StandardCharsets.UTF_8);
            pw.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(pw);
        }
        close(pw);
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

    public static String getSettingsFileName() {
        return String.format("%s%s.jedibot.json", System.getProperty("user.home"), File.separator);
    }

    public static String readFileToString(String fileName) {
        StringBuilder sb = new StringBuilder();
        File file = new File(fileName);
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line = bufferedReader.readLine();
            String lineSeparator = System.getProperty("line.separator");
            while (line != null) {
                sb.append(line);
                sb.append(lineSeparator);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bufferedReader);
        }
        return sb.toString();
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
}
