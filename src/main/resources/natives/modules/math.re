_IR """
declare double @cbrt(double)
declare i32 @llvm.ctlz.i32(i32, i1)
"""

// The natural base of logarithms
global E: double =  2.7182818284590452

// The ratio of the circumference of a circle to its diameter
global PI: double = 3.1415926535897932

// The value to multiply a degree angle to get a radians angle: PI/180
global DEG_TO_RAD: double =  0.0174532925199432

// The value to multiply a radians angle to get a degree angle: 180/PI
global RAD_TO_DEG: double = 57.2957795130823209

// Returns the absolute value of an int
func abs(val: int) -> int:
    return val if val >= 0 else -val

// Returns the absolute value of a long
func abs(val: long) -> long:
    return val if val >= 0 else -val

// Returns the absolute value of a double
func abs(val: double) -> double:
    return val if val >= 0 else -val

// Returns the absolute value of a float
func abs(val: float) -> float:
    return val if val >= 0 else -val

// Returns the angle in degrees
func toDegrees(rad: double) -> double:
    return rad * RAD_TO_DEG

// Returns the angle in radians
func toRadians(deg: double) -> double:
    return deg * DEG_TO_RAD

// Builtins:

// Returns the sine of an angle
_Builtin func sin(x: double) -> double = """
entry:
    %res = call double @llvm.sin.f64(double %x)
    ret double %res
"""

// Returns the cosine of an angle
_Builtin func cos(x: double) -> double = """
entry:
    %res = call double @llvm.cos.f64(double %x)
    ret double %res
"""

// Returns the tangent of an angle
_Builtin func tan(x: double) -> double = """
entry:
    %sin = call double @llvm.sin.f64(double %x)
    %cos = call double @llvm.cos.f64(double %x)
    %res = fdiv double %sin, %cos
    ret double %res
"""

// Returns the arcsin of an angle
_Builtin func asin(x: double) -> double = """
entry:
    %res = call double @llvm.asin.f64(double %x)
    ret double %res
"""

// Returns the arccosine of an angle
_Builtin func acos(x: double) -> double = """
entry:
    %res = call double @llvm.acos.f64(double %x)
    ret double %res
"""

// Returns the arctan of an angle
_Builtin func atan(x: double) -> double = """
entry:
    %res = call double @llvm.atan.f64(double %x)
    ret double %res
"""

// Returns the hyperbolic sine of an angle
_Builtin func sinh(x: double) -> double = """
entry:
    %res = call double @llvm.sinh.f64(double %x)
    ret double %res
"""

// Returns the hyperbolic cosine of an angle
_Builtin func cosh(x: double) -> double = """
entry:
    %res = call double @llvm.cosh.f64(double %x)
    ret double %res
"""

// Returns the hyperbolic tangent of an angle
_Builtin func tanh(x: double) -> double = """
entry:
    %res = call double @llvm.tanh.f64(double %x)
    ret double %res
"""

// Returns the power of x^y
_Builtin func pow(x: double, y: double) -> double = """
entry:
    %res = call double @llvm.pow.f64(double %x, double %y)
    ret double %res
"""

// Returns the square root of number
_Builtin func sqrt(x: double) -> double = """
entry:
    %res = call double @llvm.sqrt.f64(double %x)
    ret double %res
"""

// Returns the cubic root of a number
_Builtin func cbrt(x: double) -> double = """
entry:
    %res = call double @cbrt(double %x)
    ret double %res
"""

// Returns the natural logarithm (base e) of a number
_Builtin func ln(x: double) -> double = """
entry:
    %res = call double @llvm.log.f64(double %x)
    ret double %res
"""

// Returns the base 10 logarithm of a number
_Builtin func log10(x: double) -> double = """
entry:
    %res = call double @llvm.log10.f64(double %x)
    ret double %res
"""

// Returns the base 2 logarithm of a number
_Builtin func log2(x: double) -> double = """
entry:
    %res = call double @llvm.log2.f64(double %x)
    ret double %res
"""

// Returns the base 2 logarithm of a integer (Fastest option)
_Builtin func log2Int(n: int) -> int = """
entry:
  ; Step 1: Count leading zeros. 
  ; llvm.ctlz.i32(value, is_zero_undef)
  %clz = call i32 @llvm.ctlz.i32(i32 %n, i1 true)
  
  ; Step 2: Subtract from 31 to get the log2 floor
  ; log2(n) = 31 - clz(n)
  %result = sub i32 31, %clz
  
  ret i32 %result
"""

// Returns the logarithm of x with a specified base
func log(x: double, base: double) -> double:
    return ln(x) / ln(base)