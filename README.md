# The Rhenium Coding Language

The Rhenium coding language is a statically typed, compiled language focused on simplicity, explicitness, and low-level control while maintaining a clean, Python-like syntax. Rhenium compiles to LLVM IR, enabling native performance and portability.

## Key Features
- Compiled to LLVM
- Statically typed with type inference
- Simple Python-like syntax
- Fast compile times
- Immutable-by-default variables
- Explicit mutability
- Native LLVM IR and C++ bindings
- Structs, generics, pointers, and async execution

## Example Code

```python
func main():
    println("Hello, World!")
    
    x = 5
    
    y: mut = "alpha"
    y = "beta"
    
    if (y == "beta"):
        println("y is beta!")
    else:
        println("y is not beta!")
```

## Language Overview

### Modules
Modules are imported using the `using` keyword and resolved via dot notation.

```python
using io
using math
using utils.helpers in "my_package"
using localmodule in self
```

### Types
Rhenium supports:
- Builtin types (`int`, `bool`, `char`, `str`, `none`)
- Struct types
- Pointer types (`ptr -> T`)
- Array types (`arr -> T`)
- Range types

Type annotations are optional unless required for clarity or mutability.

```python
x = 5
y: int = 10
z: mut = 15
```

### Immutability & Mutability
Variables are immutable by default. Use `mut` explicitly when mutation is required.

```python
count: mut int = 0
count = count + 1
```

### Functions
Functions use the `func` keyword and may optionally specify a return type.

```python
func add(a: int, b: int) -> int:
    return a + b
```

### Control Flow
Rhenium supports familiar control-flow constructs:

```python
if (x > 0):
    println("positive")
else:
    println("non-positive")

for (i in range(0, 10)):
    println(i)

while (x > 0):
    x = x - 1
```

### Structs and Methods
Structs define data layouts, and `impl` blocks define methods.

```python
struct Vec2:
    x: int
    y: int

impl Vec2:
    func length() -> int:
        return (@self).x * (@self).x + (@self).y * (@self).y
```

### Pointers
Rhenium exposes explicit pointer semantics.

```python
p = ptr(x)
value = @p
@p = 42
```

### Generics
Generic functions allow type-agnostic logic.

```python
generic func swap<T>(a: mut T, b: mut T):
    tmp = @a
    @a = @b
    @b = tmp
```

### Async Execution
Async blocks run concurrently and can return values.

```python
task = async(str):
    return "Hello"

result = task.await()
castResult = cast<ptr -> str>(result)
println(@castResult)
```

## Project Status
- Parser: complete
- Type checker: complete
- LLVM backend: functional
- Standard library: minimal
- Breaking changes expected

Rhenium is currently **experimental**. Syntax and semantics may change as the language evolves.

## Goals
- Simple, readable syntax without hidden behavior
- Explicit control over memory and mutability
- LLVM-level extensibility
- No implicit runtime magic

## License
MIT License
