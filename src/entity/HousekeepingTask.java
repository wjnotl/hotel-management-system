package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class HousekeepingTask implements Serializable {
  private static final long serialVersionUID = 1L;

  public static enum TaskType {
    STANDARD_CLEAN,
    DEEP_CLEAN,
    TURNOVER,
    MAINTENANCE_CHECK
  }

  public static enum Status {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
  }

  private String taskId;
  private String roomNumber;
  private TaskType taskType;
  private Status status;
  private String assignedStaffId;
  private boolean isUrgent;
  private LocalDateTime createdAt;

  public HousekeepingTask(
      String taskId,
      String roomNumber,
      TaskType taskType,
      Status status,
      String assignedStaffId,
      boolean isUrgent,
      LocalDateTime createdAt) {
    this.taskId = taskId;
    this.roomNumber = roomNumber;
    this.taskType = taskType;
    this.status = status;
    this.assignedStaffId = assignedStaffId;
    this.isUrgent = isUrgent;
    this.createdAt = createdAt;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public Status getStatus() {
    return status;
  }

  public String getAssignedStaffId() {
    return assignedStaffId;
  }

  public boolean getIsUrgent() {
    return isUrgent;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setAssignedStaffId(String assignedStaffId) {
    this.assignedStaffId = assignedStaffId;
  }

  public void setUrgent(boolean isUrgent) {
    this.isUrgent = isUrgent;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    HousekeepingTask other = (HousekeepingTask) obj;
    return taskId != null && taskId.equals(other.taskId);
  }

  @Override
  public int hashCode() {
    return taskId != null ? taskId.hashCode() : 0;
  }
}
