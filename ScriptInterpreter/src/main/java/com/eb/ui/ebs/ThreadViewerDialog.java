package com.eb.ui.ebs;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Dialog that displays JavaFX and screen-related threads in the JVM
 * along with how long they have been running.
 * 
 * @author Earl Bosch
 */
public class ThreadViewerDialog extends Stage {

    private final TableView<ThreadEntry> threadTableView;
    private final Label threadCountLabel;
    private Timeline refreshTimeline;

    private static final String SCREEN_THREAD_PREFIX = "Screen-";
    
    // Prefixes/patterns for JavaFX threads
    private static final String[] JAVAFX_THREAD_PATTERNS = {
        "JavaFX",
        "FX Application",
        "QuantumRenderer",
        "Prism",
        "InvokeLaterDispatcher"
    };

    /**
     * Data model for a thread entry.
     */
    public static class ThreadEntry {
        private final SimpleStringProperty name;
        private final SimpleStringProperty state;
        private final SimpleLongProperty cpuTimeMs;
        private final SimpleLongProperty threadId;
        private final SimpleStringProperty daemon;
        private final SimpleStringProperty priority;
        private final SimpleStringProperty screenName;

        public ThreadEntry(String name, Thread.State state, long cpuTimeMs, long threadId, boolean isDaemon, int priority) {
            this.name = new SimpleStringProperty(name);
            this.state = new SimpleStringProperty(state != null ? state.toString() : "UNKNOWN");
            this.cpuTimeMs = new SimpleLongProperty(cpuTimeMs);
            this.threadId = new SimpleLongProperty(threadId);
            this.daemon = new SimpleStringProperty(isDaemon ? "Yes" : "No");
            this.priority = new SimpleStringProperty(String.valueOf(priority));
            // Extract screen name from thread name if it follows the "Screen-<screenName>" pattern
            this.screenName = new SimpleStringProperty(extractScreenName(name));
        }

        /**
         * Extract the screen name from a thread name if it follows the "Screen-<screenName>" pattern.
         * @param threadName The thread name to parse
         * @return The screen name, or empty string if not a screen thread
         */
        private static String extractScreenName(String threadName) {
            if (threadName != null && threadName.startsWith(SCREEN_THREAD_PREFIX)) {
                return threadName.substring(SCREEN_THREAD_PREFIX.length());
            }
            return "";
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getState() {
            return state.get();
        }

        public SimpleStringProperty stateProperty() {
            return state;
        }

        public long getCpuTimeMs() {
            return cpuTimeMs.get();
        }

        public SimpleLongProperty cpuTimeMsProperty() {
            return cpuTimeMs;
        }

        public long getThreadId() {
            return threadId.get();
        }

        public SimpleLongProperty threadIdProperty() {
            return threadId;
        }

        public String getDaemon() {
            return daemon.get();
        }

        public SimpleStringProperty daemonProperty() {
            return daemon;
        }

        public String getPriority() {
            return priority.get();
        }

        public SimpleStringProperty priorityProperty() {
            return priority;
        }

        public String getScreenName() {
            return screenName.get();
        }

        public SimpleStringProperty screenNameProperty() {
            return screenName;
        }

        /**
         * Check if this thread is a screen thread.
         * @return true if this is a screen thread
         */
        public boolean isScreenThread() {
            String screenNameValue = screenName.get();
            return screenNameValue != null && !screenNameValue.isEmpty();
        }

        /**
         * Format CPU time as a human-readable duration string.
         */
        public String getFormattedCpuTime() {
            long ms = getCpuTimeMs();
            if (ms < 0) {
                return "N/A";
            }
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return String.format("%d:%02d:%02d.%03d", hours, minutes % 60, seconds % 60, ms % 1000);
            } else if (minutes > 0) {
                return String.format("%d:%02d.%03d", minutes, seconds % 60, ms % 1000);
            } else if (seconds > 0) {
                return String.format("%d.%03d s", seconds, ms % 1000);
            } else {
                return String.format("%d ms", ms);
            }
        }
    }

    public ThreadViewerDialog() {
        setTitle("Thread Viewer");
        initModality(Modality.NONE); // Allow interaction with main window

        // --- Thread count label ---
        threadCountLabel = new Label("Loading threads...");

        // --- Create TableView for threads ---
        threadTableView = new TableView<>();
        threadTableView.setPrefHeight(400);
        threadTableView.setPrefWidth(900);
        threadTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Thread ID column
        TableColumn<ThreadEntry, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> cellData.getValue().threadIdProperty());
        idColumn.setMinWidth(50);
        idColumn.setMaxWidth(80);

