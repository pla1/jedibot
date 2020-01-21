package social.pla.jedibot;

import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

public class NasaDAO {
    private final String URL_FEED_NASA_IOD = "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss";

    public static void main(String[] args) throws Exception {
        NasaDAO dao = new NasaDAO();
        JsonObject jsonObject = dao.getLatestImageOfTheDay();
        System.out.format("%s\n", jsonObject);
    }

    public JsonObject getLatestImageOfTheDay() throws IOException {
        JsonObject jsonObject = new JsonObject();
        org.jsoup.nodes.Document document = Jsoup.connect(URL_FEED_NASA_IOD)
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
                    String photoUrl = child.attr("url");
                    jsonObject.addProperty(Main.Literals.photoUrl.name(), photoUrl);
                }
            }
            return jsonObject;
        }
        return jsonObject;
    }

    public JsonObject getLatestImageOfTheDay20200120() throws IOException, SAXException, ParserConfigurationException {
        JsonObject jsonObject = new JsonObject();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new URL(URL_FEED_NASA_IOD).openConnection().getInputStream());
        NodeList nodeList = document.getElementsByTagName("item");
        System.out.format("%d\n", nodeList.getLength());
        int quantity = nodeList.getLength();
        if (quantity > 0) {
            if (nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nodeList.item(0);
                if (el.getNodeName().contains("item")) {
                    String title = el.getElementsByTagName(Main.Literals.title.name()).item(0).getTextContent();
                    jsonObject.addProperty(Main.Literals.title.name(), title);
                    String link = el.getElementsByTagName(Main.Literals.link.name()).item(0).getTextContent();
                    jsonObject.addProperty(Main.Literals.link.name(), link);
                    String description = el.getElementsByTagName(Main.Literals.description.name()).item(0).getTextContent();
                    jsonObject.addProperty(Main.Literals.description.name(), description);
                    String photoUrl = el.getElementsByTagName(Main.Literals.enclosure.name()).item(0).getAttributes().getNamedItem(Main.Literals.url.name()).getTextContent();
                    jsonObject.addProperty(Main.Literals.photoUrl.name(), photoUrl);
                }
            }
        }
        System.out.format("%s\n", jsonObject);
        return jsonObject;
    }
}
