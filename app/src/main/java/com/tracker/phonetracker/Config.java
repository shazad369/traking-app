package com.tracker.phonetracker;

public class Config {
    // ⚠️ এখানে তোমার Linux computer-এর IP দাও
    public static final String SERVER_IP = "YOUR_LINUX_IP_HERE";
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/track";
}
