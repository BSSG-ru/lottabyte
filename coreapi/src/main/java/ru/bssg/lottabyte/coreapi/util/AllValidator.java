package ru.bssg.lottabyte.coreapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.bssg.lottabyte.core.model.task.TaskEntity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Slf4j
@Component
public class AllValidator {
    public static boolean isXml(String xml) {
        boolean isXml=true;
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            isXml = false;
        }
        return isXml;
    }

    public static boolean dateValidator(TaskEntity taskEntity) {
        SimpleDateFormat sdf = null;
        try {
            final ObjectNode node = new ObjectMapper().readValue(taskEntity.getScheduleParams(), ObjectNode.class);
            switch(taskEntity.getScheduleType())
            {
                case ONCE:
                    if (node.has("datetime")) {
                        sdf = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS");
                        sdf.parse(String.valueOf(node.get("datetime")).replaceAll("\"", ""));
                        sdf.setLenient(false);
                        return true;
                    }
                    return false;
                case DAILY:
                    if (node.has("time")) {
                        sdf = new SimpleDateFormat("HH:MM:SS");
                        sdf.parse(String.valueOf(node.get("time")).replaceAll("\"", ""));
                        sdf.setLenient(false);
                        return true;
                    }
                    return false;
                case WEEKLY:
                    if (node.has("dow") && node.has("time")) {
                        sdf = new SimpleDateFormat("HH:MM:SS");
                        sdf.parse(String.valueOf(node.get("time")).replaceAll("\"", ""));
                        sdf.setLenient(false);
                        return true;
                    }
                    return false;
                case MONTHLY:
                    if (node.has("dom") && node.has("time")) {
                        sdf = new SimpleDateFormat("HH:MM:SS");
                        sdf.parse(String.valueOf(node.get("time")).replaceAll("\"", ""));
                        sdf.setLenient(false);
                        return true;
                    }
                    return false;
                case CRON:
                    if (node.has("cron_schedule")) {
                        return org.quartz.CronExpression.isValidExpression(String.valueOf(node.get("cron_schedule")).replaceAll("\"", ""));
                    }
                    return false;
                default:
                    return false;
            }
        } catch (ParseException | JsonProcessingException e) {
            log.error(e.getMessage());
            return false;
        }
    }
}
