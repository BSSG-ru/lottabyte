package ru.bssg.lottabyte.coreapi.service.connector;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;


import java.io.*;
import java.util.*;

@Service
@Slf4j
public class ExcelS3ConnectorServiceImpl implements IConnectorService {
    public AmazonS3 getConnection(String serviceEndpoint, String signingRegion, String publicKey, String secretKey) {
        AWSCredentials awsCredentials = new BasicAWSCredentials(publicKey, secretKey);
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    public EntityQueryResult querySystem(Connector connector,
                                         List<ConnectorParam> connectorParams,
                                         System system,
                                         DataEntity entity,
                                         EntityQuery entityQuery,
                                         SystemConnection systemConnection,
                                         List<SystemConnectionParam> systemConnectionParams,
                                         UserDetails userDetails
    ) throws LottabyteException {
        EntityQueryResult res = new EntityQueryResult();

        if (entityQuery.getEntity() == null || entityQuery.getEntity().getQueryText() == null || entityQuery.getEntity().getQueryText().isEmpty())
            throw new LottabyteException(Message.LBE00005, userDetails.getLanguage(), entityQuery.getId());

        Map<String, String> paramValues = new HashMap<>();

        for (SystemConnectionParam scp : systemConnectionParams) {
            if (scp.getEntity() == null)
                throw new LottabyteException(Message.LBE00006, userDetails.getLanguage(), scp.getId());

            Optional<ConnectorParam> cp = connectorParams.stream().filter(
                    x -> x.getId().equals(scp.getEntity().getConnectorParamId())
            ).findFirst();
            if (cp.isEmpty())
                throw new LottabyteException(Message.LBE00007, userDetails.getLanguage(), scp.getEntity().getConnectorParamId());
            paramValues.put(cp.get().getEntity().getName(), scp.getEntity().getParamValue());
        }

        if (!paramValues.containsKey("s3_endpoint_url"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "s3_endpoint_url");
        if (!paramValues.containsKey("s3_signing_region"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "s3_signing_region");
        if (!paramValues.containsKey("s3_access_key"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "s3_access_key");
        if (!paramValues.containsKey("s3_secret_access_key"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "s3_secret_access_key");
        if (!paramValues.containsKey("s3_file_path"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "s3_file_path");
        if (!paramValues.containsKey("excel_first_row_headers"))
            throw new LottabyteException(Message.LBE00008, userDetails.getLanguage(), "excel_first_row_headers");

        AmazonS3 conn = null;
        Workbook workbook = null;
        try{
            conn = getConnection(paramValues.get("s3_endpoint_url"), paramValues.get("s3_signing_region"), paramValues.get("s3_access_key"), paramValues.get("s3_secret_access_key"));
            String[] s3FilePathArray = paramValues.get("s3_file_path").split("/");
            byte[] body = getEntitySampleBodyByteArrayById(conn, paramValues.get("s3_bucket_name"), s3FilePathArray[0], s3FilePathArray[1], userDetails);

            InputStream inputStream = new ByteArrayInputStream(body);
            try {
                workbook = WorkbookFactory.create(inputStream);
            } catch (IOException e) {
                throw new LottabyteException(e.getMessage(), e);
            }finally{
                try {
                    Objects.requireNonNull(workbook).close();
                } catch (IOException e) {
                    throw new LottabyteException(e.getMessage(), e);
                }
            }
        }finally {
            Objects.requireNonNull(conn).shutdown();
        }

        res.setTextSampleBody(getJsonFromXls(workbook, entityQuery.getEntity().getQueryText(), paramValues.containsKey("excel_first_row_headers")));
        res.setSampleType(EntitySampleType.table);

        return res;
    }

    public byte[] getEntitySampleBodyByteArrayById(AmazonS3 conn, String bucketName, String folder, String fileName, UserDetails userDetails) throws LottabyteException {
        try {
            S3Object object = conn.getObject(String.format("%s/%s", bucketName, folder), fileName);
            S3ObjectInputStream objectContent = object.getObjectContent();
            return IOUtils.toByteArray(objectContent);
        } catch (AmazonServiceException | IOException e) {
            throw new LottabyteException(Message.LBE00035, userDetails.getLanguage(), e);
        }
    }
    public String getJsonFromXls(Workbook workbook, String xmlFormat, boolean excelFirstRowHeaders) throws LottabyteException {
        JSONObject jsonBody = new JSONObject();
        try{
            String[] val = xmlFormat.split("!");
            String sheetString = val[0];
            int firstRow = Integer.parseInt(val[1].substring(val[1].indexOf('!') + 1, val[1].indexOf(':')).substring(1)) - 1;
            String firstColumn = String.valueOf(val[1].substring(val[1].indexOf('!') + 1, val[1].indexOf(':')).charAt(0));
            int lastRow = Integer.parseInt(val[1].substring(val[1].indexOf(':') + 1).substring(1)) - 1;
            String lastColumn = String.valueOf(val[1].substring(val[1].indexOf(':') + 1).charAt(0));

            Sheet sheet = workbook.getSheet(sheetString);

            Iterator<Row> rowIterator = sheet.iterator();
            JSONArray records = new JSONArray();
            JSONArray fields = new JSONArray();
            jsonBody.put("records", records);
            jsonBody.put("fields", fields);
            while (rowIterator.hasNext())
            {
                Row row = rowIterator.next();
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                JSONArray notNamedRecords = new JSONArray();
                records.put(notNamedRecords);

                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();
                    CellAddress cellAddress = cell.getAddress();
                    if(excelFirstRowHeaders && cellAddress.getRow() == 0){
                        JSONObject namedField = new JSONObject();
                        if (Objects.requireNonNull(cell.getCellType()) == CellType.STRING) {
                            namedField.put("name", cell.getStringCellValue());
                            namedField.put("type", "TEXT");
                        }
                        fields.put(namedField);
                        continue;
                    }

                    if((cellAddress.getColumn() >= firstColumn.charAt(0) - 65 && cellAddress.getColumn() <= lastColumn.charAt(0) - 65) && (cellAddress.getRow() >= firstRow && cellAddress.getRow() <= lastRow)) {
                        switch (cell.getCellType()) {
                            case NUMERIC:
                                notNamedRecords.put(cell.getNumericCellValue());
                                break;
                            case STRING:
                                notNamedRecords.put(cell.getStringCellValue());
                                break;
                            case BOOLEAN:
                                notNamedRecords.put(cell.getBooleanCellValue());
                                break;
                        }
                    }
                }
                if(notNamedRecords.isEmpty())
                    records.remove(records.length() - 1);
            }
            workbook.close();
        }catch (Exception e){
            throw new LottabyteException(e.getMessage());
        }
        return String.valueOf(jsonBody);
    }
}
