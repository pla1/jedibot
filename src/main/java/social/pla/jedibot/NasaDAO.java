package social.pla.jedibot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class NasaDAO {
    private final String URL_FEED = "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss";

    public static void main(String[] args) throws Exception {
        NasaDAO dao = new NasaDAO();
        JsonObject jsonObject = dao.getLatestImageOfTheDay();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.format("%s\n", gson.toJson(jsonObject));
    }

    public JsonObject getLatestImageOfTheDay()  {
        JsonObject jsonObject = new JsonObject();
        try {
            Document document = Jsoup.connect(URL_FEED)
                    .parser(Parser.xmlParser())
                    .timeout(1000 * 3)
                    .get();
            for (Element e : document.getElementsByTag("item")) {
                for (Element child : e.getAllElements()) {
                    String nodeName = child.nodeName();
                    String text = child.text();
                    if (Main.Literals.title.name().equals(nodeName)
                            || Main.Literals.link.name().equals(nodeName)
                            || Main.Literals.description.name().equals(nodeName)
                            || Main.Literals.pubDate.name().equals(nodeName)) {
                        jsonObject.addProperty(nodeName, text);
                    }
                    if (Main.Literals.enclosure.name().equals(nodeName)) {
                        String photoUrl = child.attr("url");
                        jsonObject.addProperty(Main.Literals.photoUrl.name(), photoUrl);
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
