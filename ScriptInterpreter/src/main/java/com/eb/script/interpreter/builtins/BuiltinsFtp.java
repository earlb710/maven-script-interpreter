package com.eb.script.interpreter.builtins;

import com.eb.script.ftp.FtpContext;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;
import com.eb.util.Util;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in functions for FTP operations.
 * Handles all ftp.* builtins.
 * 
 * Provides standard FTP commands:
 * - ftp.connect(host, port?, username?, password?) - Connect to FTP server
 * - ftp.disconnect(handle) - Disconnect from FTP server
 * - ftp.listFiles(handle, path?) - List files in directory
 * - ftp.upload(handle, localPath, remotePath) - Upload file to FTP server
 * - ftp.download(handle, remotePath, localPath) - Download file from FTP server
 * - ftp.delete(handle, remotePath) - Delete file on FTP server
 * - ftp.mkdir(handle, remotePath) - Create directory on FTP server
 * - ftp.rmdir(handle, remotePath) - Remove directory on FTP server
 * - ftp.rename(handle, fromPath, toPath) - Rename file on FTP server
 * - ftp.pwd(handle) - Get current working directory
 * - ftp.cd(handle, path) - Change current working directory
 * - ftp.exists(handle, path) - Check if file/directory exists
 * - ftp.size(handle, path) - Get file size
 * - ftp.binary(handle) - Set binary transfer mode
 * - ftp.ascii(handle) - Set ASCII transfer mode
 * - ftp.passive(handle) - Set passive mode
 * - ftp.active(handle) - Set active mode
 * - ftp.isConnected(handle) - Check if connected
 * - ftp.listConnections() - List all open connections
 *
 * @author Earl Bosch
 */
public class BuiltinsFtp {

    // Registry of open FTP connections
    private static final ConcurrentHashMap<String, FtpContext> ftpConnections = new ConcurrentHashMap<>();

    /**
     * Dispatch an FTP builtin by name.
     *
     * @param env Environment for file path resolution
     * @param name Lowercase builtin name (e.g., "ftp.connect")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "ftp.connect" -> connect(args);
            case "ftp.disconnect" -> disconnect(args);
            case "ftp.listfiles" -> listFiles(args);
            case "ftp.upload" -> upload(env, args);
            case "ftp.download" -> download(env, args);
            case "ftp.delete" -> delete(args);
            case "ftp.mkdir" -> mkdir(args);
            case "ftp.rmdir" -> rmdir(args);
            case "ftp.rename" -> rename(args);
            case "ftp.pwd" -> pwd(args);
            case "ftp.cd" -> cd(args);
            case "ftp.exists" -> exists(args);
            case "ftp.size" -> size(args);
            case "ftp.binary" -> binary(args);
            case "ftp.ascii" -> ascii(args);
            case "ftp.passive" -> passive(args);
            case "ftp.active" -> active(args);
            case "ftp.isconnected" -> isConnected(args);
            case "ftp.listconnections" -> listConnections();
            default -> throw new InterpreterError("Unknown FTP builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is an FTP builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("ftp.");
    }

    /**
     * Get an FTP context by handle.
     */
    private static FtpContext getContext(String handle) throws InterpreterError {
        if (handle == null || handle.isBlank()) {
            throw new InterpreterError("FTP handle cannot be null or empty");
        }
        FtpContext ctx = ftpConnections.get(handle);
        if (ctx == null) {
            throw new InterpreterError("FTP connection not found: " + handle);
        }
        if (!ctx.isConnected()) {
            throw new InterpreterError("FTP connection is not connected: " + handle);
        }
        return ctx;
    }

    /**
     * Close all FTP connections (cleanup on shutdown).
     */
    public static void closeAllConnections() {
        for (FtpContext ctx : ftpConnections.values()) {
            ctx.disconnect();
        }
        ftpConnections.clear();
    }

    // --- Individual builtin implementations ---

    /**
     * ftp.connect(host, port?, username?, password?) -> STRING (handle)
     * Connect to an FTP server. Returns a handle for subsequent operations.
     * 
     * @param args[0] host - FTP server hostname (required)
     * @param args[1] port - FTP server port (optional, default 21)
     * @param args[2] username - Username (optional, default "anonymous")
     * @param args[3] password - Password (optional, default "")
     */
    private static String connect(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.connect: host is required");
        }
        
