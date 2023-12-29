package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.repository.DriverRepository;

import java.io.*;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;
    private final ApplicationConfig applicationConfig;

    public void uploadDrivers(MultipartFile[] drivers, UserDetails userDetails) throws LottabyteException {
        for(MultipartFile driver : drivers) {
            if (!driver.isEmpty()) {
                try {
                    byte[] fileBytes = driver.getBytes();
                    String fileName = Objects.requireNonNull(driver.getOriginalFilename());

                    File newFile = new File(applicationConfig.getStoragePath() + "/" + fileName);
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(newFile));
                    stream.write(fileBytes);
                    stream.close();

                    driverRepository.insertDriver(fileName, userDetails);
                } catch (Exception e) {
                    throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage(), e);
                }
            } else {
                throw new LottabyteException(Message.LBE00041, userDetails.getLanguage());
            }
        }
    }
}