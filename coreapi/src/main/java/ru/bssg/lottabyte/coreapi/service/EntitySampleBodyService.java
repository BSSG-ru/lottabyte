package ru.bssg.lottabyte.coreapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.IEntitySampleBodyRepository;
import ru.bssg.lottabyte.coreapi.repository.impl.EntitySampleBodyS3RepositoryImpl;
import ru.bssg.lottabyte.coreapi.util.Helper;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

@Service
@Slf4j
public class EntitySampleBodyService {
    private final IEntitySampleBodyRepository iEntitySampleBodyRepository;
    private final EntitySampleService entitySampleService;

    @Autowired
    @Lazy
    public EntitySampleBodyService(EntitySampleBodyS3RepositoryImpl entitySampleBodyS3RepositoryImpl,
                                   EntitySampleService entitySampleService) {
        this.iEntitySampleBodyRepository = entitySampleBodyS3RepositoryImpl;
        this.entitySampleService = entitySampleService;
    }

    public String getEntitySampleBodyFromS3ById(String entityId, UserDetails userDetails) throws LottabyteException {
        return iEntitySampleBodyRepository.getEntitySampleBodyById(entityId, userDetails);
    }

    public ArchiveResponse deleteEntitySampleBodyFromS3ById(String sampleBodyId, UserDetails userDetails) throws LottabyteException {
        iEntitySampleBodyRepository.deleteEntitySampleBodyById(sampleBodyId, userDetails);

        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(sampleBodyId));

        return archiveResponse;
    }

    public String getSamplePrettyBodyById(String sampleId, Integer lines, UserDetails userDetails) throws LottabyteException {
        String body = getEntitySampleBodyFromS3ById(sampleId, userDetails);
        EntitySampleType type;
        try {
            type = entitySampleService.getEntitySampleById(sampleId, true, userDetails).getEntity().getSampleType();
        } catch (NullPointerException e) {
            throw new LottabyteException(Message.LBE02001, userDetails.getLanguage(), sampleId);
        }

        JsonNode tree = null;
        ObjectMapper objectMapper = new ObjectMapper();
        switch (type) {
            case json:
                objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                try {
                    tree = objectMapper.readTree(body);
                    body = objectMapper.writeValueAsString(tree);
                } catch (JsonProcessingException e) {
                    throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage());
                }
                break;
            case xml:
                body = xmlPrettyFormat(body, 2);
                break;
            case csv:
            case text:
            case unknown:
                break;
            case table:
                objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                try {
                    tree = objectMapper .readTree(body);
                    return objectMapper.writeValueAsString(tree);
                } catch (JsonProcessingException e) {
                    throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage());
                }
            default:
                throw new LottabyteException(Message.LBE02005, userDetails.getLanguage(), type);
        }

        return Helper.stringClipping(body, lines - 1);
    }
    public static String xmlPrettyFormat(String input, int indent) throws LottabyteException {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
