package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecretsLoader {

    private static final String SECRETS_FILE_PATH = "src/main/java/org/example/secrets.properties";
    private final Properties properties;

     SecretsLoader() {
        properties = new Properties();
        loadProperties();
     }

    private void loadProperties() {
        try (InputStream input = new FileInputStream(SECRETS_FILE_PATH)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load secrets.properties", e);
        }
    }

    public String getSecretKey() {
        return properties.getProperty("secretKey");
    }

    public String getAccessKey() {
        return properties.getProperty("accessKey");
    }

    public String getAccessPassphrase() {
        return properties.getProperty("accessPassphrase");
    }
}
