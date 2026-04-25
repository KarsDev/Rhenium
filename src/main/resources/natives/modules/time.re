// include the C++ functions
_NativeCPP("time") long timeMillis() and long timeNanos() and none sleepMillis(millis: long) and none sleepNanos(nanos: long)

/*
timeMillis() returns the time difference in milliseconds from UNIX up to now
timeNanos() does the same but in nanoseconds

sleepMillis(millis) pauses the thread for <millis> milliseconds
sleepNanos(nanos) does the same in nanoseconds
*/