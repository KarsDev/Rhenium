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
using localmodule in self

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
 Using _Builtin does not actually load any function in the Rhenium compiler,
 and this means that you can't call them in normal ways,
 but you can use them in LLVM declarations (Such as _IR or _Builtin func)
 */
_NativeCPP("native.cpp") _Builtin

/*
 Loads the functions and the C++ file
*/
_NativeCPP("math.cpp") int add(a: int, b: int) and int sub(a: int, b: int)

/*
<=------------------------=>|<=>|<=-----------------------=>
  TYPES
<=------------------------=>|<=>|<=-----------------------=>

Supported type categories:
- Builtin types
- Struct types
- Pointer types
- Array types
- Range types

Note:
    You don't need to specify the type when declaring a variable
*/

a: int = 10
b: bool = true
c: char = 'x'
s: str = "hello"
n: none = none

/*
Pointer types
Syntax:
    ptr -> <type>
*/

pa: ptr -> int = ptr(a)

/*
Array types
Syntax:
    arr -> <type>
*/

arr_int: arr -> int = [1, 2, 3]

/*
Type queries
*/

// Get the type of the variable a
ta = _Typeof(a)

// Get the LLVM reppresentation of the type of the variable a
tb = _TypeofLLVM(a)

/*
Type checking
*/

// Checks if a is an istance of an integer
is_int = a is int

/*
<=------------------------=>|<=>|<=-----------------------=>
  VARIABLES AND ASSIGNMENT
<=------------------------=>|<=>|<=-----------------------=>

Assignment forms:
    x = value
    x: type = value
    x: mut type = value

Note:
    Variables are IMMUTABLE by default
*/

// Deduced type: int
x = 5
y: int = 10
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
    a = a + 1
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

r: mut = range(5)
r = range(0, 5)
r = range(0, 10, 2)

/*
<=------------------------=>|<=>|<=-----------------------=>
  ARRAYS
<=------------------------=>|<=>|<=-----------------------=>
*/

nums = [1, 2, 3]

first = nums[0]

nums[1] = 42

size = len(nums)

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

/*
Struct initialization
*/

v = init Vec2(3, 4)

/*
Field access
*/

vx = v.x
vy = v.y

/*
Struct methods

Use self.<var> to access the struct variable
*/

impl Vec2:
    func length() -> int:
        return self.x * self.x + self.y * self.y

    func scale(f: int):
        self.x = self.x * f
        self.y = self.y * f

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

casted = cast<int>(a)

sz = sizeof(a)

/*
<=------------------------=>|<=>|<=-----------------------=>
  ERROR HANDLING
<=------------------------=>|<=>|<=-----------------------=>
*/

/*
Raise exceptions
*/

raise "something went wrong"
raise none

/*
Try / Catch
*/

try:
    raise "error"
catch:
    println("caught error")

/*
<=------------------------=>|<=>|<=-----------------------=>
  ENTRY POINT
<=------------------------=>|<=>|<=-----------------------=>
*/

func main() -> int:
    println("RE language reference executed successfully")
    return 0
