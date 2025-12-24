using time

/*
Random module
This struct can be used to generate pseudorandom numbers
*/
struct Random:
    // Pseudo Random seed
    // We initialize it using our new helper function
    seed: long = _randomSeed()

    // LCG Constants (Standard glibc values)
    _a: int = 1103515245
    _c: int = 12345
    _m: int = 2147483647 // 2^31 - 1

// Combines high-resolution nanotime with standard millisecond time
// to create a unique seed value.
func _randomSeed() -> long:
    return timeNanos() + timeMillis()

impl Random:
    // Generates a pseudorandom 64-bit integer 
    func nextLong() -> long:
        // seed = (a * seed + c) % m
        (@self).seed = ((@self)._a * (@self).seed + (@self)._c) % (@self)._m
    
        if ((@self).seed < 0):
            (@self).seed *= -1
        return (@self).seed

    // Generates a pseudorandom 32-bit integer 
    func nextInt() -> int:
        return cast<int>((@self).nextLong())

    // Returns a pseudorandom number within a range
    func nextInt(min: int, max: int) -> int:
        raw: int = (@self).nextInt()
        delta: long = max - min + 1
        offset: int = raw % delta
        return min + offset

    // Returns a pseudorandom bool
    func nextBool() -> bool:
        // Fixed function name match (rangeInt)
        val: int = (@self).nextInt(0, 1)
        return val == 0