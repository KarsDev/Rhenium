using file

func main() -> int:

    f = init File("TEST.txt")

    if (f.exists()):
        println("EXISTS")
    else:
        println("DOES NOT EXIST")

    f.createNew()

    f.rename("TEST_2.XT")

    println(f.name)

    return 0