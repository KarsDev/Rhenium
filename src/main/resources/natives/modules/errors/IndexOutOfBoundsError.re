/*
  IndexOutOfBoundsError is raised when an index is used to access
  a collection element that does not exist.

  Example:
    value = array.get(10)

  when array contains fewer than 11 elements.
*/
struct IndexOutOfBoundsError inherits Error:
    // The index that was requested
    index: int

    // The size of the collection being accessed
    size: int

impl IndexOutOfBoundsError:
    init(index: int, size: int):
        this.index = index
        this.size = size

    func message() -> str: // override
        return "Index out of bounds: index " + this.index + " for size " + this.size