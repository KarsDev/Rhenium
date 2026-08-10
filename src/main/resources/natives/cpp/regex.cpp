#include <regex>
#include <string>
#include <vector>
#include <cstring>
#include <cstddef>
#include <limits>

extern "C" {

void* __RegexCompile0(const char* pattern) {
    if (!pattern) {
        return nullptr;
    }

    try {
        std::regex* nr =
            new std::regex(pattern, std::regex_constants::ECMAScript);

        return static_cast<void*>(nr);
    }
    catch (...) {
        return nullptr;
    }
}

bool __RegexBools0(std::regex* regex, int option, const char* input) {
    if (!regex) {
        return false;
    }

    try {
        if (option == 0) { // MATCHES
            if (!input) {
                return false;
            }

            return std::regex_match(input, *regex);
        }
        else if (option == 1) { // SEARCH
            if (!input) {
                return false;
            }

            return std::regex_search(input, *regex);
        }
        else if (option == 2) { // DELETE
            delete regex;
            return true;
        }
        else {
            return false;
        }
    }
    catch (...) {
        return false;
    }
}

const char* __RegexFind0(std::regex* regex, const char* input) {
    if (!regex || !input) {
        return nullptr;
    }

    try {
        thread_local std::string result;

        std::cmatch match;

        if (!std::regex_search(input, match, *regex)) {
            result.clear();
            return nullptr;
        }

        result = match.str(0);
        return result.c_str();
    }
    catch (...) {
        return nullptr;
    }
}

char** __RegexFindAll0(const char* input, const std::regex& pattern, int* count) {
    if (!count) {
        return nullptr;
    }

    *count = 0;

    if (!input) {
        return nullptr;
    }

    try {
        std::vector<std::string> matches;
        std::string str(input);

        for (std::sregex_iterator it(str.begin(), str.end(), pattern);
             it != std::sregex_iterator();
             ++it) {
            matches.push_back(it->str());
        }

        if (matches.size() >
            static_cast<size_t>(std::numeric_limits<int>::max())) {
            return nullptr;
        }

        char** result = new char*[matches.size() + 1];

        for (size_t i = 0; i < matches.size(); ++i) {
            result[i] = new char[matches[i].size() + 1];

            std::memcpy(
                result[i],
                matches[i].c_str(),
                matches[i].size() + 1
            );
        }

        result[matches.size()] = nullptr;
        *count = static_cast<int>(matches.size());

        return result;
    }
    catch (...) {
        return nullptr;
    }
}

int __RegexCount0(const char* input, const std::regex& pattern) {
    if (!input) {
        return 0;
    }

    try {
        std::string str(input);
        int count = 0;

        for (std::sregex_iterator it(str.begin(), str.end(), pattern);
             it != std::sregex_iterator();
             ++it) {
            if (count == std::numeric_limits<int>::max()) {
                return count;
            }

            ++count;
        }

        return count;
    }
    catch (...) {
        return 0;
    }
}

void __RegexFreeAll0(char** result, int count) {
    if (!result) {
        return;
    }

    for (int i = 0; i < count; ++i) {
        delete[] result[i];
    }

    delete[] result;
}

const char* __RegexReplace0(std::regex* regex, const char* input, const char* replacement) {
    if (!regex || !input || !replacement) {
        return nullptr;
    }

    try {
        thread_local std::string result;

        result = std::regex_replace(
            std::string(input),
            *regex,
            replacement
        );

        return result.c_str();
    }
    catch (...) {
        return nullptr;
    }
}

char** __RegexSplit0(const char* input, const std::regex& pattern, int* count) {
    if (!count) {
        return nullptr;
    }

    *count = 0;

    if (!input) {
        return nullptr;
    }

    try {
        std::string str(input);
        std::vector<std::string> parts;

        for (std::sregex_token_iterator it(
                 str.begin(), str.end(), pattern, -1);
             it != std::sregex_token_iterator();
             ++it) {

            parts.push_back(*it);
        }

        if (parts.size() >
            static_cast<size_t>(std::numeric_limits<int>::max())) {
            return nullptr;
        }

        char** result = new char*[parts.size() + 1];

        for (size_t i = 0; i < parts.size(); ++i) {
            result[i] = new char[parts[i].size() + 1];

            std::memcpy(
                result[i],
                parts[i].c_str(),
                parts[i].size() + 1
            );
        }

        result[parts.size()] = nullptr;
        *count = static_cast<int>(parts.size());

        return result;
    }
    catch (...) {
        return nullptr;
    }
}

char** __RegexGroups0(const char* input, const std::regex& pattern, int* count) {
    if (!count) {
        return nullptr;
    }

    *count = 0;

    if (!input) {
        return nullptr;
    }

    try {
        std::string str(input);
        std::smatch match;

        if (!std::regex_search(str, match, pattern)) {
            return nullptr;
        }

        size_t groupCount = match.size() > 0 ? match.size() - 1 : 0;

        if (groupCount >
            static_cast<size_t>(std::numeric_limits<int>::max())) {
            return nullptr;
        }

        char** result = new char*[groupCount + 1];

        for (size_t i = 0; i < groupCount; ++i) {
            const std::string group = match[i + 1].matched
                ? match[i + 1].str()
                : std::string();

            result[i] = new char[group.size() + 1];

            std::memcpy(
                result[i],
                group.c_str(),
                group.size() + 1
            );
        }

        result[groupCount] = nullptr;
        *count = static_cast<int>(groupCount);

        return result;
    }
    catch (...) {
        return nullptr;
    }
}

}