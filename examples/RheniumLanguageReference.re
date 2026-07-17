/*
<=------------------------=>|<=>|<=-----------------------=>
                    RHENIUM LANGUAGE REFERENCE
<=------------------------=>|<=>|<=-----------------------=>

This file documents almost every feature of the Rhenium Programming Language.

Almost every syntax feature, rule, and behavior supported by the
parser is demonstrated here.

*/

/*
<=------------------------=>|<=>|<=-----------------------=>
  MODULE SYSTEM
<=------------------------=>|<=>|<=-----------------------=>

Modules are imported with the keyword `using`.

Syntax:
    using <module>
    using <module> in <package>

- Modules are resolved using dot-notation
- Internally dots map to path separators
- `self` refers to the input file package
*/

using io
using math
using utils.helpers in "my_package"
using localModule in self

/*
<=------------------------=>|<=>|<=-----------------------=>
  BUILTIN AND INTERNAL DECLARATIONS
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Builtin functions bind RE functions to LLVM implementations.
They are declared using `_Builtin func`.
*/

_Builtin func toggleAlarm(msg: string) -> none = """
LLVM Implementation
"""

/*
IR declarations embed raw IR code.
*/

_IR """
LLVM Top code declarations
"""

/*
Native C++ bindings
*/

/*
 Loads the functions and the resource C++ file.
 This is usually only used by the compiler native interface.

 Using _Builtin doesn't actually define any function,
 making them not usable without the LLVM implementation
*/
_NativeCPP("math") int add(a: int, b: int) and int sub(a: int, b: int)

// If the C++ file is not in the resources it can be loaded with the extern keyword
extern("math") _Builtin
// or
extern("math") int add(a: int, b: int)

/*
<=------------------------=>|<=>|<=-----------------------=>
  TYPES
<=------------------------=>|<=>|<=-----------------------=>

Supported type categories:
- Builtin types
- Struct types
- Pointer types
- Array types
- Lambda types
- Range types (Variables can't have range type)

Note:
    You don't need to specify the type when declaring a variable
*/

a: int = 10
b: bool = true
c: char = 'x'
s: str = "hello"

/*
Pointer types
Syntax:
    ptr -> <type>
*/

pointer_to_a: ptr -> int = ptr(a)

/*
Array types
Syntax:
    arr -> <type>
*/

arr_int: arr -> int = [1, 2, 3]

/*
Type queries
*/

// Get the string type reppresentation of the variable a
a_type: str = typeof(a)

// Get the LLVM string representation of the type of the variable a
a_llvm_type: str = typeofLLVM(a)

/*
Type checking
*/

// Checks if a is an integer
is_int = a is int

/*
<=------------------------=>|<=>|<=-----------------------=>
  VARIABLES AND ASSIGNMENT
<=------------------------=>|<=>|<=-----------------------=>

Assignment forms:
    x = value
    x: type = value
    x: mut [type] = value

Note:
    Variables are IMMUTABLE by default
*/

// Deduced type: int
x = 5
y: int = 10

// Here the compiler deduces the 'int' type and sets 'z' as mutable
z: mut = 15

/*
Global variables:
- Must be constant
- Cannot be mutable
*/

global PI_INT: int = 3

/*
<=------------------------=>|<=>|<=-----------------------=>
  EXPRESSIONS AND OPERATORS
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Binary operators
*/

sum = a + b
diff = a - b
prod = a * b
div = a / b

/*
Unary operators
*/

neg = -a
pos = +a
bitnot = ~a

/*
Ternary operator
Syntax:
    then_expr if condition else else_expr
*/

max = a if (a > b) else b

/*
<=------------------------=>|<=>|<=-----------------------=>
  FUNCTIONS
<=------------------------=>|<=>|<=-----------------------=>

Function declaration syntax:
    func name(params...) -> return_type:
*/

func add(a: int, b: int) -> int:
    return a + b

/*
Functions without explicit return type return `none`
*/

func log(msg: string):
    println(msg)

/*
Mutable parameters
*/

func increment(x: mut int):
    x = x + 1

/*
Function calls
*/

r = add(1, 2)
log("hello")

/*
Inline functions
*/

// inlines the functions after compilation, purely a perforemance choice
func write(l: str) inline:
  println(l)

/*
<=------------------------=>|<=>|<=-----------------------=>
  CONTROL FLOW
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
If / Else / Else If
*/

if (a > 0):
    println("positive")
else if (a < 0):
    println("negative")
