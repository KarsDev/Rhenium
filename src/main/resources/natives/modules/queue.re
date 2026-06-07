using list

/*
A first-in, first-out (FIFO) collection.

Queue<T> stores elements in the order they were inserted.
Elements are added to the back of the queue and removed
from the front.

Internally, Queue uses a circular buffer that automatically
grows when full.

Features:
- O(1) amortized enqueue
- O(1) dequeue
- O(1) peek
- Automatic capacity growth
- Generic over any element type T

The queue maintains:
- size     : number of elements stored
- capacity : allocated storage size
- head     : index of the front element
- tail     : index where the next element will be inserted

Example:
q = Queue<str>()

q.enqueue("A")
q.enqueue("B")

println(q.dequeue()) // A
println(q.peek())    // B

Time Complexity:
- enqueue() : O(1) amortized
- dequeue() : O(1)
- peek()    : O(1)
- isEmpty() : O(1)
- length()  : O(1)
- clear()   : O(1)
- toList()  : O(n)
*/
generic struct Queue<T>:
    items: ptr -> T
    capacity: int
    size: int
    head: int
    tail: int

impl Queue<T>:
    // Creates a queue with the specified capacity
    init(capacity: mut int):
        if (capacity <= 0):
            raise "Capacity must be positive"
        this.capacity = capacity
        this.size = 0
        this.head = 0
        this.tail = 0
        this.items = init arr -> T(capacity)

    // Creates a queue with default capacity (5)
    init():
        this.capacity = 5
        this.size = 0
        this.head = 0
        this.tail = 0
        this.items = init arr -> T(this.capacity)

    // Expands the internal storage preserving the order
    func resize() -> none:
        newCapacity = this.capacity * 2
        newItems = init arr -> T(newCapacity)

        for (i in range(this.size)):
            newItems[i] = this.items[(this.head + i) % this.capacity]

        this.items = newItems
        this.capacity = newCapacity
        this.head = 0
        this.tail = this.size

    // Adds an element to the back of the queue
    func enqueue(v: T) -> none:
        if (this.size == this.capacity):
            this.resize()

        this.items[this.tail] = v
        this.tail = (this.tail + 1) % this.capacity
        this.size += 1

    // Removes and returns the front element
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

    // Returns the front element without removing it
    func peek() -> T:
        if (this.size == 0):
            raise "Queue is empty"
        return this.items[this.head]

    // Returns true if the queue contains no elements
    func isEmpty() -> bool:
        return this.size == 0

    // Returns the number of elements currently stored
    func length() -> int:
        return this.size

    // Removes all elements from the queue
    func clear() -> none:
        this.size = 0
        this.head = 0
        this.tail = 0

    // Ensures the queue can hold at least newCap elements
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

    // Returns a List containing the queue's elements
    // in dequeue order
    func toList() -> List<T>:
        result = init List<T>()

        for (i in range(this.size)):
            result.add(this.items[(this.head + i) % this.capacity])

        return result