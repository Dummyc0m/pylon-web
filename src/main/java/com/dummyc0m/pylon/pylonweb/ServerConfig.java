package com.dummyc0m.pylon.pylonweb;

/**
 * Created by Dummyc0m on 4/3/16.
 */
public class ServerConfig {
    private String instanceName;
    private int port = 8083;
    private String url = "jdbcURL";
    private String user = "USER";
    private String passward = "PASS";

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassward() {
        return passward;
    }

    public void setPassward(String passward) {
        this.passward = passward;
    }
}
