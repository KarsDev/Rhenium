generic struct List<T>:
    items: ptr -> T
    size: int
    capacity: int

impl List<T>:
    init(capacity: mut int):
        if (capacity <= 0):
            raise "Capacity must be positive"
        this.size = 0
        this.capacity = capacity
        this.items = init arr -> T(capacity)

    init():
        this.size = 0
        this.capacity = 5
        this.items = init arr -> T(this.capacity)

    func resize() -> none:
        this.capacity *= 2
        newItems = init arr -> T(this.capacity)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems

    func add(v: T) -> none:
        if (this.size == this.capacity):
            this.resize()
        this.items[this.size] = v
        this.size += 1

    func get(idx: int) -> T:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds for size " + this.size
        return this.items[idx]


    func set(idx: int, v: T) -> none:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds"
        this.items[idx] = v

    func insert(idx: int, v: T) -> none:
        if (idx < 0 or idx > this.size):
            raise "Index out of bounds"
        if (this.size == this.capacity):
            this.resize()
        for (i in range(this.size, idx, -1)):
            this.items[i] = this.items[i - 1]
        this.items[idx] = v
        this.size += 1


    func remove(idx: int) -> T:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds"
        val = this.items[idx]
        for (i in range(idx, this.size - 1)):
            this.items[i] = this.items[i + 1]
        this.size -= 1
        return val

    func pop() -> T:
        if (this.size == 0):
            raise "List is empty"

        this.size -= 1
        return this.items[this.size]

    func contains(v: T) -> bool:
        for (i in range(this.size)):
            if (this.items[i] == v):
                return true
        return false


    func indexOf(v: T) -> int:
        for (i in range(this.size)):
            if (this.items[i] == v):
                return i
        return -1

    func clear() -> none:
        this.size = 0

    func isEmpty() -> bool:
        return this.size == 0

    func length() -> int:
        return this.size

    func reserve(newCap: int) -> none:
        if (newCap <= this.capacity):
            return
        newItems = init arr -> T(newCap)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems
        this.capacity = newCap

    func shrinkToFit() -> none:
        if (this.size == this.capacity):
            return
        newItems = init arr -> T(this.size)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems
        this.capacity = this.size