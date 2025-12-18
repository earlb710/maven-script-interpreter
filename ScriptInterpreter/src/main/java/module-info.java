/**
 * Module definition for the EBS Script Interpreter application.
 * Converts the application to use Java Platform Module System (JPMS).
 */
module com.eb.scriptinterpreter {
    // Java Platform Modules
    requires java.base;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires java.prefs;
    requires java.management;
    
    // JavaFX Modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.media;
    
    // RichTextFX and dependencies
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires org.fxmisc.undo;
    requires reactfx;
    requires wellbehavedfx;
    
    // Database JDBC Drivers
    requires mysql.connector.j;
    requires org.postgresql.jdbc;
    
    // Jakarta Mail
    requires jakarta.mail;
    requires org.eclipse.angus.mail;
    
    // Apache Commons Net for FTP
    requires org.apache.commons.net;
    
    // Commonmark for Markdown to HTML conversion
    requires org.commonmark;
    
    // JavaFX SVG Support - provides Batik integration
    // Batik is provided as automatic modules, we need to require them explicitly
    // Note: We only require the Batik modules we actually use to avoid issues with
    //       batik-script which has a broken module descriptor
    requires javafxsvg;
    requires batik.transcoder;
    requires batik.anim;
    requires batik.awt.util;
    // requires batik.bridge;  // Excluded - pulls in batik-script with broken module descriptor
    requires batik.css;
    requires batik.dom;
    requires batik.gvt;
    requires batik.parser;
    requires batik.svg.dom;
    requires batik.svggen;
    requires batik.util;
    requires batik.xml;
    requires xml.apis.ext; // for org.w3c.dom.svg
    // Note: batik.ext has split packages with java.xml, so we exclude it
    // requires batik.ext;
    
    // Export packages that might be needed by reflection or external access
    exports com.eb.script;
    exports com.eb.script.token;
    exports com.eb.script.token.ebs;
    exports com.eb.script.parser;
    exports com.eb.script.interpreter;
    exports com.eb.script.interpreter.builtins;
    exports com.eb.script.interpreter.db;
    exports com.eb.script.interpreter.expression;
    exports com.eb.script.interpreter.statement;
    exports com.eb.script.interpreter.plugin;
    exports com.eb.script.interpreter.screen;
    exports com.eb.script.interpreter.screen.data;
    exports com.eb.script.interpreter.screen.display;
    exports com.eb.script.arrays;
    exports com.eb.script.json;
    exports com.eb.script.file;
    exports com.eb.script.ftp;
    exports com.eb.script.image;
    exports com.eb.ui.cli;
    exports com.eb.ui.ebs;
    exports com.eb.ui.tabs;
    exports com.eb.util;
    exports com.eb.util.io;
    
    // Open packages for reflection (used by Class.forName for UI dialogs and plugin loading)
    opens com.eb.ui.ebs to javafx.graphics, javafx.base;
    opens com.eb.script.interpreter to javafx.graphics;
    opens com.eb.script.interpreter.plugin to javafx.graphics;
    opens com.eb.script.interpreter.builtins to javafx.graphics;
    
    // Open for resource access
    opens com.eb.script.interpreter.screen;
    opens icons;
    opens images.chess;
    opens scripts;
    opens css;
    opens json;
}
