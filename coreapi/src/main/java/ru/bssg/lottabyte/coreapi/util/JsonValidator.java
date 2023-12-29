package ru.bssg.lottabyte.coreapi.util;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonValidator implements IValidator {
    ObjectMapper mapper = new ObjectMapper();

    public boolean validate(String val) {
        try {
            mapper.readTree(val);
        } catch (JacksonException e) {
            return false;
        }
        return true;
    }
}
