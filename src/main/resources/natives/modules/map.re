using hash
using list

generic struct Entry<K, V>:
    key: K
    value: V

impl Entry<K, V>:
    func toString() -> str:
        return "Entry[key=" + this.key + ", value=" + this.value + "]"

generic struct HashMap<K, V>:
    buckets: ptr -> List<Entry<K, V>>
    capacity: int
    size: int

impl HashMap<K, V>:

    init(cap: int):
        if (cap <= 0):
            raise "Capacity must be positive: " + cap

        this.capacity = cap
        this.size = 0
        this.buckets = init arr -> List<Entry<K, V>>(cap)

        for (i in range(cap)):
            this.buckets[i] = init List<Entry<K, V>>()

    func put(key: K, value: V) -> none:
        idx: mut = hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (eq(bucket.items[i].key, key)):
                bucket.items[i].value = value
                return

        entry = init Entry<K, V>(key, value)

        bucket.add(entry)
        this.buckets[idx] = bucket
        this.size += 1


    func get(key: K) -> V:
        idx: mut = hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (eq(bucket.items[i].key, key)):
                return bucket.items[i].value

        raise "Key not found: " + key


    func containsKey(key: K) -> bool:
        idx: mut = hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (eq(bucket.items[i].key, key)):
                return true

        return false


    func remove(key: K) -> V:
        idx: mut = hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (eq(bucket.items[i].key, key)):
                entry = bucket.remove(i)
                this.buckets[idx] = bucket
                this.size -= 1
                return entry.value

        this.buckets[idx] = bucket

        raise "Key not found: " + key

    func getOrDefault(key: K, default: V) -> V:
        idx: mut = hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (eq(bucket.items[i].key, key)):
                return bucket.items[i].value

        return default

    func keys() -> List<K>:
        result = init List<K>()

        for (b in range(this.capacity)):
            bucket = this.buckets[b]

            for (i in range(bucket.size)):
                result.add(bucket.items[i].key)

        return result


    func values() -> List<V>:
        result = init List<V>()

        for (b in range(this.capacity)):
            bucket = this.buckets[b]

            for (i in range(bucket.size)):
                result.add(bucket.items[i].value)

        return result

    func clear() -> none:
        for (i in range(this.capacity)):
            this.buckets[i].clear()

        this.size = 0


    func isEmpty() -> bool:
        return this.size == 0


    func length() -> int:
        return this.size