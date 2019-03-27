package com.alperez.esp32.netspeed_client;

import java.util.Locale;

/**
 * Created by stanislav.perchenko on 3/26/2019
 */
public class GlobalProperties {

    /**
     * This is the Locale value using for formatting any String value within the application
     */
    public static final Locale FORMATTER_LOCALE = Locale.UK;

    public static final String BACKEND_API_URL = "https://192.168.1.1:12345";

    /********************  HTTP client-related definitions  ***************************************/
    public static final boolean HTTP_UPSTREAM_USE_GZIP = true;
    public static final String HTTP_USER_AGENT_NAME = "Avous/1.0";
}
