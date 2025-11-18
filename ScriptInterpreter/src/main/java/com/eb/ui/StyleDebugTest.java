package com.eb.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Standalone JavaFX debug test to verify CSS styling works correctly.
 * This class tests font sizes, colors, bold, italic styling on both labels and controls.
 */
public class StyleDebugTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        // Title
        Label title = new Label("=== JavaFX Styling Debug Test ===");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.getChildren().add(title);

        // Section 1: Label Font Sizes
        root.getChildren().add(createSectionLabel("1. Label Font Sizes"));
        root.getChildren().add(createTestLabel("10px font", "-fx-font-size: 10px;"));
        root.getChildren().add(createTestLabel("14px font (default-ish)", "-fx-font-size: 14px;"));
        root.getChildren().add(createTestLabel("18px font", "-fx-font-size: 18px;"));
        root.getChildren().add(createTestLabel("22px font", "-fx-font-size: 22px;"));

        // Section 2: Label Colors
        root.getChildren().add(createSectionLabel("2. Label Colors"));
        root.getChildren().add(createTestLabel("Red Text", "-fx-text-fill: red;"));
        root.getChildren().add(createTestLabel("Blue Text", "-fx-text-fill: blue;"));
        root.getChildren().add(createTestLabel("Green Text", "-fx-text-fill: green;"));
        root.getChildren().add(createTestLabel("Purple Text", "-fx-text-fill: purple;"));
        root.getChildren().add(createTestLabel("#FF5500 (Orange)", "-fx-text-fill: #FF5500;"));

        // Section 3: Label Bold/Italic
        root.getChildren().add(createSectionLabel("3. Label Bold/Italic"));
        root.getChildren().add(createTestLabel("Bold Text", "-fx-font-weight: bold;"));
        root.getChildren().add(createTestLabel("Italic Text", "-fx-font-style: italic;"));
        root.getChildren().add(createTestLabel("Bold + Italic", "-fx-font-weight: bold; -fx-font-style: italic;"));

        // Section 4: Label Combined Styles
        root.getChildren().add(createSectionLabel("4. Label Combined Styles"));
        root.getChildren().add(createTestLabel("18px Red Bold", "-fx-font-size: 18px; -fx-text-fill: red; -fx-font-weight: bold;"));
        root.getChildren().add(createTestLabel("16px Blue Italic", "-fx-font-size: 16px; -fx-text-fill: blue; -fx-font-style: italic;"));
        root.getChildren().add(createTestLabel("20px Green Bold+Italic", "-fx-font-size: 20px; -fx-text-fill: green; -fx-font-weight: bold; -fx-font-style: italic;"));

        // Section 5: TextField Font Sizes
        root.getChildren().add(createSectionLabel("5. TextField Font Sizes"));
        root.getChildren().add(createTestTextField("10px font", "-fx-font-size: 10px;"));
        root.getChildren().add(createTestTextField("14px font", "-fx-font-size: 14px;"));
        root.getChildren().add(createTestTextField("18px font", "-fx-font-size: 18px;"));
        root.getChildren().add(createTestTextField("22px font", "-fx-font-size: 22px;"));

        // Section 6: TextField Colors
        root.getChildren().add(createSectionLabel("6. TextField Colors"));
        root.getChildren().add(createTestTextField("Red text", "-fx-text-fill: red;"));
        root.getChildren().add(createTestTextField("Blue text", "-fx-text-fill: blue;"));
        root.getChildren().add(createTestTextField("Green text", "-fx-text-fill: green;"));

        // Section 7: TextField Bold/Italic
        root.getChildren().add(createSectionLabel("7. TextField Bold/Italic"));
        root.getChildren().add(createTestTextField("Bold text", "-fx-font-weight: bold;"));
        root.getChildren().add(createTestTextField("Italic text", "-fx-font-style: italic;"));
        root.getChildren().add(createTestTextField("Bold + Italic", "-fx-font-weight: bold; -fx-font-style: italic;"));

        // Section 8: TextField Combined Styles
        root.getChildren().add(createSectionLabel("8. TextField Combined Styles"));
        root.getChildren().add(createTestTextField("18px Red Bold", "-fx-font-size: 18px; -fx-text-fill: red; -fx-font-weight: bold;"));
        root.getChildren().add(createTestTextField("16px Blue Italic", "-fx-font-size: 16px; -fx-text-fill: blue; -fx-font-style: italic;"));
        root.getChildren().add(createTestTextField("20px Green All", "-fx-font-size: 20px; -fx-text-fill: green; -fx-font-weight: bold; -fx-font-style: italic;"));

        // Section 9: TextField Width Tests
        root.getChildren().add(createSectionLabel("9. TextField Width Tests"));
        TextField tf1 = new TextField("Width 100");
        tf1.setPrefWidth(100);
        root.getChildren().add(new HBox(5, new Label("Width 100:"), tf1));
        
        TextField tf2 = new TextField("Width 200");
        tf2.setPrefWidth(200);
        root.getChildren().add(new HBox(5, new Label("Width 200:"), tf2));
        
        TextField tf3 = new TextField("Width 300");
        tf3.setPrefWidth(300);
        root.getChildren().add(new HBox(5, new Label("Width 300:"), tf3));

        // Section 10: TextArea Tests
        root.getChildren().add(createSectionLabel("10. TextArea Styles"));
        TextArea ta1 = new TextArea("18px Blue Bold TextArea");
        ta1.setPrefHeight(60);
        ta1.setStyle("-fx-font-size: 18px; -fx-text-fill: blue; -fx-font-weight: bold;");
        root.getChildren().add(ta1);

        // Section 11: ComboBox Tests
        root.getChildren().add(createSectionLabel("11. ComboBox Styles"));
        ComboBox<String> cb1 = new ComboBox<>();
        cb1.getItems().addAll("Option 1", "Option 2", "Option 3");
        cb1.setValue("Option 1");
        cb1.setStyle("-fx-font-size: 18px; -fx-text-fill: red; -fx-font-weight: bold;");
        root.getChildren().add(cb1);

        // Section 12: Spinner Tests
        root.getChildren().add(createSectionLabel("12. Spinner Styles"));
        Spinner<Integer> spinner = new Spinner<>(0, 100, 50);
        spinner.setStyle("-fx-font-size: 18px; -fx-text-fill: green;");
        root.getChildren().add(spinner);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane, 800, 700);
        primaryStage.setTitle("JavaFX Styling Debug Test");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Print diagnostic information
        System.out.println("=== Styling Debug Test Started ===");
        System.out.println("If you see visual differences in:");
        System.out.println("  - Font sizes (10px vs 14px vs 18px vs 22px)");
        System.out.println("  - Colors (red, blue, green, purple)");
        System.out.println("  - Bold/Italic text styling");
        System.out.println("  - TextField widths (100 vs 200 vs 300)");
        System.out.println("Then JavaFX styling is working correctly!");
        System.out.println();
        System.out.println("If everything looks the same, there may be:");
        System.out.println("  1. A JavaFX version or platform issue");
        System.out.println("  2. CSS loading/application problem");
        System.out.println("  3. Style override happening somewhere");
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        return label;
    }

    private HBox createTestLabel(String description, String style) {
        Label descLabel = new Label(description + ":");
        descLabel.setMinWidth(200);
        
        Label testLabel = new Label("Test Label");
        testLabel.setStyle(style);
        
        HBox box = new HBox(10, descLabel, testLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createTestTextField(String description, String style) {
        Label descLabel = new Label(description + ":");
        descLabel.setMinWidth(200);
        
        TextField textField = new TextField("Test Text");
        textField.setStyle(style);
        textField.setPrefWidth(200);
        
        HBox box = new HBox(10, descLabel, textField);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