        // Name column
        TableColumn<ThreadEntry, String> nameColumn = new TableColumn<>("Thread Name");
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setMinWidth(250);

        // Screen column - shows extracted screen name for screen threads
        TableColumn<ThreadEntry, String> screenColumn = new TableColumn<>("Screen");
        screenColumn.setCellValueFactory(cellData -> cellData.getValue().screenNameProperty());
        screenColumn.setMinWidth(100);
        screenColumn.setMaxWidth(150);

        // State column
        TableColumn<ThreadEntry, String> stateColumn = new TableColumn<>("State");
        stateColumn.setCellValueFactory(cellData -> cellData.getValue().stateProperty());
        stateColumn.setMinWidth(100);
        stateColumn.setMaxWidth(120);

        // CPU Time column (formatted)
        TableColumn<ThreadEntry, String> cpuTimeColumn = new TableColumn<>("CPU Time");
        cpuTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedCpuTime()));
        cpuTimeColumn.setMinWidth(120);
        cpuTimeColumn.setMaxWidth(150);

        // Daemon column
        TableColumn<ThreadEntry, String> daemonColumn = new TableColumn<>("Daemon");
        daemonColumn.setCellValueFactory(cellData -> cellData.getValue().daemonProperty());
        daemonColumn.setMinWidth(60);
        daemonColumn.setMaxWidth(80);

        // Priority column
        TableColumn<ThreadEntry, String> priorityColumn = new TableColumn<>("Priority");
        priorityColumn.setCellValueFactory(cellData -> cellData.getValue().priorityProperty());
        priorityColumn.setMinWidth(60);
        priorityColumn.setMaxWidth(80);

        threadTableView.getColumns().add(idColumn);
        threadTableView.getColumns().add(nameColumn);
        threadTableView.getColumns().add(screenColumn);
        threadTableView.getColumns().add(stateColumn);
        threadTableView.getColumns().add(cpuTimeColumn);
        threadTableView.getColumns().add(daemonColumn);
        threadTableView.getColumns().add(priorityColumn);

        // --- Buttons ---
        Button btnRefresh = new Button("Refresh");
        Button btnStopThread = new Button("Stop Screen Thread");
        Button btnClose = new Button("Close");
        CheckBox autoRefreshCheckBox = new CheckBox("Auto-refresh (2s)");
        autoRefreshCheckBox.setSelected(true); // Auto-refresh enabled by default

        btnClose.setCancelButton(true);
        btnStopThread.setDisable(true); // Disabled until a screen thread is selected

        // --- Actions ---
        btnRefresh.setOnAction(e -> refreshThreadList());
        btnClose.setOnAction(e -> {
            stopAutoRefresh();
            close();
        });
        
        // Enable/disable stop button based on selection
        threadTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnStopThread.setDisable(newVal == null || !newVal.isScreenThread());
        });
        
        btnStopThread.setOnAction(e -> {
            ThreadEntry selected = threadTableView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isScreenThread()) {
                stopScreenThread(selected.getScreenName());
                refreshThreadList();
            }
        });
        
        autoRefreshCheckBox.setOnAction(e -> {
            if (autoRefreshCheckBox.isSelected()) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
        });

        // Stop auto-refresh when dialog is closed
        setOnCloseRequest(e -> stopAutoRefresh());

        // --- Layout ---
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(16));

        Label infoLabel = new Label(
            "This view shows JavaFX and screen-related threads.\n" +
            "CPU Time indicates how long the thread has been actively using the CPU.\n" +
            "The Screen column shows the screen name for screen-related threads."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnRefresh, btnStopThread, autoRefreshCheckBox, btnClose);

        layout.getChildren().addAll(
            infoLabel,
            threadCountLabel,
            threadTableView,
            buttonBox
        );

        VBox.setVgrow(threadTableView, Priority.ALWAYS);

        setScene(new Scene(layout));
        sizeToScene();
        setMinWidth(950);
        setMinHeight(500);

        // Initial load
        refreshThreadList();
        
        // Start auto-refresh by default
        startAutoRefresh();
    }

    /**
     * Refresh the thread list by querying JavaFX and screen threads only.
     * Preserves the currently selected row by remembering the thread ID and restoring selection after refresh.
     */
    private void refreshThreadList() {
        // Remember the currently selected thread ID to restore selection after refresh
        ThreadEntry selectedEntry = threadTableView.getSelectionModel().getSelectedItem();
        long selectedThreadId = selectedEntry != null ? selectedEntry.getThreadId() : -1;
        
        List<ThreadEntry> entries = new ArrayList<>();
        
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);
        
        for (int i = 0; i < threadIds.length; i++) {
            long threadId = threadIds[i];
            ThreadInfo info = threadInfos[i];
            
            if (info != null) {
                String threadName = info.getThreadName();
                
                // Only include JavaFX threads and screen threads
                if (!isJavaFXOrScreenThread(threadName)) {
                    continue;
                }
                
                // Get CPU time in nanoseconds, convert to milliseconds
                long cpuTimeNs = threadMXBean.getThreadCpuTime(threadId);
                long cpuTimeMs = cpuTimeNs >= 0 ? cpuTimeNs / 1_000_000 : -1;
                
                // Get additional thread info from Thread object if available
                Thread thread = findThreadById(threadId);
                boolean isDaemon = thread != null && thread.isDaemon();
                int priority = thread != null ? thread.getPriority() : Thread.NORM_PRIORITY;
                
                entries.add(new ThreadEntry(
                    threadName,
                    info.getThreadState(),
                    cpuTimeMs,
                    threadId,
                    isDaemon,
                    priority
                ));
            }
        }
        
        // Sort by thread ID
        entries.sort((a, b) -> Long.compare(a.getThreadId(), b.getThreadId()));
        
        threadTableView.getItems().clear();
        threadTableView.getItems().addAll(entries);
        
        threadCountLabel.setText("JavaFX & Screen threads: " + entries.size());
        
        // Restore selection to the previously selected thread if it still exists
        if (selectedThreadId >= 0) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getThreadId() == selectedThreadId) {
                    threadTableView.getSelectionModel().select(i);
                    threadTableView.scrollTo(i);
                    break;
                }
            }
        }
    }

    /**
     * Check if a thread is a JavaFX thread or a screen thread.
     * @param threadName The name of the thread to check
     * @return true if it's a JavaFX or screen thread
     */
    private boolean isJavaFXOrScreenThread(String threadName) {
        if (threadName == null) {
            return false;
        }
        
        // Check if it's a screen thread
        if (threadName.startsWith(SCREEN_THREAD_PREFIX)) {
            return true;
        }
        
        // Check if it matches any JavaFX thread pattern
        for (String pattern : JAVAFX_THREAD_PATTERNS) {
            if (threadName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Find a Thread object by its ID.
     * Uses ThreadGroup enumeration which is more efficient than getAllStackTraces().
     */
    private Thread findThreadById(long threadId) {
        // Get root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        // Enumerate all threads
        int estimatedSize = rootGroup.activeCount() * 2;
        Thread[] threads = new Thread[estimatedSize];
        int count = rootGroup.enumerate(threads, true);
        
        for (int i = 0; i < count; i++) {
            if (threads[i] != null && threads[i].getId() == threadId) {
                return threads[i];
            }
        }
        return null;
    }

    /**
     * Start auto-refresh timer.
     */
    private void startAutoRefresh() {
        if (refreshTimeline == null) {
            refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshThreadList()));
            refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        refreshTimeline.play();
    }

    /**
     * Stop auto-refresh timer.
     */
    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
    
    /**
     * Stop a screen thread by its screen name.
     * This interrupts the thread found by searching for threads with the "Screen-{screenName}" pattern.
     * 
     * @param screenName The name of the screen whose thread should be stopped
     */
    private void stopScreenThread(String screenName) {
        if (screenName == null || screenName.isEmpty()) {
            return;
        }
        
        String targetThreadName = SCREEN_THREAD_PREFIX + screenName;
        
        // Find the thread by name and interrupt it
        Thread targetThread = findThreadByName(targetThreadName);
        if (targetThread != null && targetThread.isAlive()) {
            // Show confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Stop Screen Thread");
            confirm.setHeaderText("Stop thread: " + targetThreadName);
            confirm.setContentText("Are you sure you want to stop this screen thread? This may cause the associated screen to become unresponsive.");
            confirm.initOwner(this);
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    targetThread.interrupt();
                }
            });
        }
    }
    
    /**
     * Find a thread by its exact name.
     * 
     * @param threadName The name of the thread to find
     * @return The Thread object, or null if not found
     */
    private Thread findThreadByName(String threadName) {
        // Get root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        // Enumerate all threads
        int estimatedSize = rootGroup.activeCount() * 2;
        Thread[] threads = new Thread[estimatedSize];
        int count = rootGroup.enumerate(threads, true);
        
        for (int i = 0; i < count; i++) {
            if (threads[i] != null && threadName.equals(threads[i].getName())) {
                return threads[i];
            }
        }
        return null;
    }
}
