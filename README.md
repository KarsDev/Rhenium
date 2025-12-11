
# 🌀 RE Language

A statically typed, indentation-based programming language with support for:

- Functions
- Structs
- Pointers
- Arrays
- Native interop
- Type inference via suffixes

---

## 🔤 Basic Syntax

### 🧮 Variables

```re
int x = 42
mut float y = 3.14
str name = "RE Language"
```

### ✅ Boolean

```re
bool flag = true
bool other = false
```

### 🔁 Control Flow

#### `if` / `else`

```re
if x > 10:
    return x
else:
    return 0
```

#### `while` Loop

```re
while x < 100:
    x += 1
```

#### `break` / `continue`

```re
while true:
    x += 1
    if x == 5:
        break
    if x % 2 == 0:
        continue
```

---

## 🧠 Functions

```re
fn add(int a, int b) -> int:
    return a + b
```

No return value:

```re
fn greet(str name) -> none:
    native.println("Hello, " + name)
```

Native call:

```re
native.println("Hello from native!")
```

---

## 🏗️ Structs

```re
struct Person:
    str name
    int age

Person p("Alice", 30)
p.name = "Bob"
```

---

## 🧱 Arrays

```re
arr:int[4] numbers = [1, 2, 3, 4]
int value = numbers[2]
numbers[0] = 100
```

Fixed-size:

```re
arr:int[10] buffer
```

---

## 🧷 Pointers

```re
int x = 10
ptr:int p = ptr(x)
@p = 20
```

---

## 🔁 Operators

- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `==`, `!=`, `<`, `>`, `<=`, `>=`
- Logic: `and`, `or`, `!`
- Assignment: `=`, `+=`, `-=`, etc.
- Ternary: `a > b ? a : b`

---

## 🧪 Type Casting

```re
int x = int(3.14)
```

---

## 🔧 Built-in Types

```
int, int8, int16, int32, int64  
float, double  
bool, str, char  
ptr, arr  
none, null
```

---

## 📐 Meta Operations

```re
sizeof(int)      // Get size of type
alignof(double)  // Get alignment
```
