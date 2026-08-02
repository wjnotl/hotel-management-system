package repo;

import adt.ArrayList;
import adt.DequeInterface;
import adt.LinkedDeque;
import adt.ListInterface;
import entity.HousekeepingTask;
import util.BinaryFileUtil;

public class HousekeepingTaskRepo {
  private final BinaryFileUtil<ListInterface<HousekeepingTask>> fileUtil;
  private ListInterface<HousekeepingTask> taskList;
  private DequeInterface<HousekeepingTask> taskDeque;

  public HousekeepingTaskRepo() {
    this.fileUtil = new BinaryFileUtil<>("housekeeping_tasks.dat");
    load();
  }

  private void load() {
    this.taskList = fileUtil.retrieveFromFile();
    if (this.taskList == null) {
      this.taskList = new ArrayList<>();
    }

    // Rebuild the deque from stored list, preserving queue order.
    // Rule: PENDING == "still waiting in the deque". Anything else (ASSIGNED, IN_PROGRESS,
    // COMPLETED, SKIPPED) has already left the queue and is kept list-only for display/reporting.
    this.taskDeque = new LinkedDeque<>();
    for (int i = 1; i <= taskList.getNumberOfEntries(); i++) {
      HousekeepingTask t = taskList.getEntry(i);
      if (t == null) continue;

      if (t.getStatus() == HousekeepingTask.Status.PENDING) {
        taskDeque.addLast(t);
      }
    }
  }

  private void save() {
    fileUtil.saveToFile(taskList);
  }

  // Normal check-out/room-status trigger, or a manually added ad-hoc task: joins the back.
  public void enqueueTask(HousekeepingTask task) {
    taskList.add(task);
    taskDeque.addLast(task);
    save();
  }

  // VIP turnover, guest complaint, escalated overdue task: jumps straight to the front.
  public void enqueueUrgentTask(HousekeepingTask task) {
    task.setUrgent(true);
    taskList.add(task);
    taskDeque.addFirst(task);
    save();
  }

  // Staff pulls the next task off the front of the deque for assignment.
  public HousekeepingTask dequeueNextTask() {
    HousekeepingTask top = taskDeque.removeFirst();
    if (top != null) {
      save();
    }
    return top;
  }

  // Escalates an already-queued task by moving it to the front (does not touch its status).
  public boolean escalateToFront(HousekeepingTask task) {
    if (task == null) return false;

    boolean removed = taskDeque.remove(task);
    if (!removed) return false;

    task.setUrgent(true);
    taskDeque.addFirst(task);
    save();
    return true;
  }

  public boolean updateTaskStatus(HousekeepingTask task, HousekeepingTask.Status newStatus) {
    if (task == null) return false;

    task.setStatus(newStatus);

    // PENDING is the only status that belongs in the deque; anything else (ASSIGNED, IN_PROGRESS,
    // COMPLETED, SKIPPED) means the task has left the queue. Safe no-op if already removed.
    if (newStatus != HousekeepingTask.Status.PENDING) {
      taskDeque.remove(task);
    }

    save();
    return true;
  }

  // Step 2 of the lifecycle: a task gets a specific staff member, but work hasn't started yet.
  // The room stays whatever it currently is (e.g. still DIRTY) until startCleaning() is called.
  public boolean assignStaff(HousekeepingTask task, String staffId) {
    if (task == null) return false;

    task.setAssignedStaffId(staffId);
    if (task.getStatus() == HousekeepingTask.Status.PENDING) {
      task.setStatus(HousekeepingTask.Status.ASSIGNED);
    }

    // Claimed for work now, so it no longer waits in the queue for someone else to grab.
    taskDeque.remove(task);
    save();
    return true;
  }

  // Reassignment to a different staff member: unlike assignStaff(), this always resets the
  // task to ASSIGNED, even if it was already IN_PROGRESS. The new staff member hasn't actually
  // started cleaning yet and still needs to hit "Start Cleaning" themselves, no matter how far
  // the previous staff got.
  public boolean reassignTask(HousekeepingTask task, String staffId) {
    if (task == null) return false;

    task.setAssignedStaffId(staffId);
    task.setStatus(HousekeepingTask.Status.ASSIGNED);

    // No longer waiting in the queue — safe no-op if it wasn't there (e.g. was IN_PROGRESS).
    taskDeque.remove(task);
    save();
    return true;
  }

  // Step 3 of the lifecycle: staff actually begins cleaning. Requires an assigned staff member —
  // returns false if called on a task nobody's been assigned to yet.
  public boolean startCleaning(HousekeepingTask task) {
    if (task == null || task.getAssignedStaffId() == null) return false;

    task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
    taskDeque.remove(task); // no-op if ASSIGNED already took it out, safe either way
    save();
    return true;
  }

  public ListInterface<HousekeepingTask> getTaskList() {
    return taskList;
  }

  public DequeInterface<HousekeepingTask> getTaskDeque() {
    return taskDeque;
  }
}
