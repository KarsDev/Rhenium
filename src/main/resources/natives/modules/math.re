/*
    sin
    cos
    tan

    asin
    acos
    atan

    sinh
    cosh
    tanh

    toRadians
    toDegrees
*/

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
    if val < 0:
        return -val
    return val

// Returns the absolute value of a long
func abs(val: long) -> long:
    if val < 0:
        return -val
    return val

// Returns the absolute value of a double
func abs(val: double) -> double:
    if val < 0:
        return -val
    return val

// Returns the absolute value of a float
func abs(val: float) -> float:
    if val < 0:
        return -val
    return val

// Returns the angle in degrees
func toDegrees(rad: double) -> double:
    return rad * RAD_TO_DEG

// Returns the angle in radians
func toRadians(deg: double) -> double:
    return deg * DEG_TO_RAD

// Builtins:

_Builtin func sin(x: double) -> double = """
entry:
    %res = call double @llvm.sin.f64(double %x)
    ret double %res
"""

_Builtin func cos(x: double) -> double = """
entry:
    %res = call double @llvm.cos.f64(double %x)
    ret double %res
"""

_Builtin func tan(x: double) -> double = """
entry:
    %sin = call double @llvm.sin.f64(double %x)
    %cos = call double @llvm.cos.f64(double %x)
    %res = fdiv double %sin, %cos
    ret double %res
"""

_Builtin func asin(x: double) -> double = """
entry:
    %res = call double @llvm.asin.f64(double %x)
    ret double %res
"""

_Builtin func acos(x: double) -> double = """
entry:
    %res = call double @llvm.acos.f64(double %x)
    ret double %res
"""

_Builtin func atan(x: double) -> double = """
entry:
    %res = call double @llvm.atan.f64(double %x)
    ret double %res
"""

_Builtin func sinh(x: double) -> double = """
entry:
    %res = call double @llvm.sinh.f64(double %x)
    ret double %res
"""

_Builtin func cosh(x: double) -> double = """
entry:
    %res = call double @llvm.cosh.f64(double %x)
    ret double %res
"""

_Builtin func tanh(x: double) -> double = """
entry:
    %res = call double @llvm.tanh.f64(double %x)
    ret double %res
"""

_Builtin func pow(x: double, y: double) -> double = """
entry:
    %res = call double @llvm.pow.f64(double %x, double %y)
    ret double %res
"""

_Builtin func sqrt(x: double) -> double = """
entry:
    %res = call double @llvm.sqrt.f64(double %x)
    ret double %res
"""

_Builtin func cbrt(x: double) -> double = """
entry:
    %res = call double @llvm.cbrt.f64(double %x)
    ret double %res
"""
