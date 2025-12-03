package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.InterpreterError;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;

/**
 * Built-in functions for email operations.
 * Handles all mail.* builtins for connecting to mail servers,
 * listing emails, and retrieving email content.
 *
 * Supports IMAP and POP3 protocols with SSL/TLS options.
 * 
 * Note: When using SSL protocols (imaps, pop3s), the implementation
 * trusts all SSL certificates by default for compatibility. For
 * production environments requiring strict certificate validation,
 * consider implementing custom SSL configuration.
 *
 * @author Earl Bosch
 */
public class BuiltinsMail {

    /**
     * Registry of open mail connections (handle -> MailContext).
     */
    private static final Map<String, MailContext> MAIL_CONNECTIONS = new ConcurrentHashMap<>();

    /**
     * Holds the state of an open mail connection.
     */
    private static class MailContext {
        final String handle;
        final Store store;
        final Session session;
        final String protocol;
        final long openedMs;
        Folder currentFolder;

        MailContext(String handle, Store store, Session session, String protocol) {
            this.handle = handle;
            this.store = store;
            this.session = session;
            this.protocol = protocol;
            this.openedMs = System.currentTimeMillis();
        }

        boolean isConnected() {
            return store != null && store.isConnected();
        }

        void close() {
            try {
                if (currentFolder != null && currentFolder.isOpen()) {
                    currentFolder.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException ignore) {
                // Ignore close errors
            }
        }
    }

    /**
     * Dispatch a mail builtin by name.
     *
     * @param env  The environment context
     * @param name Lowercase builtin name (e.g., "mail.connect")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(Environment env, String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "mail.connect" -> connect(args);
            case "mail.list" -> list(args);
            case "mail.get" -> get(args);
            case "mail.close" -> close(args);
            case "mail.folders" -> folders(args);
            default -> throw new InterpreterError("Unknown mail builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a mail builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("mail.");
    }

    // --- Individual builtin implementations ---

    /**
     * mail.connect(host, port, user, password, protocol?) -> STRING (handle)
     *
     * Connects to a mail server and returns a handle for subsequent operations.
     *
     * @param args [0] host (String), [1] port (Integer), [2] user (String),
     *             [3] password (String), [4] protocol (String, optional: "imap", "imaps", "pop3", "pop3s")
     * @return String handle to identify this connection
     * @throws InterpreterError if connection fails
     */
    private static String connect(Object[] args) throws InterpreterError {
        if (args.length < 4) {
            throw new InterpreterError("mail.connect: requires host, port, user, password");
        }

        String host = (String) args[0];
        Number portNum = (Number) args[1];
        String user = (String) args[2];
        String password = (String) args[3];
        String protocol = args.length > 4 && args[4] != null ? (String) args[4] : "imaps";

        if (host == null || host.isBlank()) {
            throw new InterpreterError("mail.connect: host cannot be empty");
        }
        if (portNum == null) {
            throw new InterpreterError("mail.connect: port cannot be null");
        }
        if (user == null || user.isBlank()) {
            throw new InterpreterError("mail.connect: user cannot be empty");
        }
        if (password == null) {
            throw new InterpreterError("mail.connect: password cannot be null");
        }

        int port = portNum.intValue();
        protocol = protocol.toLowerCase();

        // Validate protocol
        if (!protocol.equals("imap") && !protocol.equals("imaps") &&
            !protocol.equals("pop3") && !protocol.equals("pop3s")) {
            throw new InterpreterError("mail.connect: unsupported protocol '" + protocol +
                "'. Supported: imap, imaps, pop3, pop3s");
        }

        try {
            Properties props = new Properties();

            // Common properties
            props.put("mail.store.protocol", protocol);

            // Protocol-specific properties
            boolean useSSL = protocol.endsWith("s");

            props.put("mail." + protocol + ".host", host);
            props.put("mail." + protocol + ".port", String.valueOf(port));

            if (useSSL) {
                props.put("mail." + protocol + ".ssl.enable", "true");
                // Trust all SSL certificates for compatibility with various mail servers.
                // Note: In production environments with strict security requirements,
                // consider implementing custom SSL certificate validation.
                props.put("mail." + protocol + ".ssl.trust", "*");
            }

            // Timeout settings (30 seconds)
            props.put("mail." + protocol + ".connectiontimeout", "30000");
            props.put("mail." + protocol + ".timeout", "30000");

            Session session = Session.getInstance(props);
            Store store = session.getStore(protocol);
            store.connect(host, port, user, password);

            String handle = "mail-" + UUID.randomUUID();
            MailContext ctx = new MailContext(handle, store, session, protocol);
            MAIL_CONNECTIONS.put(handle, ctx);

            return handle;

        } catch (MessagingException ex) {
            throw new InterpreterError("mail.connect: " + ex.getMessage());
        }
    }

