func parseInt(s: str) -> int:
    result: mut int = 0
    sign: mut int = 1
    start: mut int = 0

    if s[0] == '+':
        start = 1
    else if s[0] == '-':
        start = 1
        sign = -1

    for idx in range(start, len(s)):
        c: char = s[idx]
        digit = c - '0'      // fastest possible digit extraction

        if digit < 0 or digit > 9:
            break

        result = result * 10 + digit

    return result * sign
