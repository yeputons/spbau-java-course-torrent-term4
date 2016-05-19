package net.yeputons.spbau.spring2016.torrent;

import ch.qos.logback.core.PropertyDefinerBase;

import java.lang.management.ManagementFactory;

public class LogbackPidPropertyDefiner extends PropertyDefinerBase {
    @Override
    public String getPropertyValue() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
