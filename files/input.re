func main() -> int:
    alpha = input("Write true or anything else ")

    beta = 1 if alpha == "true" else 2

    println("1 if true, 2 otherwise:   " + beta)

    return beta