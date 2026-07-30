package util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskSchedulerUtil {

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
  public static ScheduledExecutorService scheduleEvery(
      long initialDelay, long interval, Runnable task) {
    ScheduledExecutorService scheduler = createScheduler();
    scheduler.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.MINUTES);
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
