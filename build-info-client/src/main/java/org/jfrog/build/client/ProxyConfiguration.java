package org.jfrog.build.client;

import java.io.Serializable;

/**
 * Holds proxy configuration data.
 *
 * @author Yossi Shaul
 */
public class ProxyConfiguration implements Serializable {
    public String host;
    public int port;
    public String username;
    public String password;
    public String noProxyDomain;
    public boolean https;
}
