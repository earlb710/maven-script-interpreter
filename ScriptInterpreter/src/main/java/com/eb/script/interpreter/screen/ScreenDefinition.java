package com.eb.script.interpreter.screen;

import com.eb.script.interpreter.InterpreterContext;
import com.eb.script.token.DataType;
import javafx.stage.Stage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScreenDefinition class for managing screen creation with optional singleton pattern.
 * This class encapsulates screen properties and provides a factory method to create
 * JavaFX Stage instances.
 */
public class ScreenDefinition {
    
    private String screenName;
    private String title;
    private double width;
    private double height;
    private boolean singleton;
    private boolean showMenu; // Controls whether menu bar is shown at the top
    
    // Complex screen creation parameters
    private List<AreaDefinition> areas;
    private ConcurrentHashMap<String, Object> screenVars;
    private ConcurrentHashMap<String, DataType> varTypes;
    private ScreenFactory.OnClickHandler onClickHandler;
    private InterpreterContext context;
    
    // For singleton pattern: store the single instance
    private Stage singletonStage;
    
    // For non-singleton pattern: counter for unique titles
    private AtomicInteger instanceCounter;
    
    // Track all created stages for this definition
    private static final ConcurrentHashMap<String, ScreenDefinition> definitions = new ConcurrentHashMap<>();
    
    /**
     * Default constructor with singleton set to true and showMenu set to true
     */
    public ScreenDefinition() {
        this.singleton = true;
        this.showMenu = true; // Default to showing menu
        this.instanceCounter = new AtomicInteger(0);
    }
    
    /**
     * Constructor with all required fields
     * 
     * @param screenName The name identifier for this screen
     * @param title The window title
     * @param width The window width in pixels
     * @param height The window height in pixels
     */
    public ScreenDefinition(String screenName, String title, double width, double height) {
        this();
        this.screenName = screenName;
        this.title = title;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Constructor with all fields including singleton flag
     * 
     * @param screenName The name identifier for this screen
     * @param title The window title
     * @param width The window width in pixels
     * @param height The window height in pixels
     * @param singleton If true, createScreen() returns the same stage instance; if false, creates new instances
     */
    public ScreenDefinition(String screenName, String title, double width, double height, boolean singleton) {
        this(screenName, title, width, height);
        this.singleton = singleton;
    }
    
    /**
     * Creates and returns a Stage based on this definition.
     * If singleton is true, returns the same stage for all calls.
     * If singleton is false, creates a new stage with a counter appended to the title.
     * 
     * @return A Stage instance
     */
    public Stage createScreen() {
        if (singleton) {
            // Singleton mode: return the same stage instance
            if (singletonStage == null) {
                singletonStage = createNewStage(title);
                
                // Setup cleanup when stage is closed
                singletonStage.setOnCloseRequest(event -> {
                    singletonStage = null; // Allow recreation if closed
                });
            }
            return singletonStage;
        } else {
            // Non-singleton mode: create new stage with counter in title
            int count = instanceCounter.incrementAndGet();
            String instanceTitle = title + " #" + count;
            return createNewStage(instanceTitle);
        }
    }
    
    /**
     * Helper method to create a new Stage with the given title.
     * Uses ScreenFactory if areas are defined, otherwise creates a simple stage.
     */
    private Stage createNewStage(String stageTitle) {
        if (areas != null && !areas.isEmpty()) {
            // Use ScreenFactory to create complex screen with areas
            return ScreenFactory.createScreen(
                screenName,
                stageTitle,
                width,
                height,
                areas,
                screenVars,
                varTypes,
                onClickHandler,
                context,
                showMenu
            );
        } else {
            // Create simple stage without areas
            Stage stage = new Stage();
            stage.setTitle(stageTitle);
            stage.setWidth(width);
            stage.setHeight(height);
            
            // Create a simple scene with a StackPane
            javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, width, height);
            stage.setScene(scene);
            
            return stage;
        }
    }
    
    // Setter methods for all fields
    
    /**
     * Sets the screen name
     * 
     * @param screenName The screen name identifier
     */
    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
    
    /**
     * Sets the window title
     * 
     * @param title The window title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Sets the window width
     * 
     * @param width The width in pixels
     */
    public void setWidth(double width) {
        this.width = width;
    }
    
