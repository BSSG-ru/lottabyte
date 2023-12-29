package ru.bssg.lottabyte.coreapi.exception;


import lombok.extern.slf4j.Slf4j;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Slf4j
public class SimpleErrorHandler implements ErrorHandler {
    public void warning(SAXParseException e) throws SAXException {
        log.error(e.getMessage());
    }

    public void error(SAXParseException e) throws SAXException {
        log.error(e.getMessage());
    }

    public void fatalError(SAXParseException e) throws SAXException {
        log.error(e.getMessage());
    }
}