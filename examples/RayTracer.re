using math
using image

struct Vec3:
    x: float
    y: float
    z: float

struct Ray:
    origin: Vec3
    direction: Vec3

struct Sphere:
    center: Vec3
    radius: float

struct Camera:
    origin: Vec3
    viewportWidth: float
    viewportHeight: float

func vec3(x: float, y: float, z: float) -> Vec3:
    return init Vec3(x, y, z)

func add(a: Vec3, b: Vec3) -> Vec3:
    return vec3( \
        a.x + b.x, \
        a.y + b.y, \
        a.z + b.z \
    )

func sub(a: Vec3, b: Vec3) -> Vec3:
    return vec3( \
        a.x - b.x, \
        a.y - b.y, \
        a.z - b.z \
    )

func mul(v: Vec3, s: float) -> Vec3:
    return vec3( \
        v.x * s, \
        v.y * s, \
        v.z * s \
    )

func dot(a: Vec3, b: Vec3) -> float:
    return \
        a.x * b.x + \
        a.y * b.y + \
        a.z * b.z

func normalize(v: Vec3) -> Vec3:
    l = dot(v, v)
    return mul(v, 1.0 / Math::sqrt(l))

func rayAt(ray: Ray, t: float) -> Vec3:
    return add(ray.origin, mul(ray.direction, t))

func hitSphere(ray: Ray, sphere: Sphere) -> float:

    oc = sub(ray.origin, sphere.center)

    a = dot(ray.direction, ray.direction)
    b = 2.0 * dot(oc, ray.direction)
    c = dot(oc, oc) - sphere.radius * sphere.radius

    d = \
        b * b - \
        4 * a * c

    if (d < 0):
        return cast<float>(-1.0)

    return cast<float>((-b - Math::sqrt(d)) /(2 * a))

func getRay(camera: Camera, u: float, v: float) -> Ray:

    viewport = vec3( \
        camera.viewportWidth, \
        camera.viewportHeight, \
        0 \
    )

    lowerLeft = \
        vec3( \
            -viewport.x / 2, \
            -viewport.y / 2, \
            -1 \
        )

    dir = \
        add( \
            lowerLeft, \
            vec3( \
                viewport.x * u, \
                viewport.y * v, \
                0 \
            ) \
        )

    return init Ray( \
        camera.origin, \
        normalize(dir) \
    )

func rayColor(ray: Ray, sphere: Sphere) -> Color:

    t = hitSphere(ray, sphere)

    if (t > 0):

        p = rayAt(ray, t)

        n = normalize( \
            sub(p, sphere.center) \
        )

        return init Color( \
            (n.x + 1) * 0.5, \
            (n.y + 1) * 0.5, \
            (n.z + 1) * 0.5 \
        )

    unit = normalize(ray.direction)

    a = 0.5 * (unit.y + 1)

    return init Color( \
        (1 - a) + a * 0.5, \
        (1 - a) + a * 0.7, \
        (1 - a) + a * 1.0 \
    )

func main() -> int:

    width = 800
    height = 600

    image = init Image(width, height)

    camera = init Camera( \
        vec3(0, 0, 0), \
        2.0 * width / height, \
        2.0 \
    )

    sphere = init Sphere( \
        vec3(0, 0, -3), \
        1.0 \
    )

    for (y in range(height)):

        for (x in range(width)):

            u: float = x / (width - 1.0)
            v: float = (height - 1 - y) / (height - 1.0)

            ray = getRay(camera, u, v)

            color = rayColor(ray, sphere)

            image.setPixel( \
                x, \
                y, \
                color \
            )

    image.savePPM("sphere.ppm")

    return 0