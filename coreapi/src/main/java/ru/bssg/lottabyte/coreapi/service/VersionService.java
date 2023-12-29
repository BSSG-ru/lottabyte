package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.AdminRepository;
import ru.bssg.lottabyte.coreapi.repository.VersionRepository;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VersionService {
    private final VersionRepository versionRepository;

    public String getDDLByVersion(Integer version) {
        return versionRepository.getDDLByVersion(version);
    }
    public String getSchemaByVersion(Integer version) {
        return versionRepository.getSchemaByVersion(version);
    }
    public Integer getLastVersionByType(String type) {
        return versionRepository.getLastVersionByType(type);
    }
}
