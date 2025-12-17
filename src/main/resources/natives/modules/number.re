// Parse integer from a string
func parseInt(s: str) -> int:
    result: mut = 0
    sign: mut = 1
    start: mut = 0

    if (s[0] == '+'):
        start = 1
    else if (s[0] == '-'):
        start = 1
        sign = -1

    for (idx in range(start, len(s))):
        c = s[idx]
        digit = c - '0'

        if (digit < 0 or digit > 9):
            break

        result = result * 10 + digit

    return result * sign

// Parse a double (floating point) number from a string
func parseDouble(s: str) -> double:
    result: mut = 0.0
    sign: mut = 1.0
    start: mut = 0
    fraction: mut = 0.0
    divisor: mut = 10.0
    in_fraction: mut = false

    if (s[0] == '+'):
        start = 1
    else if (s[0] == '-'):
        start = 1
        sign = -1.0

    for (idx in range(start, len(s))):
        c = s[idx]

        if (c == '.'):
            in_fraction = true
            continue

        digit = c - '0'
        if (digit < 0 or digit > 9):
            break

        if (in_fraction):
            fraction += digit / divisor
            divisor *= 10
        else:
            result = result * 10 + digit

    return (result + fraction) * sign

func parseFloat(s: str) -> float:
    return cast<float>(parseDouble(s))

// Parse a long integer from a string
func parseLong(s: str) -> long:
    result: mut long = 0
    sign: mut = 1
    start: mut = 0

    if (s[0] == '+'):
        start = 1
    else if (s[0] == '-'):
        start = 1
        sign = -1

    for (idx in range(start, len(s))):
        c = s[idx]
        digit = c - '0'

        if (digit < 0 or digit > 9):
            break

        result = result * 10 + digit

    return result * sign

// Parse a byte (0-255 or -128 to 127) from string
func parseByte(s: str) -> byte:
    val = parseInt(s)
    return cast<byte>(val & 0xFF)  // clamp to byte range

// Parse a long integer with a custom base (2..36)
func parseLongBase(s: str, base: int) -> long:
    result: mut long = 0
    sign: mut = 1
    start: mut = 0

    if (s[0] == '+'):
        start = 1
    else if (s[0] == '-'):
        start = 1
        sign = -1

    for (idx in range(start, len(s))):
        c = s[idx]
        digit: mut = 0

        if ('0' <= c and c <= '9'):
            digit = c - '0'
        else if ('a' <= c and c <= 'z'):
            digit = c - 'a' + 10
        else if ('A' <= c and c <= 'Z'):
            digit = c - 'A' + 10
        else:
            break

        if (digit >= base):
            break

        result = result * base + digit

    return result * sign
