/*
    A dynamically-sized contiguous collection of values.

    List<T> stores elements in a resizable array and automatically grows
    its internal storage as new items are added.

    Features:
    - O(1) indexed access
    - Amortized O(1) append operations
    - Automatic capacity growth
    - Generic over any element type T

    The list maintains two separate values:
    - size     : the number of elements currently stored
    - capacity : the amount of allocated storage available

    When the list becomes full, its capacity is automatically doubled.

    Example:
    nums = List<int>()
    nums.add(10)
    nums.add(20)

    println(nums.get(0))
    println(nums.length())
*/

generic struct List<T>:
    items: ptr -> T
    size: int
    capacity: int

impl List<T>:
    // Creates a List with a given capacity
    init(capacity: mut int):
        if (capacity <= 0):
            raise "Capacity must be positive"
        this.size = 0
        this.capacity = capacity
        this.items = init arr -> T(capacity)

    // Creates a list with default capacity (5)
    init():
        this.size = 0
        this.capacity = 5
        this.items = init arr -> T(this.capacity)

    // Resizes the list, doubling the capacity
    func resize() -> none:
        this.capacity *= 2
        newItems = init arr -> T(this.capacity)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems

    // Adds an element to the list
    func add(v: T) -> none:
        if (this.size == this.capacity):
            this.resize()
        this.items[this.size] = v
        this.size += 1
    
    // Gets an element at a given index
    func get(idx: int) -> T:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds for size " + this.size
        return this.items[idx]

    // Sets an element in the list at a given index
    func set(idx: int, v: T) -> none:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds"
        this.items[idx] = v

    // Inserts an element at a given index, creating space if necessary
    func insert(idx: int, v: T) -> none:
        if (idx < 0 or idx > this.size):
            raise "Index out of bounds"
        if (this.size == this.capacity):
            this.resize()
        for (i in range(this.size, idx, -1)):
            this.items[i] = this.items[i - 1]
        this.items[idx] = v
        this.size += 1

    // Removes an element at a given index
    func remove(idx: int) -> T:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds"
        val = this.items[idx]
        for (i in range(idx, this.size - 1)):
            this.items[i] = this.items[i + 1]
        this.size -= 1
        return val

    // Removes and gets the last element from the list
    func pop() -> T:
        if (this.size == 0):
            raise "List is empty"

        this.size -= 1
        return this.items[this.size]

    // Checks whether the list contains a specific element
    func contains(v: T) -> bool:
        for (i in range(this.size)):
            if (this.items[i] == v):
                return true
        return false

    // Gets the index of an element in the list
    func indexOf(v: T) -> int:
        for (i in range(this.size)):
            if (this.items[i] == v):
                return i
        return -1

    // Clears the list, setting its size to 0
    func clear() -> none:
        this.size = 0

    // Checks whether the list is empty
    func isEmpty() -> bool:
        return this.size == 0

    // Gives the length (or size) of the list
    func length() -> int:
        return this.size

    // Ensures the list can hold at least newCap elements without resizing
    func reserve(newCap: int) -> none:
        if (newCap <= this.capacity):
            return
        newItems = init arr -> T(newCap)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems
        this.capacity = newCap

    // Reduces capacity to match the current size
    func shrinkToFit() -> none:
        if (this.size == this.capacity):
            return
        newItems = init arr -> T(this.size)
        for (i in range(this.size)):
            newItems[i] = this.items[i]
        this.items = newItems
        this.capacity = this.size