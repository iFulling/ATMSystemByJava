package model;

import java.time.LocalDateTime;

public class OperationLog {
    private Long id;
    private Long userId;
    private String operation;
    private LocalDateTime timestamp;

    public OperationLog() {}

    public OperationLog(Long id, Long userId, String operation, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.operation = operation;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "OperationLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 