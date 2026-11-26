package com.court.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs slow work (database calls) on a background thread so the window never freezes,
 * then hands the result back on the JavaFX Application Thread.
 *
 * <pre>
 * BackgroundTask.run(() -&gt; caseService.findAll(),       // background thread
 *                    cases -&gt; table.getItems().setAll(cases),  // UI thread
 *                    error -&gt; AlertUtil.error("Error", error.getMessage()));
 * </pre>
 */
public final class BackgroundTask {

    private static final Logger LOG = Logger.getLogger(BackgroundTask.class.getName());

    private BackgroundTask() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            LOG.log(Level.WARNING, "Background task failed", error);
            onError.accept(error);
        });

        Thread thread = new Thread(task, "db-worker");
        thread.setDaemon(true);   // do not keep the app alive after the window closes
        thread.start();
    }

    /** Same as above, but errors are shown in a standard error dialog. */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, error -> AlertUtil.error("Something went wrong", error.getMessage()));
    }
}
