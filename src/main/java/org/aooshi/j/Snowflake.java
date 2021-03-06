package org.aooshi.j;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 雪花算法
 */
public class Snowflake {

    /* 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动） */
    private long twepoch = 1577808000000L; //2020-01-01 00:00:00
    private final long workerIdBits = 5L;/* 机器标识位数 */
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long sequenceBits = 12L;/* 毫秒内自增位 */
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /* 时间戳左移动位 */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId;

    /* 数据标识id部分 */
    private long datacenterId;
    private long sequence = 0L;/* 0，并发控制 */
    private long lastTimestamp = -1L;/* 上次生产id时间戳 */

    /**
     * initialize default
     */
    public Snowflake() {
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    /**
     * @param workerId     machine ID,1-32
     * @param datacenterId datacenter ID,1-32
     */
    public Snowflake(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            System.out.println(
                    String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            System.out.println(
                    String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * <p>
     * 获取 maxWorkerId
     * </p>
     */
    protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && "".equals(name)) {
            /*
             * GET jvmPid
             */
            mpid.append(name.split("@")[0]);
        }
        /*
         * MAC + PID 的 hashcode 获取16个低位
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * <p>
     * 数据标识id部分
     * </p>
     */
    protected static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            System.out.println(" getDatacenterId: " + e.getMessage());
        }
        return id;
    }

    /**
     * generate new id
     *
     * @return
     */
    public synchronized long nextId() {
        return this.nextId(this.twepoch);
    }

    /**
     * generate new id
     *
     * @param twepoch
     * @return
     */
    protected long nextId(long twepoch) {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {//闰秒
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 3 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)    // 时间戳部分
                | (datacenterId << datacenterIdShift)           // 数据中心部分
                | (workerId << workerIdShift)                   // 机器标识部分
                | sequence;                                     // 序列号部分
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * init epoch
     *
     * @param epoch
     */
    public Snowflake initEpoch(long epoch) {
        twepoch = epoch;
        return this;
    }

    /**
     * set epoch
     *
     * @param year
     */
    public Snowflake epochYear(int year) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        // year -1 保持结果位18位
        calendar.set(year, 1, 1, 0, 0, 0);
        twepoch = calendar.getTime().getTime();
        return this;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
