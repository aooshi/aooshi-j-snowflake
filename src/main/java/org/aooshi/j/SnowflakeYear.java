package org.aooshi.j;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 雪花算法
 */
public class SnowflakeYear extends Snowflake {

    private EpochCell epochCell;

    /**
     * initialize default
     */
    public SnowflakeYear() {
        super();
        this.epochCell = new EpochCell();
        this.scheduleYearUpdating();
    }

    /**
     * @param workerId     machine ID,1-32
     * @param datacenterId datacenter ID,1-32
     */
    public SnowflakeYear(long workerId, long datacenterId) {
        super(workerId, datacenterId);
        this.epochCell = new EpochCell();
        this.scheduleYearUpdating();
    }

    @Override
    public synchronized long nextId() {
        throw new RuntimeException("Please invoke yearId");
    }

    /**
     * 生成id
     *
     * @return
     */
    public synchronized String yearId() {
        EpochCell cell = this.epochCell;
        long id = super.nextId(cell.epoch);
        return Long.toHexString(cell.year) + Long.toHexString(id);
    }

    private void scheduleYearUpdating() {
        SnowflakeYear snowflakeYear = this;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "Reset year");
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                snowflakeYear.epochCell = new EpochCell();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private class EpochCell {
        private long epoch;
        private int year;

        private EpochCell() {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            int year = calendar.get(Calendar.YEAR);
            // year -1 保持结果位18位
            calendar.set(year - 1, 1, 1, 0, 0, 0);
            //
            this.year = year;
            this.epoch = calendar.getTime().getTime();
        }
    }
}
