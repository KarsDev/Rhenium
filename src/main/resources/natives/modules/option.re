generic struct Option<T>:
    value: ptr -> T

impl Option<T>:
    init():
        this.value = null
    
    init(value: T):
        this.value = ptr(value)

    func isPresent() -> bool:
        return cast<anyptr>(this.value) != null

    func isEmpty() -> bool:
        return not this.isPresent()

    func get() -> T:
        if (not this.isPresent()):
            raise "Option is empty"
        
        return @(this.value)
    
    func orElse(other: T) -> T:
        return @(this.value) if this.isPresent() else other
    
    func expect(message: string) -> T:
        if (this.isEmpty()):
            raise message
        return this.get()
    
    func contains(value: T) -> bool:
        return this.isPresent() and this.get() == value