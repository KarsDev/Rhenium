using graphics.color
using collections.list
using file.file
using file.FileWriter
using math.math

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

        if not file.exists():
            file.createNew()
        
        writer = init FileWriter(file)
        writer.open()
        writer.write("P3\n")
        writer.write(this.m_width + " " + this.m_height + "\n255\n")
        
        for i in range(this.m_pixels.length()):
            c = this.m_pixels.get(i)
            k: float = 255.999
            r = Math::clamp(cast<int>(c.r * k), 0, 255)
            g = Math::clamp(cast<int>(c.g * k), 0, 255)
            b = Math::clamp(cast<int>(c.b * k), 0, 255)

            writer.write(r + " " + g + " " + b + "\n")
        
        writer.flush()
        writer.close()

    func savePNG(fileName: str):
        file = init File(fileName)
        if not file.exists():
            file.createNew()

        writer = init FileWriter(file)
        writer.open()

        // 1. Signature
        sig = init List<int>()
        for b in [137, 80, 78, 71, 13, 10, 26, 10]:
            sig.add(b)
        this.writeList(writer, sig)

        // 2. IHDR
        ihdr = init List<int>()
        this.putU32(ihdr, this.m_width)
        this.putU32(ihdr, this.m_height)
        ihdr.add(8)   // bit depth
        ihdr.add(2)   // color type: RGB
        ihdr.add(0)   // compression
        ihdr.add(0)   // filter method
        ihdr.add(0)   // no interlace
        this.writeChunk(writer, "IHDR", ihdr)

        // 3. Raw scanlines: each row = filter byte 0 + RGB bytes
        raw = init List<int>()
        k: float = 255.999
        for y in range(this.m_height):
            raw.add(0)
            for x in range(this.m_width):
                c = this.m_pixels.get(y * this.m_width + x)
                raw.add(Math::clamp(cast<int>(c.r * k), 0, 255))
                raw.add(Math::clamp(cast<int>(c.g * k), 0, 255))
                raw.add(Math::clamp(cast<int>(c.b * k), 0, 255))

        // 4. zlib stream using stored (uncompressed) deflate blocks
        z = init List<int>()
        z.add(0x78)
        z.add(0x01)
        pos: mut = 0
        total = raw.length()
        while pos < total:
            n = Math::min(65535, total - pos)
            final = 1 if pos + n >= total else 0
            z.add(final)
            z.add(n & 255)
            z.add((n >> 8) & 255)
            z.add((~n) & 255)
            z.add(((~n) >> 8) & 255)
            for i in range(n):
                z.add(raw.get(pos + i))
            pos = pos + n
        this.putU32(z, this.adler32(raw))
        this.writeChunk(writer, "IDAT", z)

        // 5. IEND
        this.writeChunk(writer, "IEND", init List<int>())

        writer.flush()
        writer.close()

    func putU32(out: List<int>, v: int):          // big-endian
        out.add((v >> 24) & 255)
        out.add((v >> 16) & 255)
        out.add((v >> 8) & 255)
        out.add(v & 255)

    func writeChunk(writer: FileWriter, typeStr: str, data: List<int>):
        buf = init List<int>()
        this.putU32(buf, data.length())
        body = init List<int>()                    // type + data (this is what the CRC covers)
        for i in range(4):
            body.add(cast<int>(typeStr[i])) // ASCII code of each type char
        for i in range(data.length()):
            body.add(data.get(i))
        for i in range(body.length()):
            buf.add(body.get(i))
        this.putU32(buf, this.crc32(body))
        this.writeList(writer, buf)

    func adler32(data: List<int>) -> int:
        a: mut = 1
        b: mut = 0
        for i in range(data.length()):
            a = (a + data.get(i)) % 65521
            b = (b + a) % 65521
        return (b << 16) | a

    func crc32(data: List<int>) -> int:
        crc: mut = 0xFFFFFFFF
        for i in range(data.length()):
            crc = crc ^ data.get(i)
            for j in range(8):
                if (crc & 1) == 1:
                    crc = (crc >> 1) ^ 0xEDB88320
                else:
                    crc = crc >> 1
        return cast<int>(crc ^ 0xFFFFFFFF)

    func writeList(writer: FileWriter, bytes: List<int>):
        digits = "0123456789abcdef"
        hex: mut = ""
        for i in range(bytes.length()):
            b = bytes.get(i)
            hex = hex + digits[(b >> 4) & 15]
            hex = hex + digits[b & 15]

            if len(hex) >= 8192:
                writer.writeBytes(hex)
                hex = ""

        writer.writeBytes(hex)

    func width() -> int:
        return this.m_width

    func height() -> int:
        return this.m_height