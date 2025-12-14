using io
using string


struct Person:
    name: str
    age: mut int
    active: bool

func create_person(name: str, age: int) -> Person:
    return init Person(name, age, true)

func main() -> int:
    x: mut int = 10
    y: ptr -> int = ptr(x)

    arr_numbers: arr -> int = [1, 2, 3, 4, 5]
    arr_strings: arr -> str = ["a", "b", "c"]

    person1: Person = create_person("Alice", 30)
    person2: Person = init Person("Bob", 25, false)

    println(person1.name)

    for (i in range(0, 5)):
        if (arr_numbers[i] % 2 == 0):
            continue
        else:
            break

    while (x > 0):
        @y = @y - 1
        x = x - 1
        if x == 5:
            break

    arr_numbers[0] = 42
    arr_strings[1] = "z"

    arr_numbers[2] = arr_numbers[2] + 10

    return 0

func sum_array(arr: arr -> int) -> int:
    total: mut int = 0
    for (i in range(0, len(arr))):
        total = total + arr[i]
    return total
