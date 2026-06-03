_NativeCPP("hash") int HashCode(data: anyptr, length: long) and bool HashCodeEquals(aData: anyptr, aLength: long, bData: anyptr, bLength: long)

namespace Hash:

    // Returns the hash code of an object
    generic func hash<T>(obj: T) -> int:
        data = cast<anyptr>(ptr(obj))
        length = sizeof(obj)
        return HashCode(data, length)

    // Returns the equality of two objects hash code
    generic func eq<T>(a: T, b: T) -> bool:
        aData = ptr(a)
        aLength = sizeof(a)
        bData = ptr(b)
        bLength = sizeof(b)

        return HashCodeEquals(aData, aLength, bData, bLength)
     