using string

struct Person:
    name: str
    age: int
    active: bool

impl Person:
    func toString() -> str:
        return this.name + ", " + this.age + " yo, " + ("active" if this.active else "inactive")

func create_person(name: str, age: int) -> Person:
    return init Person(name, age, true)

func main() -> int:
    x: mut int = 10
    y: ptr -> int = ptr(x)

    arr_numbers: arr -> int = [1, 2, 3, 4, 5]
    arr_strings: arr -> str = ["a", "b", "c"]

    person1: Person = create_person("Alice", 30)
    person2: Person = init Person("Bob", 25, false)

    s = person1.toString()

    if (s != "Alice, 30 yo, active"):
        println("error with toString in main 1")

    for (i in range(0, 5)):
        if (arr_numbers[i] % 2 == 0):
            continue
        else:
            break

    while (x > 0):
        @y = @y - 1
        x = x - 1
        if (x == 5):
            break

    arr_numbers[0] = 42
    arr_strings[1] = "z"

    arr_numbers[2] = arr_numbers[2] + 10

    return main2()


generic struct Box<T>:
    inner: T

func main2() -> int:
    alpha = init Box<int>(15)
    alpha_ptr = ptr(alpha)

    beta = @alpha_ptr
    gamma = beta.inner

    if (gamma != 15):
        println("error with gamma in main 2")

    return main3()

struct IntBox:
    inner: int

func main3() -> int:
    v = init IntBox(5)

    vp = ptr(v)

    i = (@vp).inner

    ip = ptr(i)

    @ip = 10

    z = @ip

    if (z != 10):
        println("error with z in main 3")

    if (i != 10):
        println("error with i in main 3")
        println("i=" + i)

    return 0