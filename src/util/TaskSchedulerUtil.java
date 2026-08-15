package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskSchedulerUtil {

  // Schedule a task to run once after X milliseconds (or 0 for immediate execution)
  public static ScheduledExecutorService scheduleOnce(long delayMs, Runnable task) {
    ScheduledExecutorService scheduler = createScheduler();
    long actualDelay = Math.max(0, delayMs);
    scheduler.schedule(
        () -> {
          try {
            task.run();
          } finally {
            shutdown(scheduler); // Self-shutdown after execution
          }
        },
        actualDelay,
        TimeUnit.MILLISECONDS);
    return scheduler;
  }

  // Schedule a recurring task to run every X milliseconds
  public static ScheduledExecutorService scheduleEvery(
      long initialDelayMs, long intervalMs, Runnable task) {
    ScheduledExecutorService scheduler = createScheduler();
    long actualInitialDelay = Math.max(0, initialDelayMs);
    scheduler.scheduleAtFixedRate(task, actualInitialDelay, intervalMs, TimeUnit.MILLISECONDS);
    return scheduler;
  }

  private static ScheduledExecutorService createScheduler() {
    return Executors.newSingleThreadScheduledExecutor();
  }

  private static void shutdown(ScheduledExecutorService scheduler) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }
  }
}
