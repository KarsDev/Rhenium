#include <chrono>
#include <thread>
#include <ctime>
#include <cstdint>

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


    int64_t __getLocalDate() {
        std::time_t currentTime = std::time(nullptr);
        std::tm* now = std::localtime(&currentTime);

        uint16_t year = static_cast<uint16_t>(now->tm_year + 1900);
        uint8_t month = static_cast<uint8_t>(now->tm_mon + 1);
        uint8_t day = static_cast<uint8_t>(now->tm_mday);

        return static_cast<uint16_t>(year) | (static_cast<int64_t>(month) << 16) | (static_cast<int64_t>(day) << 24);
    }
}