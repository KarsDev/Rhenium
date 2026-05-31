#include <cstdint>

extern "C" {
    int HashCode(void* data, long size) {
        const uint8_t* bytes = static_cast<const uint8_t*>(data);

        uint32_t hash = 2166136261u;
        for (long i = 0; i < size; ++i) {
            hash ^= bytes[i];
            hash *= 16777619u;
        }

        return static_cast<int>(hash);
    }

    bool HashCodeEquals(void* aData, long aSize, void* bData, long bSize) {
        return HashCode(aData, aSize) == HashCode(bData, bSize);
    }
    
}