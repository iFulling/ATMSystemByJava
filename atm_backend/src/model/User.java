package model;

import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.TimeUnit;

public class User {
    private Long id;
    private String username;
    private String password;
    private boolean enabled;
    private volatile long balance; // 以分为单位存储
    private int permissionsFlags;

    // 使用StampedLock控制余额的并发访问
    private final StampedLock balanceLock = new StampedLock();
    // 乐观读锁的超时时间（毫秒）
    private static final long OPTIMISTIC_READ_TIMEOUT = 100;

    public User() {
        this.balance = 0L;
    }

    public User(Long id, String username, String password, boolean enabled, double balance, int permissionsFlags) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.balance = (long) (balance * 100); // 转换为分
        this.permissionsFlags = permissionsFlags;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // 查询余额（乐观读锁）
    public double getBalance() {
        // 首先尝试乐观读
        long stamp = balanceLock.tryOptimisticRead();
        double currentBalance = balance / 100.0;

        // 验证乐观读是否有效
        if (!balanceLock.validate(stamp)) {
            // 如果乐观读失败，尝试获取悲观读锁
            try {
                stamp = balanceLock.tryReadLock(OPTIMISTIC_READ_TIMEOUT, TimeUnit.MILLISECONDS);
                if (stamp == 0L) {
                    // 如果获取读锁超时，抛出异常
                    throw new IllegalStateException("获取余额信息超时");
                }
                currentBalance = balance / 100.0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("获取余额信息被中断", e);
            } finally {
                if (stamp != 0L) {
                    balanceLock.unlockRead(stamp);
                }
            }
        }
        return currentBalance;
    }

    // 设置余额（写锁）
    public void setBalance(double balance) {
        long stamp = balanceLock.writeLock();
        try {
            this.balance = (long) (balance * 100); // 转换为分
        } finally {
            balanceLock.unlockWrite(stamp);
        }
    }

    // 原子操作余额的方法
    public double addAndGetBalance(double delta) {
        long stamp = balanceLock.writeLock();
        try {
            long deltaInCents = (long) (delta * 100);
            balance += deltaInCents;
            return balance / 100.0;
        } finally {
            balanceLock.unlockWrite(stamp);
        }
    }

    public double getAndAddBalance(double delta) {
        long stamp = balanceLock.writeLock();
        try {
            long deltaInCents = (long) (delta * 100);
            double oldBalance = balance / 100.0;
            balance += deltaInCents;
            return oldBalance;
        } finally {
            balanceLock.unlockWrite(stamp);
        }
    }

    public boolean compareAndSetBalance(double expect, double update) {
        long stamp = balanceLock.writeLock();
        try {
            long expectInCents = (long) (expect * 100);
            long updateInCents = (long) (update * 100);

            if (balance == expectInCents) {
                balance = updateInCents;
                return true;
            }
            return false;
        } finally {
            balanceLock.unlockWrite(stamp);
        }
    }

    public int getPermissionsFlags() {
        return permissionsFlags;
    }

    public void setPermissionsFlags(int permissionsFlags) {
        this.permissionsFlags = permissionsFlags;
    }

    // 权限检查方法
    public boolean hasDepositPermission() {
        return (permissionsFlags & 1) != 0;
    }

    public boolean hasWithdrawPermission() {
        return (permissionsFlags & 2) != 0;
    }

    public boolean hasTransferOutPermission() {
        return (permissionsFlags & 4) != 0;
    }

    public boolean hasTransferInPermission() {
        return (permissionsFlags & 8) != 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", enabled=" + enabled +
                ", balance=" + (balance / 100.0) +
                ", permissionsFlags=" + permissionsFlags +
                '}';
    }
} 