        String host = (String) args[0];
        int port = 21;
        String username = "anonymous";
        String password = "";
        
        if (args.length > 1 && args[1] != null) {
            if (args[1] instanceof Number n) {
                port = n.intValue();
            } else {
                port = Integer.parseInt(args[1].toString());
            }
        }
        if (args.length > 2 && args[2] != null) {
            username = (String) args[2];
        }
        if (args.length > 3 && args[3] != null) {
            password = (String) args[3];
        }
        
        FTPClient client = new FTPClient();
        try {
            // Set timeouts
            client.setConnectTimeout(30000); // 30 seconds
            client.setDataTimeout(java.time.Duration.ofSeconds(60)); // 60 seconds for data transfer
            client.setDefaultTimeout(30000); // 30 seconds default
            
            // Connect
            client.connect(host, port);
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                throw new InterpreterError("ftp.connect: Connection refused by server. Reply code: " + reply);
            }
            
            // Login
            if (!client.login(username, password)) {
                client.logout();
                client.disconnect();
                throw new InterpreterError("ftp.connect: Login failed for user: " + username);
            }
            
            // Set default mode to passive and binary
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            
            // Create handle and register connection
            String handle = "ftp-" + UUID.randomUUID().toString();
            FtpContext ctx = new FtpContext(handle, host, port, username, client);
            ftpConnections.put(handle, ctx);
            
