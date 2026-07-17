using map
using number

_NativeCPP("network") str BNET_00(op: int, id: str, arg1: str, arg2: str)

/*
  Represents a TCP network connection.
  
  A Network object manages a connection to a remote host and
  provides methods for sending and receiving raw data.

  Connections must be explicitly opened before use and closed
  when no longer needed.

  Example:
  conn = Network("example", "example.com", 80)
  
  if (conn.open()):
      conn.send("Hello")
      println(conn.receive())
      conn.close()

   Notes:
   - open() must be called before send() or receive().
   - close() should be called when finished.
   - receive() returns the next available chunk of data.
   - receiveAll() reads until no more data is available.
*/
struct Network:
    id: str
    host: str
    port: int

    isOpen: bool

impl Network:
    // Creates a network connection descriptor
    init(id: str, host: str, port: int):
        this.id = id
        this.host = host
        this.port = port
        this.isOpen = false

    // Opens the connection and returns true if it was successfully established
    func open() -> bool:
        if (this.isOpen):
            return false

        success = BNET_00(0, this.id, this.host, intToStr(this.port)) == "1"

        if (success):
            this.isOpen = true

        return success

    // Sends data across the connection
    func send(data: str) -> bool:
        if (this.isOpen == false):
            raise "Use Network#open() before Network#send"

        return BNET_00(1, this.id, data, "") == "1"

    // Receives a chunk of data from the connection
    func receive() -> str:
        if (this.isOpen == false):
            raise "Use Network#open() before Network#receive"

        return BNET_00(2, this.id, "", "")

    // Closes the connection
    func close() -> bool:
        if (this.isOpen == false):
            raise "Use Network#open() before Network#close"

        success = BNET_00(3, this.id, "", "") == "1"

        if (success):
            this.isOpen = false

        return success

    // Returns true if the connection is currently open
    func isConnected() -> bool:
        return this.isOpen

    // Reads all available data until the remote side stops sending
    func receiveAll() -> str:
        if (not this.isOpen):
            raise "Use Network#open() before Network#receiveAll"

        data: mut = ""

        while (true):
            part = BNET_00(2, this.id, "8192", "")

            if (len(part) == 0):
                break

            data = data + part

        return data

/*
  Reppresents an HHTP response
  
  Contains:
   - status  : HHTP status code
   - headers : response headers
   - body    : response body
*/
struct HttpResponse:
        status: int
        headers: HashMap<str, str>
        body: str

/*
  HHTP utility functions
  
  Provides:
    - DNS lookups
    - HTTP response parsing
    - Chunked transfer decoding
    - Simple HTTP GET requests
*/
namespace Http:

    // Resolves a hostname to an IP address
    func dns(host: str) -> str:
        return BNET_00(4, host, "", "")

    // Decodes an HHTP body encoded with
    // Transfer-Encoding: chunked
    func decodeChunkedBody(body: str) -> str:
        result: mut = ""
        i: mut = 0

        while (i < len(body)):
            lineEnd = strIndexOf(body, "\r\n", i)
            if (lineEnd == -1):
                break

            sizeText = strTrim(strSubRange(body, i, lineEnd), 0)
            size: int = Number::parseLongBase(sizeText, 16)

            if (size <= 0):
                break

            start = lineEnd + 2
            end: mut = start + size

            if (end > len(body)):
                end = len(body)

            result = result + strSubRange(body, start, end)

            i = end + 2

        return result

    // Parses araw HHTP response into a structured object
    func parseHttpResponse(raw: str) -> HttpResponse:
        tm = init HashMap<str, str>()
        response = init HttpResponse(0, tm, "")

        headerEnd = strIndexOf(raw, "\r\n\r\n")

        if (headerEnd == -1):
            raise "Invalid HTTP response"

        headerText = strSubRange(raw, 0, headerEnd)
        bodyText = strSubRange(raw, headerEnd + 4, len(raw))

        dynLines = strSplit(headerText, "\r\n")

        lines = dynLines.values
        linesLen = dynLines.size

        if (linesLen == 0):
            raise "Invalid HTTP response"

        statusLine = lines[0]
        statusPartsDyn = strSplit(statusLine, " ")

        if (statusPartsDyn.size < 2):
            raise "Invalid HTTP status line"

        response.status = Number::parseInt(statusPartsDyn.values[1])

        i: mut = 1
        while (i < linesLen):
            line = lines[i]
            colon = strIndexOf(line, ":")
        
            if (colon != -1):
                key = strTrim(strSubRange(line, 0, colon))
                value = strTrim(strSubRange(line, colon + 1, len(line)))
                response.headers.put(key, value)
        
            i += 1

        
        transferEncoding: mut = ""
        if (response.headers.containsKey("Transfer-Encoding")):
            transferEncoding = response.headers.get("Transfer-Encoding")

        if (strToLower(transferEncoding) == "chunked"):
            response.body = Http::decodeChunkedBody(bodyText)
        else:
            response.body = bodyText

        return response

    // Sends an  HHTP GET requests and returns the response
    func httpGet(host: str, path: str, port: int) -> HttpResponse:
        conn = init Network("http_" + host + path, host, port)

        if (conn.open() == false):
            raise "Failed to open HTTP connection"

        hostHeader: mut = host if port == 80 else host + ":" + intToStr(port)

        request = "GET " + path + " HTTP/1.1\r\n" + "Host: " + hostHeader + "\r\n" + "Connection: close\r\n" + "User-Agent: BLang/1.0\r\n" + "\r\n"

        if (conn.send(request) == false):
            conn.close()
            raise "Failed to send HTTP request"

        raw = conn.receive()
        conn.close()

        return Http::parseHttpResponse(raw)