    /**
     * mail.list(handle, folder?, start?, count?) -> JSON (array of message info)
     *
     * Lists emails in the specified folder.
     *
     * @param args [0] handle (String), [1] folder (String, optional, default "INBOX"),
     *             [2] start (Integer, optional, 1-based), [3] count (Integer, optional, default 50)
     * @return List of Maps containing message info (id, subject, from, date, size, read)
     * @throws InterpreterError if operation fails
     */
    private static List<Map<String, Object>> list(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("mail.list: requires handle");
        }

        String handle = (String) args[0];
        String folderName = args.length > 1 && args[1] != null ? (String) args[1] : "INBOX";
        int start = args.length > 2 && args[2] != null ? ((Number) args[2]).intValue() : 1;
        int count = args.length > 3 && args[3] != null ? ((Number) args[3]).intValue() : 50;

        MailContext ctx = MAIL_CONNECTIONS.get(handle);
        if (ctx == null || !ctx.isConnected()) {
            throw new InterpreterError("mail.list: not connected or invalid handle: " + handle);
        }

        try {
            // Close previous folder if different
            if (ctx.currentFolder != null && ctx.currentFolder.isOpen()) {
                if (!ctx.currentFolder.getFullName().equals(folderName)) {
                    ctx.currentFolder.close(false);
                    ctx.currentFolder = null;
                }
            }

            // Open folder if needed
            if (ctx.currentFolder == null || !ctx.currentFolder.isOpen()) {
                Folder folder = ctx.store.getFolder(folderName);
                if (!folder.exists()) {
                    throw new InterpreterError("mail.list: folder does not exist: " + folderName);
                }
                folder.open(Folder.READ_ONLY);
                ctx.currentFolder = folder;
            }

            Folder folder = ctx.currentFolder;
            int messageCount = folder.getMessageCount();

            List<Map<String, Object>> result = new ArrayList<>();

            if (messageCount == 0) {
                return result;
            }

            // Validate range
            if (start < 1) start = 1;
            if (start > messageCount) {
                return result; // No messages in range
            }

            int end = Math.min(start + count - 1, messageCount);

            // Get messages (1-based indexing)
            Message[] messages = folder.getMessages(start, end);

            for (Message msg : messages) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", msg.getMessageNumber());
                info.put("subject", msg.getSubject());

                // From addresses
                jakarta.mail.Address[] fromAddrs = msg.getFrom();
                if (fromAddrs != null && fromAddrs.length > 0) {
                    if (fromAddrs[0] instanceof InternetAddress ia) {
                        info.put("from", ia.getAddress());
                        info.put("fromName", ia.getPersonal());
                    } else {
                        info.put("from", fromAddrs[0].toString());
                    }
                }

                // Date
                java.util.Date sentDate = msg.getSentDate();
                if (sentDate != null) {
                    info.put("date", sentDate.getTime());
                    info.put("dateStr", sentDate.toString());
                }

                // Size
                info.put("size", msg.getSize());

                // Read flag
                info.put("read", msg.isSet(jakarta.mail.Flags.Flag.SEEN));

                result.add(info);
            }

