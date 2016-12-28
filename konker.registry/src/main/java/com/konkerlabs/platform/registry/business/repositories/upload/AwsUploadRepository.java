package com.konkerlabs.platform.registry.business.repositories.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.Base64;
import com.amazonaws.util.IOUtils;
import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.business.services.api.UploadService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class AwsUploadRepository implements UploadRepository {

    @Autowired
    private S3Client client;

    @Autowired
    private S3Credentials credentials;

    private static Config config = ConfigFactory.load().getConfig("cdn");

    private AmazonS3 s3Client;

    public AwsUploadRepository() {
    }

    public AwsUploadRepository(Config config) {
        this.config = config;
    }

    @PostConstruct
    public void warmUp() {
        s3Client = client.getClient(credentials.getCredentials());
    }


    @Override
    public void validateFile(InputStream is, String type) throws BusinessException {
        validateFileType(type);
        try {
            validateFileSize(is.available());
        } catch (IOException e){
            throw new BusinessException(Validations.INVALID_FILE_SIZE.getCode());
        }

    }

    private void validateFileSize(Integer size) throws BusinessException {
        Integer maxSize = config.getInt("max-size");
        if (size > maxSize) {
            throw new BusinessException(Validations.INVALID_FILE_SIZE.getCode());
        }
    }

    private void validateFileType(String type) throws BusinessException {
        String fileTypes = config.getString("file-types");
        List<String> types = null;
        if (Optional.ofNullable(fileTypes).isPresent()) {
            if (fileTypes.contains(",")) {
                types = Arrays.asList(fileTypes.split(","));
            } else {
                types = Collections.emptyList();
                types.add(fileTypes);
            }
        }
        boolean approved = false;
        for (String item : types) {
            if (type.equalsIgnoreCase(item)) {
                approved = true;
                break;
            }
        }
        if(!approved){
            throw new BusinessException(Validations.INVALID_FILE_TYPE.getCode());
        }
    }

    @Override
    public void delete(String path) throws BusinessException {
        if (!Optional.ofNullable(path).isPresent()) {
            throw new BusinessException(Validations.INVALID_PATH.getCode());
        }
        client.getClient(credentials.getCredentials());
        try {
            s3Client.deleteObject(
                    new DeleteObjectRequest(config.getString("name"), path)
            );
        } catch (AmazonServiceException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public String downloadAsBase64(String filePath) throws Exception {
        if (!Optional.ofNullable(filePath).isPresent()) {
            throw new Exception(UploadService.Validations.INVALID_PATH.getCode());
        }
        try {
            return Base64.encodeAsString(IOUtils.toByteArray(downloadFile(filePath)));
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String filePath) throws BusinessException {
        if (!Optional.ofNullable(filePath).isPresent()) {
            throw new BusinessException(Validations.INVALID_PATH.getCode());
        }
        client.getClient(credentials.getCredentials());
        try {
            if (filePath.contains("/")) {
                filePath = filePath.split("/")[filePath.split("/").length - 1];
            }

            S3Object object = s3Client.getObject(
                    new GetObjectRequest(config.getString("name"), filePath));
            return object.getObjectContent();
        } catch (AmazonServiceException e) {
            throw new BusinessException(Validations.INVALID_S3_BUCKET_CREDENTIALS.getCode());
        }
    }

    @Override
    public String upload(String base64, String fileName, String sufix, boolean isPublic) throws Exception {
        InputStream is = new ByteArrayInputStream(Base64.decode(base64.getBytes()));
        return upload(is, fileName, sufix, isPublic);
    }

    public String upload(InputStream is, String fileName, String sufix, Boolean isPublic) throws Exception {
        validateFile(is, sufix);
        if (isPublic == null) {
            isPublic = Boolean.TRUE;
        }
        if (is != null && fileName != null) {

            try {
                byte[] bytes = IOUtils.toByteArray(is);
                s3Client.putObject(
                        new PutObjectRequest(
                                config.getString("name"),
                                fileName + "." + sufix,
                                new ByteArrayInputStream(bytes),
                                S3ObjectMetadata.getObjectMetadata(bytes)
                        ).withCannedAcl(isPublic ? CannedAccessControlList.PublicRead : CannedAccessControlList.AuthenticatedRead)
                );
                return fileName + "." + sufix;
            } catch (AmazonServiceException | IOException e) {
                throw new BusinessException(Validations.INVALID_S3_BUCKET_CREDENTIALS.getCode());
            } finally {
                is.close();
            }
        } else {
            throw new BusinessException(Validations.INVALID_PARAMETERS.getCode());
        }
    }
}

@Repository
class S3Client {

    public AmazonS3 getClient(BasicAWSCredentials credentialsProvider) {
        AmazonS3 client = new AmazonS3Client(credentialsProvider);
        return client;
    }
}

@Repository
class S3Credentials {

    private static String key;
    private static String secret;

    private Config config = ConfigFactory.load().getConfig("cdn");

    @PostConstruct
    public void init() {
        this.key = config.getString("key");
        this.secret = config.getString("secret");
    }

    public BasicAWSCredentials getCredentials() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(key, secret);
        return awsCredentials;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        S3Credentials.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        S3Credentials.secret = secret;
    }
}

@Repository
class S3ObjectMetadata {

    public ObjectMetadata getObjectMetadata(InputStream is) throws BusinessException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        return objectMetadata;
    }

    public static ObjectMetadata getObjectMetadata(byte[] is) throws BusinessException {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(Long.valueOf(is.length));
        return objectMetadata;
    }
}