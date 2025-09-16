package com.amfk.starfish.sync.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Configuration
public class RestClientConfig {
    
    @Value("${rest.client.connection.timeout:30000}")
    private int connectionTimeout;
    
    @Value("${rest.client.read.timeout:300000}")
    private int readTimeout;
    
    @Value("${rest.client.max.connections:100}")
    private int maxConnections;
    
    @Value("${rest.client.truststore.path:}")
    private String truststorePath;
    
    @Value("${rest.client.truststore.password:}")
    private String truststorePassword;
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient());
        return new RestTemplate(factory);
    }
    
    private CloseableHttpClient httpClient() {
        // Create socket factory registry
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSystemSocketFactory())
                .build();
        
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections / 2);
        
        // Configure connection pool for large responses
        connectionManager.setValidateAfterInactivity(org.apache.hc.core5.util.Timeout.ofMilliseconds(5000));
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setCircularRedirectsAllowed(true)
                .setMaxRedirects(3)
                .build();
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent("AMFK-Starfish-Sync/1.0")
                .disableContentCompression() // Disable compression to avoid truncation issues
                .build();
        
        // Configure SSL if truststore is provided
        if (truststorePath != null && !truststorePath.isEmpty()) {
            configureSSL(httpClient);
        }
        
        return httpClient;
    }
    
    private void configureSSL(CloseableHttpClient httpClient) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream fis = new FileInputStream(truststorePath)) {
                trustStore.load(fis, truststorePassword.toCharArray());
            }
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            
            // Note: This is a simplified SSL configuration
            // In production, you might want to use more sophisticated SSL configuration
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            throw new RuntimeException("Failed to configure SSL truststore", e);
        }
    }
} 