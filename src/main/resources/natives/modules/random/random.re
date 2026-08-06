using clock.time

/*
Random module
This struct can be used to generate pseudorandom numbers
*/
struct Random:
    // Pseudo Random seed
    // We initialize it using our new helper function
    seed: long

    // LCG Constants (Standard glibc values)
    _a: int
    _c: int
    _m: int

// Combines high-resolution nanotime with standard millisecond time
// to create a unique seed value.
func _randomSeed() -> long:
    return Time::millis() + Time::nanos()

impl Random:
    init():
        this.seed = _randomSeed()
        this._a = 1103515245
        this._c = 12345
        this._m = 2147483647 // 2^31 - 1
    
    // Generates a pseudorandom 64-bit integer 
    func nextLong() -> long:
        // seed = (a * seed + c) % m
        this.seed = (this._a * this.seed + this._c) % this._m
    
        if (this.seed < 0):
            this.seed *= -1
        return this.seed

    // Generates a pseudorandom 32-bit integer 
    func nextInt() -> int:
        return cast<int>(this.nextLong())

    // Returns a pseudorandom number within a range
    func nextInt(min: int, max: int) -> int:
        raw: int = this.nextInt()
        delta: long = max - min + 1
        offset: int = raw % delta
        return min + offset

    // Returns a pseudorandom bool
    func nextBool() -> bool:
        // Fixed function name match (rangeInt)
        return this.nextInt(0, 1) == 0