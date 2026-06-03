#include <chrono>
#include <thread>

extern "C" {
    long _timeMillis() {
        return (long)std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }

    long _timeNanos() {
        return (long)std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }

    void _sleepMillis(long millis) {
        if (millis > 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(millis));
        }
    }

    void _sleepNanos(long nanos) {
        if (nanos > 0) {
            std::this_thread::sleep_for(std::chrono::nanoseconds(nanos));
        }
    }
}