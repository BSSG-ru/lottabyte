package ru.bssg.lottabyte.coreapi.repository.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.config.ApplicationConfig;
import ru.bssg.lottabyte.coreapi.repository.IEntitySampleBodyRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class EntitySampleBodyS3RepositoryImpl implements IEntitySampleBodyRepository {
    private final AmazonS3 amazonS3;
    private final ApplicationConfig applicationConfig;

    @Autowired
    public EntitySampleBodyS3RepositoryImpl(AmazonS3 amazonS3, ApplicationConfig applicationConfig) {
        this.amazonS3 = amazonS3;
        this.applicationConfig = applicationConfig;
    }

    @Override
    public void createEntitySampleBody(String customAttributeDefElementId, String sampleBody, UserDetails userDetails) throws LottabyteException {



        String path = applicationConfig.getBucketName(); //String.format("%s/%s", applicationConfig.getBucketName(), customAttributeDefElementId);
        String fileName = String.format("%s/%s", customAttributeDefElementId, customAttributeDefElementId);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        metadata.put("Content-Length", "" + sampleBody.getBytes(StandardCharsets.UTF_8).length);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        Optional.of(metadata).ifPresent(map -> {
            map.forEach(objectMetadata::addUserMetadata);
        });

        try {
            InputStream stream = new ByteArrayInputStream(sampleBody.getBytes(StandardCharsets.UTF_8));
            amazonS3.putObject(path, fileName, stream, objectMetadata);
        } catch (AmazonServiceException e) {
            throw new LottabyteException(Message.LBE00036, userDetails.getLanguage(), e);
        }
    }
    @Override
    public String getEntitySampleBodyById(String entityId, UserDetails userDetails) throws LottabyteException {
        try {
            //S3Object object = amazonS3.getObject(String.format("%s/%s", applicationConfig.getBucketName(), entityId), entityId);
            S3Object object = amazonS3.getObject(String.format("%s", applicationConfig.getBucketName()), entityId + "/" + entityId);
            S3ObjectInputStream objectContent = object.getObjectContent();
            return new String(IOUtils.toByteArray(objectContent), StandardCharsets.UTF_8);
        } catch (AmazonServiceException | IOException e) {
            throw new LottabyteException(Message.LBE00035, userDetails.getLanguage(), e);
        }
    }

    @Override
    public void deleteEntitySampleBodyById(String sampleBodyId, UserDetails userDetails) throws LottabyteException {
        try {
            amazonS3.deleteObject(String.format("%s/%s", applicationConfig.getExternalBucketName(), applicationConfig.getBucketName()), String.format("%s/%s", sampleBodyId, sampleBodyId));
            //amazonS3.deleteObject(String.format("%s/%s", applicationConfig.getBucketName(), sampleBodyId), sampleBodyId);
        } catch (AmazonServiceException e) {
            if (!e.getErrorCode().equalsIgnoreCase("NoSuchKey") && !e.getErrorCode().equalsIgnoreCase("AccessDenied"))
                throw new LottabyteException(Message.LBE00068, userDetails.getLanguage(), e.getErrorCode(), e);
        }
    }
}
