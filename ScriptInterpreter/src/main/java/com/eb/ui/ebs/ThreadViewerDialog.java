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
 * Dialog that displays all currently running threads in the JVM
 * along with how long they have been running.
 * 
 * @author Earl Bosch
 */
public class ThreadViewerDialog extends Stage {

    private final TableView<ThreadEntry> threadTableView;
    private final Label threadCountLabel;
    private Timeline refreshTimeline;

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

        public ThreadEntry(String name, Thread.State state, long cpuTimeMs, long threadId, boolean isDaemon, int priority) {
            this.name = new SimpleStringProperty(name);
            this.state = new SimpleStringProperty(state != null ? state.toString() : "UNKNOWN");
            this.cpuTimeMs = new SimpleLongProperty(cpuTimeMs);
            this.threadId = new SimpleLongProperty(threadId);
            this.daemon = new SimpleStringProperty(isDaemon ? "Yes" : "No");
            this.priority = new SimpleStringProperty(String.valueOf(priority));
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
        threadTableView.getColumns().add(stateColumn);
        threadTableView.getColumns().add(cpuTimeColumn);
        threadTableView.getColumns().add(daemonColumn);
        threadTableView.getColumns().add(priorityColumn);

        // --- Buttons ---
        Button btnRefresh = new Button("Refresh");
        Button btnClose = new Button("Close");
        CheckBox autoRefreshCheckBox = new CheckBox("Auto-refresh (2s)");

        btnClose.setCancelButton(true);

        // --- Actions ---
        btnRefresh.setOnAction(e -> refreshThreadList());
        btnClose.setOnAction(e -> {
            stopAutoRefresh();
            close();
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
            "This view shows all currently running threads in the JVM.\n" +
            "CPU Time indicates how long the thread has been actively using the CPU."
        );
        infoLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btnRefresh, autoRefreshCheckBox, btnClose);

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
    }

    /**
     * Refresh the thread list by querying all active threads.
     */
    private void refreshThreadList() {
        List<ThreadEntry> entries = new ArrayList<>();
        
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);
        
        for (int i = 0; i < threadIds.length; i++) {
            long threadId = threadIds[i];
            ThreadInfo info = threadInfos[i];
            
            if (info != null) {
                // Get CPU time in nanoseconds, convert to milliseconds
                long cpuTimeNs = threadMXBean.getThreadCpuTime(threadId);
                long cpuTimeMs = cpuTimeNs >= 0 ? cpuTimeNs / 1_000_000 : -1;
                
                // Get additional thread info from Thread object if available
                Thread thread = findThreadById(threadId);
                boolean isDaemon = thread != null && thread.isDaemon();
                int priority = thread != null ? thread.getPriority() : Thread.NORM_PRIORITY;
                
                entries.add(new ThreadEntry(
                    info.getThreadName(),
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
        
        threadCountLabel.setText("Total threads: " + entries.size());
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
}
