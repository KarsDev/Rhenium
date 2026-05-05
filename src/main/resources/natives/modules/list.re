using memory

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

    func resize() -> none:
        this.capacity = (this.capacity * 6) / 10
        
        newItems = init arr -> T(this.capacity)

        for (i in range(this.size)):
            newItems[i] = this.items[i]
        
        this.items = newItems
    
    func add(v: T) -> none:
        if (this.capacity == this.size):
            this.resize()

        this.items[this.size] = v

        this.size += 1

    func get(idx: int) -> T:
        if (idx < 0 or idx >= this.size):
            raise "Index out of bounds for size " + this.size
        
        return this.items[idx]