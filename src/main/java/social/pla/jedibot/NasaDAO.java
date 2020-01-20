package social.pla.jedibot;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class NasaDAO {
    private final String URL_FEED_NASA_IOD = "https://www.nasa.gov/rss/dyn/lg_image_of_the_day.rss";
    private final String FILE_NAME_NASA_IOD = ".nasa_iod.xml";
    public static void main(String[] args) throws Exception {
        NasaDAO dao = new NasaDAO();
        dao.getStatusText();
    }

    public String getStatusText() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("test.xml");

        return null;
    }
}
