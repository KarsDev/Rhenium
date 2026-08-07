using clock.time
using errors.IllegalArgumentError

struct SecureRandom:
    seed: mut long

impl SecureRandom:
    init():
        nanos: long = Time::nanos()
        millis: long = Time::millis()

        ns: mut long = nanos
        ns ^= millis << 21
        ns ^= millis >> 7
        ns ^= nanos << 13
        ns ^= nanos >> 17

        this.seed = ns

    init(seed: long):
        this.seed = seed

    // Generates a cryptographically-mixed pseudorandom 64-bit integer
    func nextLong() -> long:
        this.seed ^= Time::nanos()
        this.seed += Time::millis()
        this.seed ^= Time::nanos() << 17

        this.seed += -7046029254386353131

        z: mut long = this.seed

        z ^= z >> 30
        z *= -4658895280553007687

        z ^= z >> 27
        z *= -7723592293110705685

        z ^= z >> 31

        this.seed ^= z

        if (z < 0):
            z *= -1

        return z

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
        raw: mut long = this.nextLong()
        if (raw < 0):
            raw *= -1
        return cast<float>(raw) / cast<float>(9223372036854775807)

    // Returns a pseudorandom double in [0, 1)
    func nextDouble() -> double:
        raw: mut long = this.nextLong()
        if (raw < 0):
            raw *= -1
        return cast<double>(raw) / cast<double>(9223372036854775807)

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