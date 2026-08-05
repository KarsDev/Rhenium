using collections.list
using memory

struct Casual inherits Writeable:
    inner: int

impl Casual:
    func toString() -> str:
        return "Casual{inner=" + this.inner + "}"

func main(argc: int, args: ptr -> str) -> int:
    vals = init List<ptr -> Casual>()

    for (i in range(5)):
        add(ptr(vals), i)
    
    for (i in range(5)):
        println(@(vals.get(i)))

func add(vs: ptr -> List<ptr -> Casual>, i: int):
    v = init Casual(i)

    raw = Memory::malloc(sizeof(Casual))
    obj = cast<ptr -> Casual>(raw)

    @obj = copy(v)

    (@vs).add(obj)