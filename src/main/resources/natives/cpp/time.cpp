#include <chrono>
#include <thread>

extern "C" {
    long timeMillis() {
        return (long)std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }

    long timeNanos() {
        return (long)std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
    }

    void sleepMillis(long millis) {
        if (millis > 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(millis));
        }
    }

    void sleepNanos(long nanos) {
        if (nanos > 0) {
            std::this_thread::sleep_for(std::chrono::nanoseconds(nanos));
        }
    }
}