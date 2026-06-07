/*
  Represents an optional value.
  
  An Option<T> can either:
  - contain a value (Some)
  - contain no value (None)
  
  This type is useful when a value may or may not exist,
  avoiding the need for null references.
  
  Example:
  name = Option<str>("Alice")
  
  if (name.isPresent()):
      println(name.get())
  
  empty = Option<str>()
  println(empty.orElse("Unknown"))
  
  Common Uses:
  - Search results
  - Configuration values
  - Function return values that may fail
  - Nullable data
  
  Example:
  func findUser(id: int) -> Option<User>:
      ...
*/
generic struct Option<T>:
    value: ptr -> T

impl Option<T>:
    // Creates an empty Option
    init():
        this.value = null
    
    // Creates an Option containing a value
    init(value: T):
        this.value = ptr(value)

    // Returns true if a value is present
    func isPresent() -> bool:
        return cast<anyptr>(this.value) != null

    // Returns true if no value is present
    func isEmpty() -> bool:
        return not this.isPresent()

    // Gets the contained value, raising an exception if the Option is empty
    func get() -> T:
        if (not this.isPresent()):
            raise "Option is empty"
        
        return @(this.value)
    
    // Returns the contained value or 'other' if the Option is empty
    func orElse(other: T) -> T:
        return @(this.value) if this.isPresent() else other
    
    // Returns the contained value or raises an exception with the message if empty
    func expect(message: str) -> T:
        if (this.isEmpty()):
            raise message
        return this.get()
    
    // Return true if the Option contains the specified value
    func contains(value: T) -> bool:
        return this.isPresent() and this.get() == value