using color
using list
using file
using FileWriter
using math

struct Image:
    m_width: int
    m_height: int
    m_pixels: List<Color>

impl Image:
    init(width: int, height: int):
        this.m_width = width
        this.m_height = height
        this.m_pixels = init List<Color>(width * height)
    
    func setPixel(x: int, y: int, color: Color):
        m_pixels.insert(y *  this.m_width + x, color)
    
    func savePPM(fileName: str):
        file = init File(fileName)

        if (not file.exists()):
            file.createNew()
        
        writer = init FileWriter(file)
        writer.open()
        writer.write("P3\n")
        writer.write(this.m_width + " " + this.m_height + "\n255\n")
        
        for (i in range(this.m_pixels.length())):
            c = this.m_pixels.get(i)
            k: float = 255.999
            r = Math::clamp(cast<int>(c.r * k), 0, 255)
            g = Math::clamp(cast<int>(c.g * k), 0, 255)
            b = Math::clamp(cast<int>(c.b * k), 0, 255)

            writer.write(r + " " + g + " " + b + "\n")
        
        writer.flush()
        writer.close()

    func width() -> int:
        return this.m_width

    func height() -> int:
        return this.m_height