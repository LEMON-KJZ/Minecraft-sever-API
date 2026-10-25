package com.example.serverapi.http;

import com.example.serverapi.config.ServerApiConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public final class TlsContextFactory {
    private TlsContextFactory() {}

    public static SSLContext create(ServerApiConfig config) throws Exception {
        Path path = Path.of(config.keystorePath());
        String type = path.toString().toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12";
        KeyStore store = KeyStore.getInstance(type);
        char[] password = config.keystorePassword().toCharArray();
        try (InputStream input = Files.newInputStream(path)) { store.load(input, password); }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(store, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        return context;
    }
}
