package kennw.andern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Загрузка списка систем из systems.xml. */
public final class SystemsRepository {

    private SystemsRepository() {
    }

    public static List<SapSystem> load(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // защита от XXE
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(file);

        List<SapSystem> result = new ArrayList<SapSystem>();
        NodeList nodes = doc.getElementsByTagName("system");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            result.add(new SapSystem(
                    e.getAttribute("sid"),
                    e.getAttribute("client"),
                    e.getAttribute("sysnr"),
                    e.getAttribute("host")));
        }
        return result;
    }
}
