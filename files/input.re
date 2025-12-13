using time

func work() -> int:
    s: mut int = 0
    for i in range(1_000_000_000):
        s = s + 1
    return s

func main() -> int:
    start = timeMillis()

    res = work()

    took = timeMillis() - start

    println("res=" + res)
    println("took=" + took + "ms")

    return 0