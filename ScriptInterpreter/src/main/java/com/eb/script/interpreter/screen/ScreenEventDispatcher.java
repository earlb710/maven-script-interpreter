package com.eb.script.interpreter.screen;

import com.eb.script.interpreter.InterpreterError;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages event dispatch and execution for screen threads.
 * 
 * This class provides a mechanism to execute EBS code on a screen's dedicated thread
 * rather than on the JavaFX Application Thread. This enables true thread-based screen
 * isolation where stopping a screen thread also stops its event handlers.
 * 
 * Events are dispatched via a BlockingQueue and executed by the screen thread,
 * while UI updates are still performed on the JavaFX Application Thread via Platform.runLater().
 * 
 * @author Earl Bosch
 */
public class ScreenEventDispatcher {

    /** Default timeout for synchronous event dispatch in milliseconds */
    public static final long DEFAULT_DISPATCH_TIMEOUT_MS = 30000;
    
    /** Polling interval for the event loop in milliseconds */
    private static final long EVENT_LOOP_POLL_INTERVAL_MS = 100;

    /**
     * Functional interface for EBS code execution.
     */
    @FunctionalInterface
    public interface CodeExecutor {
        /**
         * Execute EBS code and optionally return a result.
         * 
         * @param ebsCode The EBS code to execute
         * @return The result of execution (can be null)
         * @throws InterpreterError If execution fails
         */
        Object execute(String ebsCode) throws InterpreterError;
    }

    /**
     * Represents an event to be executed on the screen thread.
     */
    private static class ScreenEvent {
        final String ebsCode;
        final boolean waitForResult;
        final AtomicReference<Object> result;
        final AtomicReference<Exception> error;
        final CountDownLatch completionLatch;

        ScreenEvent(String ebsCode, boolean waitForResult) {
            this.ebsCode = ebsCode;
            this.waitForResult = waitForResult;
            this.result = new AtomicReference<>();
            this.error = new AtomicReference<>();
            this.completionLatch = waitForResult ? new CountDownLatch(1) : null;
        }

        void setResult(Object value) {
            result.set(value);
        }

        void setError(Exception e) {
            error.set(e);
        }

        void signalCompletion() {
            if (completionLatch != null) {
                completionLatch.countDown();
            }
        }

        Object waitForResult(long timeoutMs) throws InterpreterError {
            if (completionLatch != null) {
                try {
                    boolean completed = completionLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
                    if (!completed) {
                        throw new InterpreterError("Event execution timed out after " + timeoutMs + "ms");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterpreterError("Event execution was interrupted");
                }
            }
            
            Exception e = error.get();
            if (e != null) {
                if (e instanceof InterpreterError) {
                    throw (InterpreterError) e;
                }
                throw new InterpreterError("Event execution failed: " + e.getMessage());
            }
            
            return result.get();
        }
    }

    /** Special poison pill event to signal thread shutdown */
    private static final ScreenEvent SHUTDOWN_EVENT = new ScreenEvent(null, false);

    private final String screenName;
    private final BlockingQueue<ScreenEvent> eventQueue;
    private final CodeExecutor codeExecutor;
    private volatile boolean running;
    private Thread executorThread;

    /**
     * Creates a new ScreenEventDispatcher.
     * 
     * @param screenName The name of the screen this dispatcher is for
     * @param codeExecutor The executor to use for running EBS code
     */
    public ScreenEventDispatcher(String screenName, CodeExecutor codeExecutor) {
        this.screenName = screenName;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.codeExecutor = codeExecutor;
        this.running = false;
    }

    /**
     * Starts the event processing loop on the specified thread.
     * This method should be called from within the screen thread's run method.
     */
    public void runEventLoop() {
        running = true;
        executorThread = Thread.currentThread();
        
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for an event with a timeout to allow periodic interrupt checks
                    ScreenEvent event = eventQueue.poll(EVENT_LOOP_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    
                    if (event == null) {
                        continue; // No event, continue waiting
                    }
                    
                    if (event == SHUTDOWN_EVENT) {
                        break; // Shutdown requested
                    }
                    
                    // Execute the event
                    try {
                        Object result = codeExecutor.execute(event.ebsCode);
                        event.setResult(result);
                    } catch (Exception e) {
                        event.setError(e);
                    } finally {
                        event.signalCompletion();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            running = false;
            // Complete any remaining events with interruption error
            drainQueueOnShutdown();
        }
    }

    /**
     * Dispatches EBS code for execution on the screen thread.
     * This method does not wait for execution to complete.
     * 
     * @param ebsCode The EBS code to execute
     */
    public void dispatchAsync(String ebsCode) {
        if (!running) {
            System.err.println("Warning: Cannot dispatch event - screen thread is not running for screen: " + screenName);
            return;
        }
        
        ScreenEvent event = new ScreenEvent(ebsCode, false);
        eventQueue.offer(event);
    }

    /**
     * Dispatches EBS code for execution on the screen thread and waits for the result.
     * 
     * @param ebsCode The EBS code to execute
     * @return The result of the execution
     * @throws InterpreterError If execution fails or times out
     */
    public Object dispatchSync(String ebsCode) throws InterpreterError {
        return dispatchSync(ebsCode, DEFAULT_DISPATCH_TIMEOUT_MS);
    }

    /**
     * Dispatches EBS code for execution on the screen thread and waits for the result.
     * 
     * @param ebsCode The EBS code to execute
     * @param timeoutMs Maximum time to wait for execution in milliseconds
     * @return The result of the execution
     * @throws InterpreterError If execution fails or times out
     */
    public Object dispatchSync(String ebsCode, long timeoutMs) throws InterpreterError {
        // If we're already on the screen thread, execute directly to avoid deadlock
        if (Thread.currentThread() == executorThread) {
            return codeExecutor.execute(ebsCode);
        }
        
        if (!running) {
            throw new InterpreterError("Cannot dispatch event - screen thread is not running for screen: " + screenName);
        }
        
        ScreenEvent event = new ScreenEvent(ebsCode, true);
        eventQueue.offer(event);
        
        return event.waitForResult(timeoutMs);
    }

    /**
     * Checks if the dispatcher is currently running.
     * 
     * @return true if the event loop is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Signals the event loop to stop.
     * The loop will finish processing the current event before stopping.
     */
    public void shutdown() {
        running = false;
        eventQueue.offer(SHUTDOWN_EVENT);
    }

    /**
     * Gets the screen name associated with this dispatcher.
     * 
     * @return The screen name
     */
    public String getScreenName() {
        return screenName;
    }

    /**
     * Drains the queue on shutdown, signaling completion with errors for any pending events.
     */
    private void drainQueueOnShutdown() {
        ScreenEvent event;
        while ((event = eventQueue.poll()) != null) {
            if (event != SHUTDOWN_EVENT && event.completionLatch != null) {
                event.setError(new InterpreterError("Screen thread was shut down before event could be executed"));
                event.signalCompletion();
            }
        }
    }
}
