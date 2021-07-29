package org.aooshi.j;

import org.junit.Test;

public class SnowflakeTest {

    @Test
    public void SnowflakeTest()
    {
        Snowflake s = new Snowflake();
        for (int i = 0; i < 100; i++) {
            System.out.println(s.nextId());
        }
    }

    @Test
    public void SnowflakeYearTest() {
        SnowflakeYear s = new SnowflakeYear();
        for (int i = 0; i < 100; i++) {
            System.out.println(s.yearId());
        }
    }

}
