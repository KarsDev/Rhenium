using file
using FileWriter

func main() -> int:

    file = init File("test.txt")

    file.createNew()

    fw = init FileWriter(file)

    fw.open()

    fw.write("Hello World!")

    fw.flush()
    fw.close()

    return 0