# The Rhenium Coding Language

Rhenium is a statically typed, compiled language focused on simplicity, explicitness, and low-level control while maintaining a clean, Python-like syntax. Rhenium compiles to LLVM IR and provides native C++ integration for low-level and platform-specific code.

The language reference included with this project is production-ready and is itself an executable demonstration of the supported syntax and behavior.

## Key Features

- Compiled to LLVM IR
- Static typing with type inference
- Python-like, indentation-based syntax
- Immutable-by-default variables with explicit `mut`
- Structs, methods, constructors, and field access
- Generics and generic constraints
- Pointers, references, arrays, casts, and `sizeof`
- Lambdas and inline functions
- `if` / `else if` / `else`, `while`, `for`, and `match`
- Ranges and array operations
- Exceptions with `raise`, `try`, and `catch`
- Async blocks with `.run()` and `.await()`
- Namespaces, type aliases, enums, traits, and struct inheritance
- Explicit resource cleanup with `delete`
- `zero` initialization and deep-copy support with `copy`
- LLVM builtins and raw IR declarations
- Native C++ bindings and `extern` declarations
- Module packages with `mod.re` entry points and `export`
- Multiple `main` entry-point forms, including command-line arguments

## Example Code

```text
func main() -> int:
    println("Hello, World!")

    count: mut = 0
    while (count < 3):
        println(count)
        count++

    return 0
```

## Language Overview

### Modules and Packages

Modules are imported with `using`. Module names use dot notation, and `self` refers to the current input-file package.

```text
using math
using list
using utils.helpers in "my_package"
using localModule in self
```

A directory can be imported as a cohesive module when it contains `mod.re`:

```text
utils/
  mod.re
  helpers.re
  strings.re
```

```text
using utils
```

The compiler resolves that import to `utils/mod.re` when a directory module is selected. A `mod.re` file can re-export package members, and `export` is reserved for module package exports.

```text
export utils
```

### Builtin, LLVM, and Native Declarations

Rhenium can bind language-level declarations to compiler-provided LLVM implementations, embed raw LLVM IR, and load native C++ resources.

```text
_Builtin func toggleAlarm(msg: string) -> none = """
LLVM Implementation
"""

_IR """
LLVM Top code declarations
"""

_NativeCPP("math") int add(a: int, b: int) and int sub(a: int, b: int) // used natively
extern("math") int add(a: int, b: int) // with math.cpp in the same folder
```

### Types

The reference demonstrates these type categories:

- Builtin types such as `int`, `bool`, `char`, `str`, and `none`
- Struct types
- Pointer types (`ptr -> T`)
- Array types (`arr -> T`)
- Lambda types
- Range values

Type annotations are optional when the compiler can infer the type.

```text
a: int = 10
b: long = 10
c: short = 10
d: byte = 10
e: float = 10.0
f: double = 10.0
g: bool = true
h: char = 'x'
i: str = "hello"

pointer_to_a: ptr -> int = ptr(a)
arr_int: arr -> int = [1, 2, 3]
```

Rhenium also provides type queries and type checks:

```text
a_type: str = typeof(a)
is_int = a is int
```

### Variables, Mutability, and Globals

Variables are immutable by default. Use `mut` when mutation is required.

```text
x = 5
y: int = 10
z: mut = 15

func increment(x: mut int) -> int:
    x = x + 1
    return x
```

Global variables must be constant and cannot be mutable.

```text
global PI_INT = 3
```

### Expressions and Operators

Rhenium supports binary operators, unary operators, ternary expressions, and prefix/postfix increment/decrement.

```text
a = 5
b = 6

sum = a + b
diff = a - b
neg = -a
bitnot = ~a

max = a if a > b else b

x: mut = 12
y = x++
z = --x
```

### Functions

Functions use `func` and may specify an explicit return type. Without an explicit return type, a function returns `none`.

```text
func add(a: int, b: int) -> int:
    return a + b

func log(msg: str):
    println(msg)
```

Functions can be marked `inline` as a compile-time performance choice, and generic functions can be inlined as well.

```text
func write(l: str) -> none inline:
    println(l)

generic func sum<T>(a: T, b: T) -> T inline:
    return a + b
```

### Control Flow and Ranges

Rhenium supports conditional branches, `while` loops, `for` loops, `continue`, `break`, and range iteration.

```text
if (a > 0):
    println("positive")
else if (a < 0):
    println("negative")
else:
    println("zero")

for (i in range(0, 10)):
    println(i)

while (a < 10):
    a += 1
```

Range forms are:

```text
range(<start>[, <end>[, <step>]])
```

Examples include `range(5)`, `range(0, 5)`, and `range(0, 10, 2)`.

### Match Statements

`match` provides multi-branch selection with a fallback `_` case.

```text
match (value):
    1:
        println("one")
    2:
        println("two")
    _:
        println("other")
```

### Arrays

Static arrays support indexing and assignment. `len` returns their length and can also dispatch to a struct's `length` method.

```text
nums = [1, 2, 3]
first = nums[0]
nums[1] = 42
size = len(nums)
```

Dynamic arrays can be initialized explicitly and released with `delete`:

```text
v = 5
// v must not be a constant,
// otherwise the compiler would compile this as a static array
dynarr = init arr -> int(v) 
delete dynarr
```

### Structs, Methods, and Initialization

Struct fields can be mutable or immutable. Instances are initialized with `init`, and methods are declared inside `impl` blocks.

