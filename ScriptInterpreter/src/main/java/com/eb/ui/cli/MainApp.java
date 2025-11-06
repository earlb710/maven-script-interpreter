package com.eb.ui.cli;

import com.eb.ui.ebs.EbsApp;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    private final EbsApp console;
    

    public MainApp() {
        console = new EbsApp();
    }

    @Override
    public void start(Stage stage) throws Exception {
        console.start(stage);
        //console.submit("/open test_script_json.ebs");
    }

    @Override
    public void stop() throws Exception {
        console.stop();
    }
    
    
    public static void main(String[] args) {
        launch(args);
    }
}