else:
    println("zero")

/*
While loops
*/

while (a < 10):
    a += 1
    if (a == 5):
        continue
    if (a == 8):
        break

/*
For loops
*/

for (i in range(0, 10)):
    println(i)

/*
Range forms

range(<start>[, <end>[, <step>]])
*/

r: mut = range(5) // from 0 to 5
r = range(0, 5) // from 0 to 5
r = range(0, 10, 2) // from 0 to 10 by incrementing by 2 every time (0, 2, 4, 6, 8, 10)

/*
<=------------------------=>|<=>|<=-----------------------=>
  MATCH STATEMENTS
<=------------------------=>|<=>|<=-----------------------=>
*/

tw = 7

// prints 7
// if 'tw' was not in the range [1,9] it would have went to digit out of bounds
match (tw):    
    1:
      println("one")
    2:
      println("two")
    3:
      println("three")
    4:
      println("four")
    5:
      println("five")
    6:
      println("six")
    7:
      println("seven")
    8:
      println("eight")
    9:
      println("nine")
    _:
      println("Digit out of bounds [1, 9]: " + tw)

/*
<=------------------------=>|<=>|<=-----------------------=>
  ARRAYS
<=------------------------=>|<=>|<=-----------------------=>
*/

nums = [1, 2, 3] // creates the static array

first = nums[0] // arrays start at index 0

nums[1] = 42 // sets the nums array index 1 ( nums[1] = 2 ) to 42

size = len(nums) // returns 3


// Arrays can also be dynamic, their memory must be handled using the 'delete' keyword

dynarr = init arr -> int(5) // creates an int array of size 5

delete dynarr

/*
<=------------------------=>|<=>|<=-----------------------=>
  STRUCTS
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Struct declaration
*/

struct Vec2:
    x: int
    y: int

// Builtin structs cannot be initialized and are usually used by the compiler
_Builtin struct NotInit:
    handle: anyptr

/*
Struct initialization
*/

v = init Vec2(3, 4) // Uses the default constructor, sets x=3 and y=4

/*
Field access
*/

vx = v.x
vy = v.y

/*
Struct methods

Use this.<var> to access the struct variable or this.<func>([params]) to call a function

The 'self' keyword is the reppresentation of the current struct pointer,
instead, the 'this' keyword reppresents the dereference of the self (@self)
*/

impl Vec2:
    // Constructors are optional, in this case it's redundant, but it's still a good practice
    init(x: int, y: int):
      this.x = x
      this.y = y

    func length() -> int:
        return this.x * this.x + this.y * this.y

    func scale(f: this):
        this.x =* f
        this.y =* f

/*
Method calls
*/

len2 = v.length()
v.scale(2)

/*
<=------------------------=>|<=>|<=-----------------------=>
  POINTERS AND REFERENCES
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Reference creation
*/

rp = ptr(v)

// Type anyptr: pointer to any object
// anyptr is the C++ equivalent to void*
intptr = ptr(0x01C433)



/*
Dereference
*/

dv = @rp

/*
Dereference assignment
*/

@rp = init Vec2(0, 0)

/*
<=------------------------=>|<=>|<=-----------------------=>
  CASTING AND SIZE
<=------------------------=>|<=>|<=-----------------------=>
*/

casted = cast<int>(a) // casts 'a' to an integer

sz = sizeof(a) // gets the size of a, it's the equivalent of sizeof(int)

/*
<=------------------------=>|<=>|<=-----------------------=>
  ERROR HANDLING
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Raise exceptions
*/

raise "something went wrong" // prints the error
raise none // doesn't print anything

/*
Try / Catch
*/

try:
    raise "error"
catch:
    println("caught error")

// Async blocks

t = async:
  b = 12
  println("b = " + b)

// Runs the block asynchronously
t.run()

// Async blocks with return type anyptr, implicitly ptr -> str
t2 = async(str):
  return "Hello World!"

// Waits for the thread to run and get the result asynchronously
result = t2.await()


strPtr = cast<ptr -> str>(result)

// Prints "Hello World!"
println(@strPtr)


/*
<=------------------------=>|<=>|<=-----------------------=>
  GENERICS
<=------------------------=>|<=>|<=-----------------------=>
*/

// Declare a generic function with type 'T'
// T can be any type
generic func swap<T>(a: mut T, b: mut T):
    tmp = @a
    @a = @b
    @b = tmp

// Declare a generic struct with type 'T'
// T can be any type just like in functions
generic struct Box<T>:
    content: T

