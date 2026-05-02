using math

global TEN_FACTORIAL = 3628800

func main() -> int:
    testArray()
    testAsync()
    testFunctions()
    testModules()
    testPointers()
    testRaise()
    testNativeFunctions()
    testGenerics()
    testTernary()

    println("\nAll test passed successfully")

func testArray():
    a = [1, 2, 3]
    idx: mut = 0

    zz = init arr -> int(0)
    if (len(zz) != 0):
        raise "Empty array failed"

    if (len(a) != 3):
        raise "Error with len"

    for (_ in a):
        v = a[idx]
        idx += 1
        if (v != idx):
            raise "Error with array"
    
    bLenght = 3
    b = init arr -> int(bLenght)

    for (i in range(bLenght)):
        b[i] = ((bLenght * bLenght) - i)

    if (b[1] != 8):
        raise "Error with array"

    println("Variable testing passed successfully")
    println("Binary testing passed successfully")
    println("Len testing passed successfully")
    println("Statement testing passed successfully")
    println("Array testing passed successfully")

func testAsync():
    a = async(long):
        f = 10
        r: mut long = 1

        for (i in range(f)):
            j = f - i
            if (j == 0):
                break
            r *= j
        
        return r

    res_anyptr = a.await()
    res_intptr = cast<ptr -> int>(res_anyptr)
    res = @res_intptr
    
    if (res != TEN_FACTORIAL):
        raise "Error with async"
    
    println("Dereference testing passed successfully")
    println("Cast testing passed successfully")
    println("Async testinc passed successfully")
    println("Global testinc passed successfully")

struct Test:
    inner: int

impl Test:
    init(a: int, b: bool):
        this.inner = a

    func getInner() -> int:
        return this.inner
    
func testFunctions():
    a = init Test(5)
    b = init Test(5, true)

    if (a != b):
        raise "Error with struct"
    if (not (a == b)):
        raise "Error with equals"
    
    if (a.inner != b.getInner()):
        raise "Error with struct functions"

    if (a is not Test):
        raise "Error with instance"

    println("Struct testing passed successfully")
    println("Function testing passed successfully")
    println("Instance testing passed successfully")

func testModules():
    if (ln(E) != 1):
        raise "Error with math"
    
    println("Module testing passed successfully")

struct PtrTest:
    lol: str

func testPointers():
    x = init PtrTest("olo")
    x_ptr = ptr(x)
    x_ptr_ptr = ptr(x_ptr)

    if ((@(@x_ptr_ptr)).lol != "olo"):
        raise "Error with pointers"
    
    (@(@x_ptr_ptr)).lol = "lol"

    if ((@(@x_ptr_ptr)).lol != "lol"):
        raise "Error with pointers"

    println("Pointer testing passed successfully")

func testRaise():
    try:
        raise "Error with raise/try-catch"
    catch:
        println("Error testing passed successfully")

func testNativeFunctions():
    i = 5

    if (sizeof(i) != 4):
        raise "Error with sizeof"
    if (_Typeof(i) != "int"):
        raise "Error with _Typeof"
    if (_TypeofLLVM(i) != "i32"):
        raise "Error with _TypeofLLVM"

    println("Native functions testing passed successfully")

generic struct Box<T>:
    inner: T

func specificTest0(a: ptr -> Box<int>):
    (@a).inner +=1

generic func sum<T>(a: T, b: T) -> T:
    return a + b

func testGenerics():
    a: Box<int> = init Box<int>(32)

    if (a.inner != 32):
        raise "Error with generic struct"
    
    if (sum(5, 10) != 15):
        raise "Error with generic func"

    specificTest0(ptr(a))

    if (a.inner != 33):
        raise "Error with function call in generic params: " + a.inner

    println("Generics testing passed successfully")

func testTernary():
    x = sum(15, 25)

    a = "working" if x == 40 else "..."

    if (a != "working"):
        raise "Error with ternary"

    println("Ternary testing passed successfully")