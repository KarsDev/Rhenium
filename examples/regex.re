using regex

func main() -> int:
    regex = Regex::compile("(hello)")

    if (regex.matches("hello")):
        println("matches: PASS")
    else:
        println("matches: FAIL")

    if (regex.search("say hello world")):
        println("search: PASS")
    else:
        println("search: FAIL")

    found = regex.find("say hello world")

    if (found.isPresent()):
        println("find: PASS")
        println("find result: " + found.get())
    else:
        println("find: FAIL")

    all = regex.findAll("hello hello hello")

    println("findAll count: " + all.size)

    if (all.size == 3):
        println("findAll: PASS")
    else:
        println("findAll: FAIL")

    count = regex.count("hello hello hello")

    println("count: " + count)

    if (count == 3):
        println("count: PASS")
    else:
        println("count: FAIL")

    groups = regex.groups("say hello world")

    println("groups count: " + groups.size)

    if (groups.size == 1):
        println("groups: PASS")
        println("group 1: " + groups.values[0])
    else:
        println("groups: FAIL")

    splitRegex = Regex::compile("\\s+")

    parts = splitRegex.split("one two three")

    println("split count: " + parts.size)

    if (parts.size == 3):
        println("split: PASS")
        println("split[0]: " + parts.values[0])
        println("split[1]: " + parts.values[1])
        println("split[2]: " + parts.values[2])
    else:
        println("split: FAIL")

    replaceRegex = Regex::compile("hello")

    replaced = replaceRegex.replace("hello world hello", "hi")

    println("replace result: " + replaced)

    if (replaced == "hi world hi"):
        println("replace: PASS")
    else:
        println("replace: FAIL")

    delete regex
    delete splitRegex
    delete replaceRegex

    println("delete: PASS")

    return 0