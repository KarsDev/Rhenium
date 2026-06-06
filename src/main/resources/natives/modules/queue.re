using list

generic struct Queue<T>:
    items: ptr -> T
    capacity: int
    size: int
    head: int
    tail: int

impl Queue<T>:
    init(capacity: mut int):
        if (capacity <= 0):
            raise "Capacity must be positive"
        this.capacity = capacity
        this.size = 0
        this.head = 0
        this.tail = 0
        this.items = init arr -> T(capacity)

    init():
        this.capacity = 5
        this.size = 0
        this.head = 0
        this.tail = 0
        this.items = init arr -> T(this.capacity)

    func resize() -> none:
        newCapacity = this.capacity * 2
        newItems = init arr -> T(newCapacity)

        for (i in range(this.size)):
            newItems[i] = this.items[(this.head + i) % this.capacity]

        this.items = newItems
        this.capacity = newCapacity
        this.head = 0
        this.tail = this.size

    func enqueue(v: T) -> none:
        if (this.size == this.capacity):
            this.resize()

        this.items[this.tail] = v
        this.tail = (this.tail + 1) % this.capacity
        this.size += 1

    func dequeue() -> T:
        if (this.size == 0):
            raise "Queue is empty"

        val = this.items[this.head]
        this.head = (this.head + 1) % this.capacity
        this.size -= 1

        if (this.size == 0):
            this.head = 0
            this.tail = 0

        return val

    func peek() -> T:
        if (this.size == 0):
            raise "Queue is empty"
        return this.items[this.head]

    func isEmpty() -> bool:
        return this.size == 0

    func length() -> int:
        return this.size

    func clear() -> none:
        this.size = 0
        this.head = 0
        this.tail = 0

    func reserve(newCap: int) -> none:
        if (newCap <= this.capacity):
            return

        newItems = init arr -> T(newCap)
        for (i in range(this.size)):
            newItems[i] = this.items[(this.head + i) % this.capacity]

        this.items = newItems
        this.capacity = newCap
        this.head = 0
        this.tail = this.size

    func toList() -> List<T>:
        result = init List<T>()

        for (i in range(this.size)):
            result.add(this.items[(this.head + i) % this.capacity])

        return result