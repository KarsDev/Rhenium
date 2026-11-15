using math

generic func check<T>(lbl: str, actual: T, expected: str) -> int:
    got = "" + actual
    if got == expected:
        return 0
    println("FAIL  at expr: " + lbl)
    println("      why: expected " + expected + ", got " + got)
    return 1

func main() -> int:
    failed: mut = 0

    println("--- arithmetic ---")
    failed += check("7 + 3", 7 + 3, "10")
    failed += check("7 - 3", 7 - 3, "4")
    failed += check("7 * 3", 7 * 3, "21")
    failed += check("7 / 2", 7 / 2, "3")
    failed += check("8 / 2", 8 / 2, "4")
    failed += check("7 % 3", 7 % 3, "1")
    failed += check("-7 % 3", -7 % 3, "-1")
    failed += check("7 % -3", 7 % -3, "1")
    failed += check("-7 % -3", -7 % -3, "-1")
    failed += check("0 - 0", 0 - 0, "0")

    println("--- unary ---")
    a = 5
    failed += check("+a", +a, "5")
    failed += check("-a", -a, "-5")
    failed += check("- -a", - -a, "5")
    failed += check("~a", ~a, "-6")
    failed += check("~0", ~0, "-1")
    failed += check("~-1", ~-1, "0")
    failed += check("not true", not true, "false")
    failed += check("not false", not false, "true")
    failed += check("not 0", not 0, "true")
    failed += check("not 5", not 5, "false")

    println("--- comparison ---")
    failed += check("3 == 3", 3 == 3, "true")
    failed += check("3 == 4", 3 == 4, "false")
    failed += check("3 != 4", 3 != 4, "true")
    failed += check("3 != 3", 3 != 3, "false")
    failed += check("3 < 4", 3 < 4, "true")
    failed += check("4 < 3", 4 < 3, "false")
    failed += check("3 < 3", 3 < 3, "false")
    failed += check("3 <= 3", 3 <= 3, "true")
    failed += check("4 <= 3", 4 <= 3, "false")
    failed += check("4 > 3", 4 > 3, "true")
    failed += check("3 > 4", 3 > 4, "false")
    failed += check("3 > 3", 3 > 3, "false")
    failed += check("3 >= 3", 3 >= 3, "true")
    failed += check("3 >= 4", 3 >= 4, "false")
    failed += check("-1 < 0", -1 < 0, "true")
    failed += check("1.5 < 2", 1.5 < 2, "true")
    failed += check("2 == 2.0", 2 == 2.0, "true")

    println("--- logical ---")
    failed += check("true and true", true and true, "true")
    failed += check("true and false", true and false, "false")
    failed += check("false and true", false and true, "false")
    failed += check("false and false", false and false, "false")
    failed += check("true or true", true or true, "true")
    failed += check("true or false", true or false, "true")
    failed += check("false or true", false or true, "true")
    failed += check("false or false", false or false, "false")
    failed += check("not (true and false)", not (true and false), "true")
    failed += check("not true or true", not true or true, "true")
    failed += check("true or false and false", true or false and false, "true")
    failed += check("(true or false) and false", (true or false) and false, "false")

    println("--- bitwise ---")
    failed += check("12 & 10", 12 & 10, "8")
    failed += check("12 | 10", 12 | 10, "14")
    failed += check("12 ^ 10", 12 ^ 10, "6")
    failed += check("~12", ~12, "-13")
    failed += check("1 << 0", 1 << 0, "1")
    failed += check("1 << 1", 1 << 1, "2")
    failed += check("1 << 10", 1 << 10, "1024")
    failed += check("3 << 4", 3 << 4, "48")
    failed += check("1024 >> 3", 1024 >> 3, "128")
    failed += check("1 >> 1", 1 >> 1, "0")
    failed += check("255 >> 4", 255 >> 4, "15")
    failed += check("-16 >> 2", -16 >> 2, "-4")
    failed += check("-1 >> 10", -1 >> 10, "-1")
    failed += check("0 & 0", 0 & 0, "0")
    failed += check("5 & 0", 5 & 0, "0")
    failed += check("5 | 0", 5 | 0, "5")
    failed += check("5 ^ 0", 5 ^ 0, "5")
    failed += check("5 ^ 5", 5 ^ 5, "0")
    failed += check("5 & 5", 5 & 5, "5")
    failed += check("5 | 5", 5 | 5, "5")
    failed += check("0xFF & 0x0F", 0xFF & 0x0F, "15")
    failed += check("0xF0 | 0x0F", 0xF0 | 0x0F, "255")
    failed += check("0xFF ^ 0x0F", 0xFF ^ 0x0F, "240")
    failed += check("-1 & 0xFF", -1 & 0xFF, "255")
    failed += check("-8 | 3", -8 | 3, "-5")
    failed += check("-8 ^ 3", -8 ^ 3, "-5")
    failed += check("6 & -2", 6 & -2, "6")
    failed += check("0b1010 & 0b0110", 0b1010 & 0b0110, "2")
    failed += check("0b1010 | 0b0110", 0b1010 | 0b0110, "14")
    failed += check("0b1010 ^ 0b0110", 0b1010 ^ 0b0110, "12")
    failed += check("1L << 62", 1L << 62, "4611686018427387904")
    failed += check("true & false", true & false, "false")
    failed += check("true | false", true | false, "true")
    failed += check("true ^ true", true ^ true, "false")

    failed += check("1 | 2 ^ 3 & 4", 1 | 2 ^ 3 & 4, "3")
    failed += check("(1 | 2) ^ (3 & 4)", (1 | 2) ^ (3 & 4), "3")
    failed += check("1 | 6 & 3", 1 | 6 & 3, "3")
    failed += check("2 + 3 << 1", 2 + 3 << 1, "10")
    failed += check("1 << 2 + 1", 1 << 2 + 1, "8")
    failed += check("5 & 3 == 3", 5 & 3 == 3, "false")
    failed += check("(5 & 3) == 3", (5 & 3) == 3, "false")
    failed += check("(5 & 3) == 1", (5 & 3) == 1, "true")

    println("--- assignment ---")
    x: mut = 10
    failed += check("x = 10", x, "10")
    x += 5
    failed += check("x += 5", x, "15")
    x -= 3
    failed += check("x -= 3", x, "12")
    x *= 2
    failed += check("x *= 2", x, "24")
    x %= 10
    failed += check("x %= 10", x, "4")
    x /= 2
    failed += check("x /= 2", x, "2")

    b: mut = 0b1100
    b &= 0b1010
    failed += check("b &= 0b1010", b, "8")
    b |= 0b0001
    failed += check("b |= 0b0001", b, "9")
    b ^= 0b1111
    failed += check("b ^= 0b1111", b, "6")
    b <<= 3
    failed += check("b <<= 3", b, "48")
    b >>= 2
    failed += check("b >>= 2", b, "12")

    p = q = 7
    failed += check("p = q = 7 (p)", p, "7")
    failed += check("p = q = 7 (q)", q, "7")

    println("--- string ---")
    failed += check("\"ab\" + \"cd\"", "ab" + "cd", "abcd")
    s: mut = "hi"
    s += "!"
    failed += check("s += \"!\"", s, "hi!")

    println("--- ternary ---")
    failed += check("1 if true else 2", 1 if true else 2, "1")
    failed += check("1 if false else 2", 1 if false else 2, "2")
    failed += check("\"big\" if 10 > 5 else \"small\"", "big" if 10 > 5 else "small", "big")
    failed += check("1 if false else 2 if false else 3", 1 if false else 2 if false else 3, "3")

    println("--- precedence ---")
    failed += check("2 + 3 * 4", 2 + 3 * 4, "14")
    failed += check("(2 + 3) * 4", (2 + 3) * 4, "20")
    failed += check("10 - 4 - 3", 10 - 4 - 3, "3")
    failed += check("100 / 10 / 5", 100 / 10 / 5, "2")
    failed += check("2 * 3 % 4", 2 * 3 % 4, "2")
    failed += check("not 1 == 2", not 1 == 2, "true")
    failed += check("1 + 2 == 3 and 4 > 3", 1 + 2 == 3 and 4 > 3, "true")
    failed += check("1 < 2 and 2 < 3 or false", 1 < 2 and 2 < 3 or false, "true")
    failed += check("~1 + 1", ~1 + 1, "-1")
    failed += check("-~1", -~1, "2")
    failed += check("~-1", ~-1, "0")
    failed += check("1 + 2 << 3", 1 + 2 << 3, "24")
    failed += check("8 >> 1 + 1", 8 >> 1 + 1, "2")
    failed += check("6 & 3 | 8", 6 & 3 | 8, "10")
    failed += check("10 % 4 * 2", 10 % 4 * 2, "4")

    println("--- mixed types ---")
    failed += check("0.1 + 0.2 == 0.3", 0.1 + 0.2 == 0.3, "false")
    failed += check("abs(0.1 + 0.2 - 0.3) < 1e-9", Math::abs(0.1 + 0.2 - 0.3) < 1e-9, "true")

    println("--- edge cases ---")
    failed += check("0 * -1", 0 * -1, "0")
    failed += check("-0", -0, "0")
    failed += check("1L << 31", 1L << 31, "2147483648")
    failed += check("2147483647L + 1", 2147483647L + 1, "2147483648")
    failed += check("9223372036854775807L", 9223372036854775807L, "9223372036854775807")
    failed += check("-9223372036854775807L - 1", -9223372036854775807L - 1, "-9223372036854775808")
    failed += check("0xFFFFFFFF", 0xFFFFFFFF, "4294967295")
    failed += check("0xFFFFFFFF + 1", 0xFFFFFFFF + 1, "4294967296")
    failed += check("1_000_000", 1_000_000, "1000000")

    println("--- done: " + failed + " failed ---")
    return failed