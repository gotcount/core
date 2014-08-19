/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.parser.apache;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Sebastian
 */
public class ApacheParser {

    private static final Map<String, Pattern> matchers = new HashMap<>();
    private static final SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

    static {
        matchers.put("common", Pattern.compile("([^ ]+) ([^ ]+) ([^ ]+) \\[(.+)\\] \"(([A-Z]+) )?([^ ]*) ?(.+)?\" (\\d+) (\\d+|-)"));
    }

    public static ApacheLogRecord parse(String line) {
        return parse(line, "common");
    }

    public static ApacheLogRecord parse(String line, String schema) {
        return parse(line, schema, 1800);
    }

    public static ApacheLogRecord parse(String line, String schema, int sessionTimeout) {
        return (new ApacheParser(schema, sessionTimeout)).parser(line, matchers.get(schema));
    }

    public static Stream<ApacheLogRecord> parse(File file) throws FileNotFoundException {
        return parse(file, "common");
    }

    public static Stream<ApacheLogRecord> parse(File file, String schema) throws FileNotFoundException {
        return (new ApacheParser(schema)).parseFile(file);
    }

    private final String schema;
    private final int sessionTimeout;

    private ApacheParser(String schema) {
        this(schema, 1800);
    }

    private ApacheParser(String schema, int sessionTimeout) {
        this.schema = schema;
        this.sessionTimeout = sessionTimeout;
    }

    private ApacheLogRecord parseLine(String line) {
        return parser(line, matchers.get(schema));
    }

    private Stream<ApacheLogRecord> parseFile(File file) throws FileNotFoundException {
        if (!matchers.containsKey(schema)) {
            throw new IllegalArgumentException("no parser for this schema found");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("cannot access file");
        }
        final BufferedReader br = new BufferedReader(new FileReader(file));
        final Pattern p = matchers.get(schema);

        return br.lines().map(l -> parseLine(l));
    }

    private ApacheLogRecord parser(String line, Pattern p) {
        Matcher m = p.matcher(line);
        try {
            if (m.find()) {
                final String client = m.group(1);
                final Date date = df.parse(m.group(4));

                return new ApacheLogRecord(
                        client,
                        m.group(2),
                        m.group(3),
                        date,
                        m.group(6),
                        m.group(7),
                        m.group(8),
                        Short.parseShort(m.group(9)),
                        (m.group(10).equals("-")) ? -1 : Integer.parseInt(m.group(10)),
                        null,
                        null);

            } else {
                throw new IllegalArgumentException(String.format("could not parse '%s'", line));
            }
        } catch (ParseException e) {
            System.out.println(m.group(3));
            throw new RuntimeException(e);
        }
    }
    
}
