package de.comci.cube;

import java.util.Calendar;
import java.util.Date;

/**
 * Definition of TimeUnits, besides the usual suspects this enum provides
 * quarter and (iso-)week as two additional units of time.
 * 
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public enum TimeUnit {
    
    YEAR(Calendar.YEAR),
    QUARTER(Calendar.MONTH),
    MONTH(Calendar.MONTH),
    WEEK(Calendar.WEEK_OF_YEAR),
    DAY(Calendar.DAY_OF_MONTH),
    HOUR(Calendar.HOUR_OF_DAY),
    MINUTE(Calendar.MINUTE),
    SECOND(Calendar.SECOND);
    
    private final int calendarField;
    
    TimeUnit(int calendarField) {
        this.calendarField = calendarField;
    }
        
    /**
     * Get the value according to the current time unit from the given 
     * {@link Date} object.
     * 
     * @param date object to extract the time unit value from
     * @return the value according to the current time unit. The value for
     * quarter will be calculated based upon the month value. All others are 
     * simple calls to {@link Calendar#get(int)}.
     */
    public short get(Date date) {        
        Calendar c = Calendar.getInstance();
        // set 'first day of' and 'minimal days in first' week to receive a
        // valid iso 8601 week number.
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setMinimalDaysInFirstWeek(4);
        c.setTime(date);        
        switch (this) {
            // quarter needs to be calculated as there is no support for
            // it in the Calendar object
            case QUARTER:
                return (short)Math.ceil((c.get(calendarField) + 1.0) / 3.0);
            default:
                return (short)c.get(calendarField);
        }        
    }
        
}
