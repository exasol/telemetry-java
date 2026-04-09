package com.exasol.telemetry;

interface TelemetryEnvironment
{
    String getenv(String name);

    final class SystemEnvironment
            implements TelemetryEnvironment
    {
        static final SystemEnvironment INSTANCE = new SystemEnvironment();

        private SystemEnvironment() {}

        @Override
        public String getenv(String name)
        {
            return System.getenv(name);
        }
    }
}
