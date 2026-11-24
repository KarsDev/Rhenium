using clock.time
using errors.IllegalArgumentError

// The Random module can be used to generate pseudorandom numbers
// This uses SplitMix64
struct Random:
    // Pseudo Random seed
    seed: mut long

namespace RandomUtil:
    // Combines high-resolution nanotime with standard millisecond time
    // to create a unique seed value.
    func randomSeed() -> long:
        return Time::millis() + Time::nanos()

impl Random:
    init():
        this.seed = RandomUtil::randomSeed()

    init(seed: long):
        this.seed = seed

    // Generates a pseudorandom 64-bit signed integer
    func nextLong() -> long:
        this.seed += 0x9E3779B97F4A7C15

        z: mut long = this.seed

        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EB
        z = z ^ (z >>> 31)

        return z

    // Generates a pseudorandom 64-bit integer in a range
    func nextLong(min: long, max: long) -> long:
        if min > max:
            raise init IllegalArgumentError("min cannot be greater than max")

        // Special case: complete signed 64-bit range
        if min == -9223372036854775808 and max == 9223372036854775807:
            return this.nextLong()

        delta: long = max - min + 1

        if delta <= 0:
            raise init IllegalArgumentError("Range is too large")

        raw: long = this.nextLong() & 0x7FFFFFFFFFFFFFFF
        offset: long = raw % delta
        return min + offset

    // Generates a pseudorandom 32-bit signed integer
    func nextInt() -> int:
        return cast<int>(this.nextLong())

    // Generates a pseudorandom 32-bit integer in a range
    func nextInt(min: int, max: int) -> int:
        if min > max:
            raise init IllegalArgumentError("min cannot be greater than max")

        raw: long = this.nextLong() & 0x7FFFFFFFFFFFFFFF
        delta: long = max - min + 1
        offset: long = raw % delta
        return cast<int>(cast<long>(min) + offset)

    // Generates a pseudorandom 32-bit integer from 0..max
    func nextInt(max: int) -> int:
        return this.nextInt(0, max)

    // Returns a pseudorandom bool
    func nextBool() -> bool:
        return (this.nextLong() & 1) == 0

    // Returns a pseudorandom float in [0, 1)
    func nextFloat() -> float:
        return cast<float>(this.nextLong() >>> 40) / 16777216f

    // Returns a pseudorandom double in [0, 1)
    func nextDouble() -> double:
        return cast<double>(this.nextLong() >>> 11) / 9007199254740992d

    // Returns a pseudorandom float in a range
    func nextFloat(min: float, max: float) -> float:
        if min > max:
            raise init IllegalArgumentError("min cannot be greater than max")

        return min + this.nextFloat() * (max - min)

    // Returns a pseudorandom double in a range
    func nextDouble(min: double, max: double) -> double:
        if min > max:
            raise init IllegalArgumentError("min cannot be greater than max")

        return min + this.nextDouble() * (max - min)

    // Returns true with the given probability from 0.0 to 1.0
    func chance(probability: double) -> bool:
        if probability < 0.0 or probability > 1.0:
            raise init IllegalArgumentError("probability must be between 0.0 and 1.0")

        return this.nextDouble() < probability