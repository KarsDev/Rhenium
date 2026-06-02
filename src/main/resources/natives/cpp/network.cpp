#include <string>
#include <unordered_map>
#include <mutex>

#ifdef _WIN32
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #pragma comment(lib, "ws2_32.lib")
#else
    #include <arpa/inet.h>
    #include <netdb.h>
    #include <sys/socket.h>
    #include <unistd.h>
#endif

std::unordered_map<std::string, int> openConnections;
std::mutex networkMutex;

#ifdef _WIN32
bool networkInitialized = false;

void initializeNetwork()
{
    if (networkInitialized) return;

    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);

    networkInitialized = true;
}
#endif

int getSocket(const std::string& id)
{
    auto it = openConnections.find(id);
    if (it == openConnections.end())
        return -1;

    return it->second;
}

bool openConnection(
    const std::string& id,
    const std::string& host,
    int port)
{
    std::lock_guard<std::mutex> lock(networkMutex);

#ifdef _WIN32
    initializeNetwork();
#endif

    if (openConnections.find(id) != openConnections.end())
        return false;

    struct addrinfo hints{};
    struct addrinfo* result = nullptr;

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;

    std::string portStr = std::to_string(port);

    if (getaddrinfo(
        host.c_str(),
        portStr.c_str(),
        &hints,
        &result) != 0)
    {
        return false;
    }

    int sock = -1;

    for (auto ptr = result; ptr != nullptr; ptr = ptr->ai_next)
    {
        sock = (int)socket(
            ptr->ai_family,
            ptr->ai_socktype,
            ptr->ai_protocol);

        if (sock < 0)
            continue;

        if (connect(
            sock,
            ptr->ai_addr,
            (int)ptr->ai_addrlen) == 0)
        {
            break;
        }

#ifdef _WIN32
        closesocket(sock);
#else
        close(sock);
#endif

        sock = -1;
    }

    freeaddrinfo(result);

    if (sock < 0)
        return false;

    openConnections[id] = sock;
    return true;
}

bool sendData(
    const std::string& id,
    const std::string& data)
{
    std::lock_guard<std::mutex> lock(networkMutex);

    int sock = getSocket(id);
    if (sock < 0)
        return false;

    const char* ptr = data.c_str();
    size_t total = 0;
    size_t size = data.size();

    while (total < size)
    {
        int sent = send(
            sock,
            ptr + total,
            (int)(size - total),
            0);

        if (sent <= 0)
            return false;

        total += (size_t)sent;
    }

    return true;
}

std::string receiveData(
    const std::string& id)
{
    int sock;

    {
        std::lock_guard<std::mutex> lock(networkMutex);

        sock = getSocket(id);

        if (sock < 0)
            return "";
    }

#ifdef _WIN32
    DWORD timeout = 3000; // 3 seconds
    setsockopt(
        sock,
        SOL_SOCKET,
        SO_RCVTIMEO,
        (const char*)&timeout,
        sizeof(timeout));
#else
    timeval timeout{};
    timeout.tv_sec = 3;
    timeout.tv_usec = 0;

    setsockopt(
        sock,
        SOL_SOCKET,
        SO_RCVTIMEO,
        &timeout,
        sizeof(timeout));
#endif

    std::string result;
    char buffer[4096];

    while (true)
    {
        int bytes = recv(
            sock,
            buffer,
            sizeof(buffer),
            0);

        if (bytes > 0)
        {
            result.append(buffer, bytes);

            // If less than buffer size arrived,
            // assume we've consumed everything currently available.
            if (bytes < (int)sizeof(buffer))
                break;
        }
        else
        {
            break;
        }
    }

    return result;
}
bool closeConnection(
    const std::string& id)
{
    std::lock_guard<std::mutex> lock(networkMutex);

    auto it = openConnections.find(id);
    if (it == openConnections.end())
        return false;

#ifdef _WIN32
    closesocket(it->second);
#else
    close(it->second);
#endif

    openConnections.erase(it);
    return true;
}

std::string resolveHost(
    const std::string& host)
{
    struct addrinfo hints{};
    struct addrinfo* result = nullptr;

    hints.ai_family = AF_INET;

    if (getaddrinfo(
        host.c_str(),
        nullptr,
        &hints,
        &result) != 0)
    {
        return "";
    }

    char ip[INET_ADDRSTRLEN];

    auto* addr =
        (struct sockaddr_in*)result->ai_addr;

    inet_ntop(
        AF_INET,
        &addr->sin_addr,
        ip,
        sizeof(ip));

    freeaddrinfo(result);

    return std::string(ip);
}

extern "C"
{
    const int NET_OPEN  = 0;
    const int NET_SEND  = 1;
    const int NET_RECV  = 2;
    const int NET_CLOSE = 3;
    const int NET_DNS   = 4;

    char* BNET_00(
        int op,
        char* p1,
        char* p2,
        char* p3)
    {
        static std::string buffer;

        std::string a(p1 ? p1 : "");
        std::string b(p2 ? p2 : "");
        std::string c(p3 ? p3 : "");

        switch(op)
        {
            case NET_OPEN:
                return openConnection(
                    a,
                    b,
                    std::stoi(c))
                    ? (char*)"1"
                    : (char*)"0";

            case NET_SEND:
                return sendData(a, b)
                    ? (char*)"1"
                    : (char*)"0";

            case NET_RECV:
                buffer = receiveData(a);
                return (char*)buffer.c_str();

            case NET_CLOSE:
                return closeConnection(a)
                    ? (char*)"1"
                    : (char*)"0";

            case NET_DNS:
                buffer = resolveHost(a);
                return (char*)buffer.c_str();
        }

        return (char*)"0";
    }
}