// Now just initialize the Box with the generic type <..>
b = init Box<int>(12)

/*
<=------------------------=>|<=>|<=-----------------------=>
  LAMBDA
<=------------------------=>|<=>|<=-----------------------=>
*/

// define lambda called square that takes a parameter called x with type int and multiplies it by itself
square = lambda(x: int) = x*x

// the lambda will be compiled as a function, so it's called as one

// call square lambda
v = square(2)

/*
<=------------------------=>|<=>|<=-----------------------=>
  NAMESPACES
<=------------------------=>|<=>|<=-----------------------=>
*/

// define namespace called Alpha
namespace Alpha:
  global I = -1

  func beta():
    println("called Alpha::beta()")

// calls function beta in namespace Alpha
Alpha::beta()

// gets the global variable I in namespace Alpha
Alpha::I

/*
<=------------------------=>|<=>|<=-----------------------=>
  TYPE
<=------------------------=>|<=>|<=-----------------------=>
*/

// creates an alias of ptr -> int
type IntPtr = ptr -> int

x: int = 5
x_ptr: IntPtr = ptr(x)

/*
<=------------------------=>|<=>|<=-----------------------=>
  TYPE
<=------------------------=>|<=>|<=-----------------------=>
*/

// declare enum colors
enum Colors:
  RED
  BLUE
  GREEN

// get field RED, equals to 0 as RED is index 0 in Colors
r = Colors.RED

// if we assign a constant to enums, getting fields return constants
enum Cars:
  MUSTANG = "Mustang"
  FORD = "Ford"
  FERRARI = "Ferrari"


/*
<=------------------------=>|<=>|<=-----------------------=>
  STRUCT INHERITANCE
<=------------------------=>|<=>|<=-----------------------=>
*/

// defines trait Vehicle that requires a function called vroom
trait Vehicle:
  func wroom()

// Inherits all Vehicles function requirements
struct Car inherits Vehicle:
  name: str

impl Car:
  // override Vehicle#wroom()
  func wroom():
    println("The car is running")

// define function wroomVehicle that accepts as parameter a T which must inherit Vehicle trait
func wroomVehicle<T inherits Vehicle>(vehicle: T):
  vehicle.wroom()

// If a struct must inherit multiple traits, they can be inherited by splitting them with ',':
struct Word inherits LetterContainer, Printable, Writeable:
  letters: arr -> char


/*
<=------------------------=>|<=>|<=-----------------------=>
  EXTERN FUNCTION
<=------------------------=>|<=>|<=-----------------------=>
*/

// declares an extern (see C++ docs) function, name won't be mangled
extern func write():
    println("Hello World!")

/*
<=------------------------=>|<=>|<=-----------------------=>
  DELETE
<=------------------------=>|<=>|<=-----------------------=>
*/

// The `delete` keyword is used to release resources owned by an object.
// A delete block behaves similarly to a destructor and is called when
// `delete <object>` is executed.
// The destructor of struct variables that are instantiated in a block 
// is automatically called when it falls out of scope.

using memory
using string

struct CharBuf:
    inner: ptr -> char
    length: int

impl CharBuf:
    init(s: str):
        this.length = len(s)

        raw = Memory::malloc(this.length + 1)
        chars = cast<ptr -> char>(raw)

        Memory::memcpy(raw, cast<ptr -> char>(s), this.length)
        chars[this.length] = '\0'

        this.inner = chars

    delete:
        if (this.inner != null):
            Memory::free(cast<anyptr>(this.inner))
            this.inner = null

        this.length = 0

    func getInner() -> str:
        if (this.inner == null):
            return ""

        return strFromChars(this.inner, this.length)

buf = init CharBuf("Hello World")
delete buf
buf.getInner() // Safe after deletion (the object invalidates itself)

/*
<=------------------------=>|<=>|<=-----------------------=>
  ZERO
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
 The 'zero' keyword is used for initializing a variable without explicitly passing the value.
 The format is zero <type>
 Using the same variable won't cause any error.
*/

i = zero int

j: CharBuf = zero // type is not required in explicit variable declarations

// 'zero' can also be used for function returns as it's considered a value, but the type is explicitly required
func test() -> int:
  return zero int // returns 0

/*
<=------------------------=>|<=>|<=-----------------------=>
  ENTRY POINT
<=------------------------=>|<=>|<=-----------------------=>
*/

func main() -> int:
    println("RE language reference executed successfully")
    return 0