            return handle;
            
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (Exception ignore) {
            }
            throw new InterpreterError("ftp.connect: " + ex.getMessage());
        }
    }

    /**
     * ftp.disconnect(handle) -> BOOL
     * Disconnect from an FTP server.
     */
    private static Boolean disconnect(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.disconnect: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = ftpConnections.remove(handle);
        if (ctx == null) {
            return false;
        }
        
        ctx.disconnect();
        return true;
    }

    /**
     * ftp.listFiles(handle, path?) -> JSON (array of file info)
     * List files in the specified directory.
     */
    private static List<Map<String, Object>> listFiles(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.listFiles: handle is required");
        }
        
        String handle = (String) args[0];
        String path = args.length > 1 && args[1] != null ? (String) args[1] : ".";
        
        FtpContext ctx = getContext(handle);
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            FTPFile[] files = ctx.client.listFiles(path);
            if (files == null) {
                return result;
            }
            
            for (FTPFile file : files) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", file.getName());
                info.put("isDir", file.isDirectory());
                info.put("isFile", file.isFile());
                info.put("isLink", file.isSymbolicLink());
                info.put("size", file.getSize());
                info.put("modified", file.getTimestamp() != null ? 
                    Util.formatDate(java.nio.file.attribute.FileTime.from(file.getTimestamp().toInstant())) : null);
                info.put("modifiedMs", file.getTimestamp() != null ? 
                    file.getTimestamp().getTimeInMillis() : 0L);
                info.put("user", file.getUser());
                info.put("group", file.getGroup());
                info.put("permissions", file.getRawListing());
                result.add(info);
            }
            
            return result;
            
        } catch (Exception ex) {
            throw new InterpreterError("ftp.listFiles: " + ex.getMessage());
        }
    }

    /**
     * ftp.upload(handle, localPath, remotePath) -> BOOL
     * Upload a local file to the FTP server.
     */
    private static Boolean upload(Environment env, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("ftp.upload: handle, localPath, and remotePath are required");
        }
        if (args[0] == null || args[1] == null || args[2] == null) {
            throw new InterpreterError("ftp.upload: handle, localPath, and remotePath cannot be null");
        }
        
        String handle = (String) args[0];
        String localPath = (String) args[1];
        String remotePath = (String) args[2];
        
        FtpContext ctx = getContext(handle);
        
        try {
            Path local = Util.resolveSandboxedPath(localPath);
            if (!Files.exists(local)) {
                throw new InterpreterError("ftp.upload: Local file not found: " + localPath);
            }
            
            try (FileInputStream fis = new FileInputStream(local.toFile())) {
                boolean success = ctx.client.storeFile(remotePath, fis);
                if (!success) {
                    throw new InterpreterError("ftp.upload: Failed to upload file. Reply: " + ctx.client.getReplyString());
                }
                return true;
            }
            
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.upload: " + ex.getMessage());
        }
    }

    /**
     * ftp.download(handle, remotePath, localPath) -> BOOL
     * Download a file from the FTP server to a local path.
     */
    private static Boolean download(Environment env, Object[] args) throws InterpreterError {
        if (args.length < 3) {
            throw new InterpreterError("ftp.download: handle, remotePath, and localPath are required");
        }
        if (args[0] == null || args[1] == null || args[2] == null) {
            throw new InterpreterError("ftp.download: handle, remotePath, and localPath cannot be null");
        }
        
        String handle = (String) args[0];
        String remotePath = (String) args[1];
        String localPath = (String) args[2];
        
        FtpContext ctx = getContext(handle);
        
        try {
            Path local = Util.resolveSandboxedPath(localPath);
            
            // Create parent directories if needed
            Path parent = local.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            
            try (FileOutputStream fos = new FileOutputStream(local.toFile())) {
                boolean success = ctx.client.retrieveFile(remotePath, fos);
                if (!success) {
                    throw new InterpreterError("ftp.download: Failed to download file. Reply: " + ctx.client.getReplyString());
                }
                return true;
            }
            
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.download: " + ex.getMessage());
        }
    }

    /**
     * ftp.delete(handle, remotePath) -> BOOL
     * Delete a file on the FTP server.
     */
    private static Boolean delete(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.delete: handle and remotePath are required");
        }
        
        String handle = (String) args[0];
        String remotePath = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            boolean success = ctx.client.deleteFile(remotePath);
            if (!success) {
                throw new InterpreterError("ftp.delete: Failed to delete file. Reply: " + ctx.client.getReplyString());
            }
            return true;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.delete: " + ex.getMessage());
        }
    }

    /**
     * ftp.mkdir(handle, remotePath) -> BOOL
     * Create a directory on the FTP server.
     */
    private static Boolean mkdir(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.mkdir: handle and remotePath are required");
        }
        
        String handle = (String) args[0];
        String remotePath = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            boolean success = ctx.client.makeDirectory(remotePath);
            if (!success) {
                throw new InterpreterError("ftp.mkdir: Failed to create directory. Reply: " + ctx.client.getReplyString());
            }
            return true;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.mkdir: " + ex.getMessage());
        }
    }

    /**
     * ftp.rmdir(handle, remotePath) -> BOOL
     * Remove a directory on the FTP server.
     */
    private static Boolean rmdir(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.rmdir: handle and remotePath are required");
        }
        
        String handle = (String) args[0];
        String remotePath = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            boolean success = ctx.client.removeDirectory(remotePath);
            if (!success) {
                throw new InterpreterError("ftp.rmdir: Failed to remove directory. Reply: " + ctx.client.getReplyString());
            }
            return true;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.rmdir: " + ex.getMessage());
        }
    }

    /**
     * ftp.rename(handle, fromPath, toPath) -> BOOL
     * Rename a file on the FTP server.
     */
    private static Boolean rename(Object[] args) throws InterpreterError {
        if (args.length < 3 || args[0] == null || args[1] == null || args[2] == null) {
            throw new InterpreterError("ftp.rename: handle, fromPath, and toPath are required");
        }
        
        String handle = (String) args[0];
        String fromPath = (String) args[1];
        String toPath = (String) args[2];
        
        FtpContext ctx = getContext(handle);
        
        try {
            boolean success = ctx.client.rename(fromPath, toPath);
            if (!success) {
                throw new InterpreterError("ftp.rename: Failed to rename file. Reply: " + ctx.client.getReplyString());
            }
            return true;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.rename: " + ex.getMessage());
        }
    }

    /**
     * ftp.pwd(handle) -> STRING
     * Get the current working directory on the FTP server.
     */
    private static String pwd(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.pwd: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = getContext(handle);
        
        try {
            String pwd = ctx.client.printWorkingDirectory();
            if (pwd == null) {
                throw new InterpreterError("ftp.pwd: Failed to get working directory. Reply: " + ctx.client.getReplyString());
            }
            return pwd;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.pwd: " + ex.getMessage());
        }
    }

    /**
     * ftp.cd(handle, path) -> BOOL
     * Change the current working directory on the FTP server.
     */
    private static Boolean cd(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.cd: handle and path are required");
        }
        
        String handle = (String) args[0];
        String path = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            boolean success = ctx.client.changeWorkingDirectory(path);
            if (!success) {
                throw new InterpreterError("ftp.cd: Failed to change directory. Reply: " + ctx.client.getReplyString());
            }
            return true;
        } catch (InterpreterError ie) {
            throw ie;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.cd: " + ex.getMessage());
        }
    }

    /**
     * ftp.exists(handle, path) -> BOOL
     * Check if a file or directory exists on the FTP server.
     */
    private static Boolean exists(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.exists: handle and path are required");
        }
        
        String handle = (String) args[0];
        String path = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            // Try to get file info
            FTPFile[] files = ctx.client.listFiles(path);
            if (files != null && files.length > 0) {
                return true;
            }
            
            // Try as directory
            String savedDir = ctx.client.printWorkingDirectory();
            boolean exists = ctx.client.changeWorkingDirectory(path);
            if (exists && savedDir != null) {
                ctx.client.changeWorkingDirectory(savedDir);
            }
            return exists;
            
        } catch (Exception ex) {
            throw new InterpreterError("ftp.exists: " + ex.getMessage());
        }
    }

    /**
     * ftp.size(handle, path) -> LONG
     * Get the size of a file on the FTP server.
     */
    private static Long size(Object[] args) throws InterpreterError {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw new InterpreterError("ftp.size: handle and path are required");
        }
        
        String handle = (String) args[0];
        String path = (String) args[1];
        
        FtpContext ctx = getContext(handle);
        
        try {
            FTPFile[] files = ctx.client.listFiles(path);
            if (files != null && files.length > 0) {
                return files[0].getSize();
            }
            return null;
        } catch (Exception ex) {
            throw new InterpreterError("ftp.size: " + ex.getMessage());
        }
    }

    /**
     * ftp.binary(handle) -> BOOL
     * Set the transfer mode to binary.
     */
    private static Boolean binary(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.binary: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = getContext(handle);
        
        try {
            return ctx.client.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (Exception ex) {
            throw new InterpreterError("ftp.binary: " + ex.getMessage());
        }
    }

    /**
     * ftp.ascii(handle) -> BOOL
     * Set the transfer mode to ASCII.
     */
    private static Boolean ascii(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.ascii: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = getContext(handle);
        
        try {
            return ctx.client.setFileType(FTP.ASCII_FILE_TYPE);
        } catch (Exception ex) {
            throw new InterpreterError("ftp.ascii: " + ex.getMessage());
        }
    }

    /**
     * ftp.passive(handle) -> BOOL
     * Set passive mode.
     */
    private static Boolean passive(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.passive: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = getContext(handle);
        
        ctx.client.enterLocalPassiveMode();
        return true;
    }

    /**
     * ftp.active(handle) -> BOOL
     * Set active mode.
     */
    private static Boolean active(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            throw new InterpreterError("ftp.active: handle is required");
        }
        
        String handle = (String) args[0];
        FtpContext ctx = getContext(handle);
        
        ctx.client.enterLocalActiveMode();
        return true;
    }

    /**
     * ftp.isConnected(handle) -> BOOL
     * Check if the FTP connection is still active.
     */
    private static Boolean isConnected(Object[] args) throws InterpreterError {
        if (args.length < 1 || args[0] == null) {
            return false;
        }
        
        String handle = (String) args[0];
        FtpContext ctx = ftpConnections.get(handle);
        return ctx != null && ctx.isConnected();
    }

    /**
     * ftp.listConnections() -> JSON (array of connection info)
     * List all open FTP connections.
     */
    private static List<Map<String, Object>> listConnections() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (FtpContext ctx : ftpConnections.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("handle", ctx.handle);
            info.put("host", ctx.host);
            info.put("port", ctx.port);
            info.put("username", ctx.username);
            info.put("isConnected", ctx.isConnected());
            info.put("connectedMs", ctx.connectedMs);
            result.add(info);
        }
        
        return result;
    }
}
