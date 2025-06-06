package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferRecord {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String remark;
    private String status;
    private int permissionsSnapshot;
    private Long logId;

    public TransferRecord() {}

    public TransferRecord(Long id, Long fromUserId, Long toUserId, BigDecimal amount, 
                         LocalDateTime timestamp, String remark, String status, int permissionsSnapshot, Long logId) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.remark = remark;
        this.status = status;
        this.permissionsSnapshot = permissionsSnapshot;
        this.logId = logId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPermissionsSnapshot() { return permissionsSnapshot; }
    public void setPermissionsSnapshot(int permissionsSnapshot) { this.permissionsSnapshot = permissionsSnapshot; }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    @Override
    public String toString() {
        return "TransferRecord{" +
                "id=" + id +
                ", fromUserId=" + fromUserId +
                ", toUserId=" + toUserId +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", remark='" + remark + '\'' +
                ", status='" + status + '\'' +
                ", permissionsSnapshot=" + permissionsSnapshot +
                ", logId=" + logId +
                '}';
    }
} 