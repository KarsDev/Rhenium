using File
using FileWriter

func main() -> int:
    input = init File("file_input.txt") 

    input.createNew()

    writer = init FileWriter(input)

    writer.open()

    writer.write("hmmmmm")

    writer.flush()
    writer.close()

    reader = init FileReader(input)

    reader.open()

    line = reader.readLine()

    reader.close()

    println(line)

    input.delete()

    return 0