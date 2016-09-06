package com.vaa25.gitsum;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by vaa25 on 05.09.16.
 */
public final class Common {

    public void print(final Map<?, Duration> durationByDate) {
        for (Map.Entry<?, Duration> entry : durationByDate.entrySet()) {
            final long minutes = entry.getValue().toMinutes();
            final long hours = minutes / 60;
            System.out.println(entry.getKey().toString() + '\t' + hours + "h " + (minutes - hours * 60) + 'm');
        }
    }

    public Duration duration(final String body) {
        final Duration duration = Arrays.stream(body.split("\\n"))
            .filter(this::hasCash)
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .filter(time -> hasTime(time.trim()))
            .map(this::parseDuration)
            .reduce(Duration::plus)
            .orElseThrow(() -> new RuntimeException(body));
        return duration;
    }

    private Duration parseDuration(String time) {
        int hours;
        if (time.contains("h")) {
            hours = Integer.valueOf(time.substring(0, time.indexOf("h")));
            time = time.substring(time.indexOf("h") + 1);
        } else {
            hours = 0;
        }
        int minutes;
        if (time.contains("m")) {
            minutes = Integer.valueOf(time.substring(0, time.indexOf("m")));
        } else {
            minutes = 0;
        }
        return Duration.ofHours(hours).plusMinutes(minutes);
    }

    private boolean hasTime(final String s) {
        return s.matches("\\d??\\d??h") || s.matches("\\d??\\d??m") || s.matches("\\d??\\d??h\\d??\\d??m");
    }

    public boolean hasCash(final String body) {
        return body.contains("@Bizon4ik") && body.contains("spent");
    }
}
