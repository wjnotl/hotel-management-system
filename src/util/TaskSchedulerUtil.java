package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskSchedulerUtil {

  // Create a new single-thread scheduler instance
  public static ScheduledExecutorService createScheduler() {
    return Executors.newSingleThreadScheduledExecutor();
  }

  // Schedule a task to run once after X minutes
  public static ScheduledExecutorService scheduleOnce(long minutes, Runnable task) {
    ScheduledExecutorService scheduler = createScheduler();
    scheduler.schedule(
        () -> {
          try {
            task.run();
          } finally {
            shutdown(scheduler); // Self-shutdown after execution
          }
        },
        minutes,
        TimeUnit.MINUTES);
    return scheduler;
  }

  // Schedule a recurring task to run every X minutes
  public static ScheduledExecutorService scheduleEvery(long minutes, Runnable task) {
    ScheduledExecutorService scheduler = createScheduler();
    scheduler.scheduleAtFixedRate(task, 0, minutes, TimeUnit.MINUTES);
    return scheduler;
  }

  // Gracefully wait for running task to finish, then stop
  public static void shutdown(ScheduledExecutorService scheduler) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }
  }

  // Immediately stop everything and interrupt running tasks
  public static void shutdownNow(ScheduledExecutorService scheduler) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }
}
