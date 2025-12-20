#include <thread>
#include <iostream>
#include <atomic>

struct RheniumThread {
    void* (*func)(void*);
    void* arg;

    std::thread th;
    void* result;

    std::atomic<bool> started;
    std::atomic<bool> finished;

    RheniumThread() : func(nullptr), arg(nullptr), result(nullptr),
                      started(false), finished(false) {}
};

extern "C" {

void* rhenium_spawn(void* (*func)(void*), void* arg) {
    auto* handle = new RheniumThread();
    handle->func = func;
    handle->arg = arg;
    return handle;
}

void rhenium_run(void* thread_handle) {
    auto* handle = static_cast<RheniumThread*>(thread_handle);
    if (!handle) return;

    bool expected = false;
    if (!handle->started.compare_exchange_strong(expected, true, std::memory_order_acq_rel))
        return;

    if (handle->th.joinable())
        handle->th.join();

    handle->finished.store(false, std::memory_order_relaxed);
    handle->result = nullptr;

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

    handle->started.store(false, std::memory_order_release); // allow rerun
    return handle->result;
}

void rhenium_destroy(void* thread_handle) {
    auto* handle = static_cast<RheniumThread*>(thread_handle);
    if (!handle) return;

    if (handle->th.joinable())
        handle->th.join();

    delete handle;
}

}