```text
struct Vec2:
    x: mut int
    y: mut int

impl Vec2:
    init(x: int, y: int):
        this.x = x
        this.y = y

    func length() -> int:
        return this.x * this.x + this.y * this.y

    func mul(v: int):
        this.x *= v
        this.y *= v
        
v = init Vec2(3, 4)
len2 = len(v)
v.mul(2)
```

Inside an implementation, `this` represents the dereferenced current struct pointer, while `self` represents the current struct pointer.

### Pointers, References, Casting, and Size

Pointers are explicit and can be created with `ptr`, dereferenced with `@`, cast between types, and inspected with `sizeof`.

```text
rp = ptr(v)
dv = @rp
@rp = init Vec2(0, 0)

casted = cast<int>(a)
sz = sizeof(a)
```

The reference also demonstrates `anyptr` for a pointer to an arbitrary object.

### Error Handling

Rhenium supports `raise`, `try`, and `catch`, including catching a specific error type and binding the raised value to a variable.

```text
try:
    raise "error"
catch str msg:
    println("caught error: " + msg)
```

Custom errors can be built from `Error` and can provide a `message()` implementation.
The caught error can be a string or a struct inheriting `Error`.
```text
struct TestError inherits Error:
    msg: str

impl TestError:
    func message() -> str:
        return "Test Message \"" + msg + "\""
```

### Async Execution

Async blocks run concurrently. A block can be started with `.run()` or awaited for a result with `.await()`.

```text
t = async:
    println("running asynchronously")

t.run()

t2 = async(str):
    return "Hello World!"

result = Thread::awaitAndCast<str>(t2) // awaits and cast the result pointer to a string
println(result)
```

### Generics

Generic functions and structs use type parameters such as `T`.

```text
generic func swap<T>(a: ptr -> T, b: ptr -> T):
    tmp = @a
    @a = @b
    @b = tmp

a = 5
b = 6

swap(ptr(a), ptr(b)) // In this case the type is deducted by the compiler
println("a=" + a + ", b=" + b)

generic struct Box<T>:
    content: T

b = init Box<int>(12)
```

Generic type parameters can also be constrained with traits.

### Lambdas

Lambdas are first-class function values and are compiled as functions.

```text
square = lambda(x: int) = x * x
v = square(2)
```

### Namespaces and Type Aliases

Namespaces group globals and functions and use `::` for access.

```text
namespace Alpha:
    global I = -1

    func beta():
        println("called Alpha::beta()")

Alpha::beta()
Alpha::I
```

Type aliases create reusable names for existing types.

```text
type IntPtr = ptr -> int

x: int = 5
x_ptr: IntPtr = ptr(x)
```

### Enums

Enums can use implicit ordinal values or explicitly assigned constants.

```text
enum Colors:
    RED
    BLUE
    GREEN

r = Colors.RED

enum Cars:
    MUSTANG = "Mustang"
    FORD = "Ford"
    FERRARI = "Ferrari"
```

### Traits and Struct Inheritance

Traits declare required functions. Structs can inherit one or multiple traits, and generic functions can constrain type parameters with `inherits`.

```text
trait Vehicle:
    func wroom()

struct Car inherits Vehicle:
    name: str

impl Car:
    func wroom(): // override
        println("The car is running")

func wroomVehicle<T inherits Vehicle>(vehicle: T):
    vehicle.wroom()
```

Multiple inherited traits are separated with commas:

```text
struct Word inherits LetterContainer, Printable, Writeable:
    letters: arr -> char
```

### External Functions

`extern` can declare functions that should not be name-mangled.

```text
extern func write():
    println("Hello World!")
```

### Resource Management and `delete`

A struct can define a `delete` block that behaves like a destructor when the object is explicitly deleted. Struct instances created inside a scope are also destroyed automatically when they fall out of scope.

```text
struct CharBuf:
    inner: mut ptr -> char
    length: mut int

impl CharBuf:
    delete:
        if (this.inner != null):
            Memory::free(cast<anyptr>(this.inner))
            this.inner = null

        this.length = 0

func createBuf():
    buf = init CharBuf("Hello World")
    // Redundant:  the compiler automatically calls delete when falling out of scope
    //delete buf 
```

### Zero Initialization and Copying

`zero` initializes a variable without explicitly providing a value, and `copy` performs a deep copy.

```text
i = zero int
j: CharBuf = zero // deducted type: CharBuf

func test() -> int:
    return zero int // cannot deduct type in return

alpha = init CharBuf("Alpha")
copied = copy(alpha)
```

### Entry Points

A Rhenium program can define a conventional `main` entry point:

```text
func main() -> int:
    println("RE language reference executed successfully")
    return 0
```

The reference also demonstrates a command-line entry-point overload:

```text
func main(argc: int, args: ptr -> str) -> int:
    println("Specified " + argc + " args:")
    for (i in range(argc)):
        println("   - " + args[i])
    return 0
```

## Language Reference

`RheniumLanguageReference.re` is the comprehensive executable reference for the language. It demonstrates the parser-supported syntax and behavior across modules, declarations, types, expressions, control flow, data structures, memory management, error handling, concurrency, generics, namespaces, traits, enums, and entry points.

## VSCode Extension

`rhenium-0.0.1.vsix` is the Rhenium VSCode extension. Place it into the extensions folder then install from vsix.

## Goals

- Simple, readable syntax without hidden behavior
- Explicit control over memory and mutability
- LLVM-level extensibility
- Native integration where needed
- No implicit runtime magic

## License

MIT License