    /**
     * Sets the window height
     * 
     * @param height The height in pixels
     */
    public void setHeight(double height) {
        this.height = height;
    }
    
    /**
     * Sets the singleton flag
     * 
     * @param singleton If true, createScreen() returns the same stage; if false, creates new instances
     */
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }
    
    /**
     * Sets whether the menu bar should be shown at the top of the screen
     * 
     * @param showMenu If true, menu bar is shown; if false, menu bar is hidden
     */
    public void setShowMenu(boolean showMenu) {
        this.showMenu = showMenu;
    }
    
    /**
     * Sets the area definitions for complex screen layout
     * 
     * @param areas List of AreaDefinitions containing containers and items
     */
    public void setAreas(List<AreaDefinition> areas) {
        this.areas = areas;
    }
    
    /**
     * Sets the screen variables map for two-way data binding
     * 
     * @param screenVars The ConcurrentHashMap containing screen variables
     */
    public void setScreenVars(ConcurrentHashMap<String, Object> screenVars) {
        this.screenVars = screenVars;
    }
    
    /**
     * Sets the variable types map for proper type conversion
     * 
     * @param varTypes The ConcurrentHashMap containing screen variable types
     */
    public void setVarTypes(ConcurrentHashMap<String, DataType> varTypes) {
        this.varTypes = varTypes;
    }
    
    /**
     * Sets the onClick handler for button events
     * 
     * @param onClickHandler Handler for button onClick events
     */
    public void setOnClickHandler(ScreenFactory.OnClickHandler onClickHandler) {
        this.onClickHandler = onClickHandler;
    }
    
    /**
     * Sets the interpreter context for storing bound controls
     * 
     * @param context InterpreterContext to store bound controls for later refresh
     */
    public void setContext(InterpreterContext context) {
        this.context = context;
    }
    
    // Getter methods for all fields
    
    /**
     * Gets the screen name
     * 
     * @return The screen name identifier
     */
    public String getScreenName() {
        return screenName;
    }
    
    /**
     * Gets the window title
     * 
     * @return The window title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the window width
     * 
     * @return The width in pixels
     */
    public double getWidth() {
        return width;
    }
    
    /**
     * Gets the window height
     * 
     * @return The height in pixels
     */
    public double getHeight() {
        return height;
    }
    
    /**
     * Gets the singleton flag
     * 
     * @return true if singleton mode is enabled
     */
    public boolean isSingleton() {
        return singleton;
    }
    
    /**
     * Gets whether the menu bar should be shown at the top of the screen
     * 
     * @return true if menu bar is shown, false otherwise
     */
    public boolean isShowMenu() {
        return showMenu;
    }
    
    /**
     * Gets the current instance counter value
     * 
     * @return The number of instances created (for non-singleton mode)
     */
    public int getInstanceCount() {
        return instanceCounter.get();
    }
    
    /**
     * Gets the area definitions
     * 
     * @return List of AreaDefinitions
     */
    public List<AreaDefinition> getAreas() {
        return areas;
    }
    
    /**
     * Gets the screen variables map
     * 
     * @return The screen variables ConcurrentHashMap
     */
    public ConcurrentHashMap<String, Object> getScreenVars() {
        return screenVars;
    }
    
    /**
     * Gets the variable types map
     * 
     * @return The variable types ConcurrentHashMap
     */
    public ConcurrentHashMap<String, DataType> getVarTypes() {
        return varTypes;
    }
    
    /**
     * Gets the onClick handler
     * 
     * @return The onClick handler
     */
    public ScreenFactory.OnClickHandler getOnClickHandler() {
        return onClickHandler;
    }
    
    /**
     * Gets the interpreter context
     * 
     * @return The interpreter context
     */
    public InterpreterContext getContext() {
        return context;
    }
    
    @Override
    public String toString() {
        return "ScreenDefinition{" +
                "screenName='" + screenName + '\'' +
                ", title='" + title + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", singleton=" + singleton +
                ", showMenu=" + showMenu +
                ", instances=" + instanceCounter.get() +
                ", hasAreas=" + (areas != null && !areas.isEmpty()) +
                ", hasScreenVars=" + (screenVars != null) +
                ", hasContext=" + (context != null) +
                '}';
    }
}
