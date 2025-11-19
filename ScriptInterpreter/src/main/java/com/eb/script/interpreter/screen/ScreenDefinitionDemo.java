package com.eb.script.interpreter.screen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Demo application to test the ScreenDefinition class functionality.
 * This demonstrates both singleton and non-singleton modes.
 */
public class ScreenDefinitionDemo extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Demo 1: Singleton mode (default)
        System.out.println("=== Demo 1: Singleton Mode ===");
        ScreenDefinition singletonDef = new ScreenDefinition(
            "MainScreen", 
            "Main Application Window", 
            800, 
            600
        );
        
        // Create screen multiple times - should get the same instance
        Stage screen1 = singletonDef.createScreen();
        Stage screen2 = singletonDef.createScreen();
        Stage screen3 = singletonDef.createScreen();
        
        System.out.println("Screen1 == Screen2: " + (screen1 == screen2));
        System.out.println("Screen2 == Screen3: " + (screen2 == screen3));
        System.out.println("Singleton definition: " + singletonDef);
        
        screen1.show();
        
        // Demo 2: Non-singleton mode
        System.out.println("\n=== Demo 2: Non-Singleton Mode ===");
        ScreenDefinition multiInstanceDef = new ScreenDefinition(
            "DocWindow",
            "Document Window",
            600,
            400,
            false  // non-singleton
        );
        
        // Create multiple screens - should get different instances
        Stage doc1 = multiInstanceDef.createScreen();
        Stage doc2 = multiInstanceDef.createScreen();
        Stage doc3 = multiInstanceDef.createScreen();
        
        System.out.println("Doc1 == Doc2: " + (doc1 == doc2));
        System.out.println("Doc1 title: " + doc1.getTitle());
        System.out.println("Doc2 title: " + doc2.getTitle());
        System.out.println("Doc3 title: " + doc3.getTitle());
        System.out.println("Multi-instance definition: " + multiInstanceDef);
        
        // Show all document windows at different positions
        doc1.setX(100);
        doc1.setY(100);
        doc1.show();
        
        doc2.setX(200);
        doc2.setY(150);
        doc2.show();
        
        doc3.setX(300);
        doc3.setY(200);
        doc3.show();
        
        // Demo 3: Using setters
        System.out.println("\n=== Demo 3: Using Setters ===");
        ScreenDefinition customDef = new ScreenDefinition();
        customDef.setScreenName("CustomScreen");
        customDef.setTitle("Custom Window");
        customDef.setWidth(500);
        customDef.setHeight(300);
        customDef.setSingleton(false);
        
        Stage custom = customDef.createScreen();
        System.out.println("Custom definition: " + customDef);
        System.out.println("Custom window title: " + custom.getTitle());
        
        custom.setX(400);
        custom.setY(250);
        custom.show();
        
        // Setup proper shutdown
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("Close the main window (Main Application Window) to exit.");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
