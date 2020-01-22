package social.pla.jedibot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.Date;

public class HprDAO {
    private final String URL_FEED = "http://hackerpublicradio.org/hpr_rss.php";

    public static void main(String[] args) throws Exception {
        HprDAO dao = new HprDAO();
        JsonObject jsonObject = dao.getLatestEpisode();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (int i = 0 ; i < 100;i++) {
            JsonObject settings = Utils.getSettings();
            settings.addProperty(Main.Literals.millisecondsUpdated.name(), System.currentTimeMillis());
            settings.add(Main.Literals.hprLatestEpisode.name(), jsonObject);
            Utils.writeSettings(settings);
            System.out.format("HPR Latest Episode:\n%s\nSettings before write:\n%s\nSettings after write:\n%s\n%s",
                    gson.toJson(jsonObject), gson.toJson(settings), gson.toJson(Utils.getSettings()), new Date());
        }
    }


    public JsonObject getLatestEpisode() {
        JsonObject jsonObject = new JsonObject();
        try {
            Document document = Jsoup.connect(URL_FEED)
                    .parser(Parser.xmlParser())
                    .timeout(1000 * 3)
                    .get();
            for (Element e : document.getElementsByTag("item")) {
                for (org.jsoup.nodes.Element child : e.getAllElements()) {
                    String nodeName = child.nodeName();
                    String html = child.html();
                    html = html.replace("<![CDATA[", "");
                    html = html.replace("]]>", "");
                    String text = Jsoup.parse(html).text();
                    if (Main.Literals.title.name().equals(nodeName)
                            || Main.Literals.link.name().equals(nodeName)
                            || Main.Literals.description.name().equals(nodeName)
                            || Main.Literals.pubDate.name().equals(nodeName)) {
                        jsonObject.addProperty(nodeName, text);
                    }
                    if (Main.Literals.enclosure.name().equals(nodeName)) {
                        String audioUrl = child.attr("url");
                        jsonObject.addProperty(Main.Literals.audioUrl.name(), audioUrl);
                    }
                }
                return jsonObject;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
