using hash
using list

/*
  Represents a single key-value pair stored inside a HashMap.
  
  Users typically do not create Entry objects directly.
  They are used internally by HashMap buckets and returned
  by entries().
*/
generic struct Entry<K, V>:
    key: K
    value: V

impl Entry<K, V>:
    func toString() -> str:
        return "Entry[key=" + this.key + ", value=" + this.value + "]"

/*
  A hash table that stores key-value pairs.
 
  HashMap<K, V> provides efficient insertion, lookup, and removal
  of values associated with unique keys.
 
  Internally, the map uses:
  - Hashing to determine bucket placement
  - Separate chaining with List<Entry<K, V>> for collision handling
 
  Features:
  - Average O(1) insertion
  - Average O(1) lookup
  - Average O(1) removal
  - Generic over key type K and value type V
 
  Keys must support:
  - Equality comparison (==)
  - Hashing through Hash::hash()
 
  The map maintains:
  - size     : number of key-value pairs stored
  - capacity : number of buckets available
 
  Multiple keys may hash to the same bucket. These collisions are
  resolved by storing entries in a list within each bucket.
 
  Example:
  map = HashMap<str, int>()
 
  map.put("age", 25)
  map.put("score", 100)
 
  println(map.get("age"))
  println(map.containsKey("score"))
  println(map.length())
 
  Time Complexity:
  - put()          : O(1) average, O(n) worst-case
  - get()          : O(1) average, O(n) worst-case
  - remove()       : O(1) average, O(n) worst-case
  - containsKey()  : O(1) average, O(n) worst-case
  - keys()         : O(n)
  - values()       : O(n)
  - entries()      : O(n)
*/
generic struct HashMap<K, V>:
    buckets: ptr -> List<Entry<K, V>>
    capacity: int
    size: int

impl HashMap<K, V>:
    // Creates an empty map with default bucket count (5)
    init():
        this.capacity = 5
        this.size = 0
        this.buckets = init arr -> List<Entry<K, V>>(5)

        for (i in range(5)):
            this.buckets[i] = init List<Entry<K, V>>()

    // Creates an empty map with a given bucket count
    init(cap: int):
        if (cap <= 0):
            raise "Capacity must be positive: " + cap

        this.capacity = cap
        this.size = 0
        this.buckets = init arr -> List<Entry<K, V>>(cap)

        for (i in range(cap)):
            this.buckets[i] = init List<Entry<K, V>>()

    // Inserts or updates a key-value pair
    func put(key: K, value: V) -> none:
        idx: mut = (Hash::hash(key)) % this.capacity
        if (idx < 0):
            idx += this.capacity
        s = this.buckets[idx].size
        for (i in range(s)):
            k = this.buckets[idx].items[i].key
            if (k == key):
                this.buckets[idx].items[i].value = value
                return
        this.buckets[idx].add(init Entry<K, V>(key, value))
        this.size += 1

    // Retrieves the value associated with a key
    func get(key: K) -> V:
        idx: mut = Hash::hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (bucket.items[i].key == key):
                return bucket.items[i].value

        raise "Key not found: " + key

    // Returns true if the map contains the given key
    func containsKey(key: K) -> bool:
        idx: mut = Hash::hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (bucket.items[i].key == key):
                return true

        return false

    // Removes a key-value pair and returns its value
    func remove(key: K) -> V:
        idx: mut = Hash::hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (bucket.items[i].key == key):
                entry = bucket.remove(i)
                this.buckets[idx] = bucket
                this.size -= 1
                return entry.value

        this.buckets[idx] = bucket

        raise "Key not found: " + key

    // Returns the value associated with a key,
    // or the supplied value if absent
    func getOrDefault(key: K, default: V) -> V:
        idx: mut = Hash::hash(key) % this.capacity

        if (idx < 0):
            idx += this.capacity

        bucket = this.buckets[idx]

        for (i in range(bucket.size)):
            if (bucket.items[i].key == key):
                return bucket.items[i].value

        return default

    // Returns a list containing all keys in the map
    func keys() -> List<K>:
        result = init List<K>()

        for (b in range(this.capacity)):
            bucket = this.buckets[b]

            for (i in range(bucket.size)):
                result.add(bucket.items[i].key)

        return result

    // Returns a list containing all values in the map
    func values() -> List<V>:
        result = init List<V>()

        for (b in range(this.capacity)):
            bucket = this.buckets[b]

            for (i in range(bucket.size)):
                result.add(bucket.items[i].value)

        return result

    // Removes all entries from the map
    func clear() -> none:
        for (i in range(this.capacity)):
            this.buckets[i].clear()

        this.size = 0

    // Returns a list containing every key-value pair
    func entries() -> List<Entry<K, V>>:
        result = init List<Entry<K, V>>()

        for (b in range(this.capacity)):
            bucket = this.buckets[b]

            for (i in range(bucket.size)):
                result.add(bucket.items[i])

        return result
    
    // Returns true if the map contains no entries
    func isEmpty() -> bool:
        return this.size == 0

    // Returns the number of key-value pairs stored
    func length() -> int:
        return this.size