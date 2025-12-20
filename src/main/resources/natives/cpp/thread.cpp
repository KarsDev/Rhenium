#include <thread>
#include <iostream>
#include <cstdlib>
#include <atomic>

struct RheniumThread {
    void* (*func)(void*);
    void* arg;

    std::thread th;
    void* result;

    std::atomic<bool> started;
    std::atomic<bool> finished;
};

extern "C" {

void* rhenium_spawn(void* (*func)(void*), void* arg) {
    auto* handle = new RheniumThread();
    handle->func = func;
    handle->arg = arg;
    handle->result = nullptr;
    handle->started.store(false, std::memory_order_relaxed);
    handle->finished.store(false, std::memory_order_relaxed);
    return handle;
}

void rhenium_run(void* thread_handle) {
    auto* handle = static_cast<RheniumThread*>(thread_handle);
    if (!handle) return;

    if (handle->started.exchange(true, std::memory_order_acq_rel))
        return;

    handle->th = std::thread([handle]() {
        handle->result = handle->func(handle->arg);
        handle->finished.store(true, std::memory_order_release);
    });
}

void* rhenium_await(void* thread_handle) {
    auto* handle = static_cast<RheniumThread*>(thread_handle);
    if (!handle) return nullptr;

    if (!handle->started.load(std::memory_order_acquire))
        rhenium_run(thread_handle);

    if (handle->th.joinable())
        handle->th.join();

    void* result = handle->result;
    delete handle;
    return result;
}

}
