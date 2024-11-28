package org.openl.spring.env;

import java.util.Random;
import java.util.UUID;

import org.springframework.core.env.PropertySource;

import org.openl.util.StringUtils;

/**
 * Like in the Spring Boot.
 *
 * @author Yury Molchan
 */
class RandomValuePropertySource extends PropertySource<Random> {

    public RandomValuePropertySource() {
        super("random", new Random());
    }

    @Override
    public Object getProperty(String name) {
        switch (name) {
            case "random.int":
                return getSource().nextInt();
            case "random.long":
                return getSource().nextLong();
            case "random.uuid":
                return UUID.randomUUID().toString();
        }

        if (name.startsWith("random.int(")) {
            var range = name.substring(11, name.length() - 1);
            String[] values = StringUtils.split(range, ',');

            int min = 0;
            int max = Integer.valueOf(values[0]);
            if (values.length > 1) {
                min = max;
                max = Integer.valueOf(values[1]);
            }

            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Illegal range '" + range + "'");
            }

            return getSource().ints(min, max).findFirst().getAsInt();
        }

        if (name.startsWith("random.long(")) {
            var range = name.substring(12, name.length() - 1);
            String[] values = StringUtils.split(range, ',');

            long min = 0;
            long max = Long.valueOf(values[0]);
            if (values.length > 1) {
                min = max;
                max = Long.valueOf(values[1]);
            }

            if (min < 0 || max < min) {
                throw new IllegalArgumentException("Illegal range '" + range + "'");
            }

            return getSource().longs(min, max).findFirst().getAsLong();
        }
        return null;
    }
}
