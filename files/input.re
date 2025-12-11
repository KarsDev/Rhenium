using time
using string

func main() -> int:
    start = timeMillis()

    println("Starting the wait proc...")

    end: mut = timeMillis()

    while true:
        end = timeMillis()
        if end - start >= 1000:
            break

    txt = longToStr(end - start)

    println("Took " + txt + "ms")

    raise "LOL"