            return result;

        } catch (MessagingException ex) {
            throw new InterpreterError("mail.list: " + ex.getMessage());
        }
    }

    /**
     * mail.get(handle, messageId) -> JSON (full message content)
     *
     * Retrieves a specific email by its message ID.
     *
     * @param args [0] handle (String), [1] messageId (Integer)
     * @return Map containing full message details (subject, from, to, cc, date, body, attachments)
     * @throws InterpreterError if operation fails
     */
    private static Map<String, Object> get(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("mail.get: requires handle and messageId");
        }

        String handle = (String) args[0];
        Number msgIdNum = (Number) args[1];

        if (msgIdNum == null) {
            throw new InterpreterError("mail.get: messageId cannot be null");
        }

        int messageId = msgIdNum.intValue();

        MailContext ctx = MAIL_CONNECTIONS.get(handle);
        if (ctx == null || !ctx.isConnected()) {
            throw new InterpreterError("mail.get: not connected or invalid handle: " + handle);
        }

        if (ctx.currentFolder == null || !ctx.currentFolder.isOpen()) {
            throw new InterpreterError("mail.get: no folder is open. Call mail.list first to open a folder.");
        }

        try {
            Folder folder = ctx.currentFolder;

            if (messageId < 1 || messageId > folder.getMessageCount()) {
                throw new InterpreterError("mail.get: messageId " + messageId +
                    " out of range (1-" + folder.getMessageCount() + ")");
            }

            Message msg = folder.getMessage(messageId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", msg.getMessageNumber());
            result.put("subject", msg.getSubject());

            // From addresses
            jakarta.mail.Address[] fromAddrs = msg.getFrom();
            if (fromAddrs != null && fromAddrs.length > 0) {
                List<Map<String, Object>> fromList = new ArrayList<>();
                for (jakarta.mail.Address addr : fromAddrs) {
                    fromList.add(addressToMap(addr));
                }
                result.put("from", fromList);
            }

            // To addresses
            jakarta.mail.Address[] toAddrs = msg.getRecipients(Message.RecipientType.TO);
            if (toAddrs != null) {
                List<Map<String, Object>> toList = new ArrayList<>();
                for (jakarta.mail.Address addr : toAddrs) {
                    toList.add(addressToMap(addr));
                }
                result.put("to", toList);
            }

            // CC addresses
            jakarta.mail.Address[] ccAddrs = msg.getRecipients(Message.RecipientType.CC);
            if (ccAddrs != null) {
                List<Map<String, Object>> ccList = new ArrayList<>();
                for (jakarta.mail.Address addr : ccAddrs) {
                    ccList.add(addressToMap(addr));
                }
                result.put("cc", ccList);
            }

            // BCC addresses
            jakarta.mail.Address[] bccAddrs = msg.getRecipients(Message.RecipientType.BCC);
            if (bccAddrs != null) {
                List<Map<String, Object>> bccList = new ArrayList<>();
                for (jakarta.mail.Address addr : bccAddrs) {
                    bccList.add(addressToMap(addr));
                }
                result.put("bcc", bccList);
            }

            // Date
            java.util.Date sentDate = msg.getSentDate();
            if (sentDate != null) {
                result.put("date", sentDate.getTime());
                result.put("dateStr", sentDate.toString());
            }

            // Size
            result.put("size", msg.getSize());

            // Read flag
            result.put("read", msg.isSet(jakarta.mail.Flags.Flag.SEEN));

            // Content type
            result.put("contentType", msg.getContentType());

            // Body and attachments
            List<Map<String, Object>> attachments = new ArrayList<>();
            String body = extractContent(msg, attachments);
            result.put("body", body);
            result.put("attachments", attachments);

            return result;

        } catch (Exception ex) {
            throw new InterpreterError("mail.get: " + ex.getMessage());
        }
    }

    /**
     * mail.close(handle) -> BOOL
     *
     * Closes the mail connection.
     *
     * @param args [0] handle (String)
     * @return true if closed successfully
     * @throws InterpreterError if operation fails
     */
    private static Boolean close(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("mail.close: requires handle");
        }

        String handle = (String) args[0];

        if (handle == null) {
            return Boolean.FALSE;
        }

        MailContext ctx = MAIL_CONNECTIONS.remove(handle);
        if (ctx == null) {
            return Boolean.FALSE;
        }

        ctx.close();
        return Boolean.TRUE;
    }

    /**
     * mail.folders(handle) -> JSON (array of folder names)
     *
     * Lists available folders in the mail account.
     *
     * @param args [0] handle (String)
     * @return List of folder information maps
     * @throws InterpreterError if operation fails
     */
    private static List<Map<String, Object>> folders(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("mail.folders: requires handle");
        }

        String handle = (String) args[0];

        MailContext ctx = MAIL_CONNECTIONS.get(handle);
        if (ctx == null || !ctx.isConnected()) {
            throw new InterpreterError("mail.folders: not connected or invalid handle: " + handle);
        }

        try {
            Folder defaultFolder = ctx.store.getDefaultFolder();
            Folder[] folders = defaultFolder.list("*");

            List<Map<String, Object>> result = new ArrayList<>();
            for (Folder folder : folders) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", folder.getFullName());
                info.put("type", getFolderType(folder.getType()));

                // Try to get message count if folder holds messages
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    try {
                        folder.open(Folder.READ_ONLY);
                        info.put("messageCount", folder.getMessageCount());
                        info.put("unreadCount", folder.getUnreadMessageCount());
                        folder.close(false);
                    } catch (MessagingException e) {
                        info.put("messageCount", -1);
                        info.put("unreadCount", -1);
                    }
                }

                result.add(info);
            }

            return result;

        } catch (MessagingException ex) {
            throw new InterpreterError("mail.folders: " + ex.getMessage());
        }
    }

    // --- Helper methods ---

    /**
     * Converts an Address to a Map representation.
     */
    private static Map<String, Object> addressToMap(jakarta.mail.Address addr) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (addr instanceof InternetAddress ia) {
            map.put("address", ia.getAddress());
            map.put("name", ia.getPersonal());
        } else {
            map.put("address", addr.toString());
        }
        return map;
    }

    /**
     * Extracts text content from a message, handling multipart messages.
     */
    private static String extractContent(Part part, List<Map<String, Object>> attachments) throws Exception {
        String contentType = part.getContentType();

        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                Part bodyPart = mp.getBodyPart(i);

                String disposition = bodyPart.getDisposition();
                if (disposition != null &&
                    (disposition.equalsIgnoreCase(Part.ATTACHMENT) ||
                     disposition.equalsIgnoreCase(Part.INLINE))) {
                    // This is an attachment
                    Map<String, Object> attachment = new LinkedHashMap<>();
                    attachment.put("filename", bodyPart.getFileName());
                    attachment.put("contentType", bodyPart.getContentType());
                    attachment.put("size", bodyPart.getSize());
                    attachments.add(attachment);
                } else {
                    // This is body content
                    String content = extractContent(bodyPart, attachments);
                    if (content != null && !content.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(content);
                    }
                }
            }
            return sb.toString();
        }

        // For other types, try to get as string
        Object content = part.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof InputStream) {
            // Binary attachment or unknown content
            return null;
        }

        return null;
    }

    /**
     * Returns a string representation of folder type flags.
     */
    private static String getFolderType(int type) {
        List<String> types = new ArrayList<>();
        if ((type & Folder.HOLDS_MESSAGES) != 0) {
            types.add("messages");
        }
        if ((type & Folder.HOLDS_FOLDERS) != 0) {
            types.add("folders");
        }
        return String.join(",", types);
    }

    /**
     * Cleans up all open mail connections. Called during interpreter shutdown.
     */
    public static void closeAll() {
        for (MailContext ctx : MAIL_CONNECTIONS.values()) {
            ctx.close();
        }
        MAIL_CONNECTIONS.clear();
    }
}
