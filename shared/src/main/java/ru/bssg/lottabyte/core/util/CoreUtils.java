package ru.bssg.lottabyte.core.util;

import org.apache.commons.lang3.StringUtils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.URI;

public class CoreUtils {

    public static String getRelativeHref(URI uri) {
        return uri == null ? null : getRelativeHref(uri.toString());
    }

    public static String getRelativeHref(String href) {
        if (StringUtils.isBlank(href)) {
            return null;
        } else {
            String relativeHref = href;
            if (href.contains("v3/")) {
                relativeHref = "/" + href.substring(href.indexOf("v3/"));
            }

            return relativeHref;
        }
    }

    public static boolean validateXPath(String strXPath) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//book[author='Abc']/title/text()");
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
