package de.comci.cube;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A time class allowing to the set the precision in terms of {@link  TimeUnit}.
 *
 * Instances of Time will be compared based on the given precision. In case the
 * precision of both instances is not the same the higher one will be used.
 *
 * Two Time instances will be equal when all values up to the higher precision
 * of the two instances are equal.
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class Time implements Comparable<Time> {

    private final TimeUnit precision;
    private final Date base;

    public Time(Date base, TimeUnit precision) {
        this.base = base;
        this.precision = precision;
    }
    
    @Override
    public int compareTo(Time o) {
        int result = 0,
                compareTo = (int) Math.max(this.precision.ordinal(), o.precision.ordinal());
        for (int i = 0; i <= compareTo; i++) {
            final TimeUnit unit = TimeUnit.values()[i];
            if ((result = unit.get(this.base) - unit.get(o.base)) != 0) {
                break;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        for (int i = 0; i <= this.precision.ordinal(); i++) {
            hash = 47 * hash + TimeUnit.values()[i].get(base);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return compareTo((Time) obj) == 0;
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat();
        switch (precision) {
            case YEAR:
                formatter.applyPattern("yyyy");
                break;
            case QUARTER:
                return String.format("%04d-Q%d", TimeUnit.YEAR.get(base), TimeUnit.QUARTER.get(base));
            case MONTH:
                formatter.applyPattern("yyyy-MM");
                break;
            case WEEK:
                return String.format("%04d-W%02d", TimeUnit.YEAR.get(base), TimeUnit.WEEK.get(base));
            case DAY:
                formatter.applyPattern("yyyy-MM-dd");
                break;
            case HOUR:
                formatter.applyPattern("yyyy-MM-dd:HH");
                break;
            case MINUTE:
                formatter.applyPattern("yyyy-MM-dd:HH:mm");
                break;
            case SECOND:
                formatter.applyPattern("yyyy-MM-dd:HH:mm:ss");
                break;
        }
        return formatter.format(base);
    }

}
