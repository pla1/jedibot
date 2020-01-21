package social.pla.jedibot;

import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.IOException;

public class HprDAO {
    private final String URL_FEED_HPR = "http://hackerpublicradio.org/hpr_rss.php";

    public static void main(String[] args) throws Exception {
        HprDAO dao = new HprDAO();
        JsonObject jsonObject = dao.getLatestEpisode();
        System.out.format("%s\n", jsonObject);
    }

    public JsonObject getLatestEpisode() throws IOException {
        JsonObject jsonObject = new JsonObject();
        org.jsoup.nodes.Document document = Jsoup.connect(URL_FEED_HPR)
                .parser(Parser.xmlParser())
                .timeout(1000 * 3)
                .get();
        for (org.jsoup.nodes.Element e : document.getElementsByTag("item")) {
            for (org.jsoup.nodes.Element child : e.getAllElements()) {
                String nodeName = child.nodeName();
                String text = child.text();
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
        return jsonObject;
    }
}
