package social.pla.jedibot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;

public class XkcdDAO {
    private final static String URL_FEED = "https://xkcd.com/atom.xml";

    public static void main(String[] args) {
        XkcdDAO dao = new XkcdDAO();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = dao.getLatest();
        System.out.format("%s\n", gson.toJson(jsonObject));
        System.exit(0);
    }

    public JsonObject getLatest() {
        JsonObject jsonObject = new JsonObject();
        try {
            Document document = Jsoup.connect(URL_FEED)
                    .parser(Parser.xmlParser())
                    .timeout(1000 * 3)
                    .get();
            for (Element e : document.getElementsByTag("entry")) {
                for (Element child : e.getAllElements()) {
                    String nodeName = child.nodeName();
                    String html = child.html();
                    html = html.replace("<![CDATA[", "");
                    html = html.replace("]]>", "");
                    String text = Jsoup.parse(html).text();
                    System.out.format("Name: %s value: %s\n", nodeName, text);
                    if (Main.Literals.title.name().equals(nodeName)
                            || Main.Literals.id.name().equals(nodeName)
                            || Main.Literals.updated.name().equals(nodeName)) {
                        jsonObject.addProperty(nodeName, text);
                    }
                    if (Main.Literals.link.name().equals(nodeName)) {
                        String link = child.attr("href");
                        jsonObject.addProperty(Main.Literals.link.name(), link);
                    }
                    if (Main.Literals.summary.name().equals(nodeName)) {
                        Document docSummary = Jsoup.parse(text);
                        Element imageElement = docSummary.select("img").first();
                        String url = imageElement.attr("src");
                        jsonObject.addProperty(Main.Literals.photoUrl.name(), url);
                        String description = imageElement.attr("title");
                        jsonObject.addProperty(Main.Literals.description.name(), description);
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
