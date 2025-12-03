package com.eb.script.ftp;

import org.apache.commons.net.ftp.FTPClient;

/**
 * FTP connection context that holds the FTP client and connection metadata.
 *
 * @author Earl Bosch
 */
public final class FtpContext {

    public final String handle;
    public final String host;
    public final int port;
    public final String username;
    public final FTPClient client;
    public final long connectedMs;

    public FtpContext(String handle, String host, int port, String username, FTPClient client) {
        this.handle = handle;
        this.host = host;
        this.port = port;
        this.username = username;
        this.client = client;
        this.connectedMs = System.currentTimeMillis();
    }

    /**
     * Check if the FTP client is connected.
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Disconnect the FTP client.
     */
    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.logout();
            } catch (Exception ignore) {
            }
            try {
                client.disconnect();
            } catch (Exception ignore) {
            }
        }
    }
}
