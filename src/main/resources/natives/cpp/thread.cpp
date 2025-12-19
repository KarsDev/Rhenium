#include <thread>
#include <iostream>
#include <cstdlib>

extern "C" {

struct RheniumThread {
    std::thread th;
    void* result;
};

void* rhenium_spawn(void* (*func)(void*), void* arg) {
    RheniumThread* handle = new RheniumThread();
    handle->result = nullptr;

    handle->th = std::thread([func, arg, handle]() {
        handle->result = func(arg);
    });

    return handle;
}

// Join function
void* rhenium_join(void* thread_handle) {
    RheniumThread* handle = static_cast<RheniumThread*>(thread_handle);

    if (handle->th.joinable()) {
        handle->th.join();
    }

    void* result = handle->result;

    delete handle;

    return result;
}

}
