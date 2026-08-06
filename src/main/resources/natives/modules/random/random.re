using clock.time
using errors.IllegalArgumentError

// The Random module can be used to generate pseudorandom numbers
struct Random:
    // Pseudo Random seed
    seed: long

    // LCG Constants
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
        this.setConstants()

    init(seed: long):
        this.seed = seed
        this.setConstants()

    init(seed: long, _a: int, _c: int, _m: int):
        raise init IllegalArgumentError("Default constructor is not usable for Random")

    func setConstants() -> none:
        this._a = 1103515245
        this._c = 12345
        this._m = 2147483647 // 2^31 - 1

    // Generates a pseudorandom 64-bit integer
    func nextLong() -> long:
        this.seed = (this._a * this.seed + this._c) % this._m

        if (this.seed < 0):
            this.seed *= -1
        return this.seed

    // Generates a pseudorandom 64-bit integer in a range
    func nextLong(min: long, max: long) -> long:
        if (min > max):
            raise init IllegalArgumentError("min cannot be greater than max")

        raw: long = this.nextLong()
        delta: long = max - min + 1
        offset: long = raw % delta
        return min + offset

    // Generates a pseudorandom 32-bit integer
    func nextInt() -> int:
        return cast<int>(this.nextLong())

    // Generates a pseudorandom 32-bit integer in a range
    func nextInt(min: int, max: int) -> int:
        if (min > max):
            raise init IllegalArgumentError("min cannot be greater than max")

        raw: int = this.nextInt()
        delta: long = max - min + 1
        offset: int = raw % delta
        return min + offset

    // Generates a pseudorandom 32-bit integer from 0..max
    func nextInt(max: int) -> int:
        return this.nextInt(0, max)

    // Returns a pseudorandom bool
    func nextBool() -> bool:
        return this.nextInt(0, 1) == 0

    // Returns a pseudorandom float in [0, 1)
    func nextFloat() -> float:
        return cast<float>(this.nextLong()) / cast<float>(this._m)

    // Returns a pseudorandom double in [0, 1)
    func nextDouble() -> double:
        return cast<double>(this.nextLong()) / cast<double>(this._m)

    // Returns a pseudorandom float in a range
    func nextFloat(min: float, max: float) -> float:
        if (min > max):
            raise init IllegalArgumentError("min cannot be greater than max")

        return min + (this.nextFloat() * (max - min))

    // Returns a pseudorandom double in a range
    func nextDouble(min: double, max: double) -> double:
        if (min > max):
            raise init IllegalArgumentError("min cannot be greater than max")

        return min + (this.nextDouble() * (max - min))

    // Returns true with the given probability from 0.0 to 1.0
    func chance(probability: double) -> bool:
        if (probability < 0.0 or probability > 1.0):
            raise init IllegalArgumentError("probability must be between 0.0 and 1.0")

        return this.nextDouble() < probability