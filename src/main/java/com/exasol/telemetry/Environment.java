package com.exasol.telemetry;

interface Environment {
    String getenv(String name);

    @SuppressWarnings("java:S6548") // It's OK to use a singleton here
    final class SystemEnvironment implements Environment {
        static final SystemEnvironment INSTANCE = new SystemEnvironment();

        private SystemEnvironment() {
        }

        @Override
        public String getenv(final String name) {
            return System.getenv(name);
        }
    }
}
