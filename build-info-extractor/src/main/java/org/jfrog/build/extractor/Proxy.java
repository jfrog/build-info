package org.jfrog.build.extractor;

public class Proxy {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean https;

    public Proxy(String httpHost, int httpPort, String httpUsername, String httpPassword, boolean https) {
        this.host = httpHost;
        this.port = httpPort;
        this.username = httpUsername;
        this.password = httpPassword;
        this.https = https;
    }

    public boolean isHttps() {
        return https;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
