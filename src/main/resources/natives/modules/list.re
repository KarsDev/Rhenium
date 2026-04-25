using memory

struct List:
    items: anyptr
    size: int
    capacity: int

impl List:
    init(capacity: mut int):
        if (capacity <= 0):
            capacity = 4

        this.size = 0
        this.capacity = capacity
        this.items = alloc(capacity * sizeof(anyptr))
        memset(this.items, 0, capacity * sizeof(anyptr))

    func add(item: anyptr) -> none:
        if (this.size >= this.capacity):
            this.capacity = this.capacity * 2
            this.items = realloc(this.items, this.capacity * sizeof(anyptr))

        items_arr: ptr -> anyptr = cast<ptr -> anyptr>(this.items)
        items_arr[this.size] = item
        this.size = this.size + 1

    func remove(idx: int) -> none:
        if (idx < 0 or idx >= this.size):
            return

        count = this.size - idx - 1
        if (count > 0):
            items_arr: ptr -> anyptr = cast<ptr -> anyptr>(this.items)
            memmove(ptr(items_arr[idx]), ptr(items_arr[idx + 1]), count * sizeof(anyptr))

        this.size = this.size - 1

    func remove(item: anyptr) -> none:
        items_arr: ptr -> anyptr = cast<ptr -> anyptr>(this.items)

        for (i in range(0, this.size)):
            if (items_arr[i] == item):
                this.remove(i)
                return

    func get(idx: int) -> anyptr:
        if (idx < 0 or idx >= this.size):
            return null

        items_arr: ptr -> anyptr = cast<ptr -> anyptr>(this.items)
        return items_arr[idx]

    func destroy() -> none:
        if (this.items != null):
            free(this.items)
            this.items = null
            this.size = 0
            this.capacity = 0
