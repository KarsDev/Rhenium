// include the C++ functions
_NativeCPP("time") long _timeMillis() and long _timeNanos() and none _sleepMillis(millis: long) and none _sleepNanos(nanos: long)

/*
Time::millis() returns the time difference in milliseconds from UNIX up to now
Time::nanos() does the same but in nanoseconds

Time::sleep(millis) pauses the thread for <millis> milliseconds
Time::sleepNanos(nanos) does the same in nanoseconds
*/

namespace Time:
    func sleep(millis: long) -> none:
        _sleepMillis(millis)
    
    func sleepNanos(nanos: long) -> none:
        _sleepNanos(nanos)
    
    func millis() -> long:
        return _timeMillis()

    func nanos() -> long:
        return _timeNanos()
