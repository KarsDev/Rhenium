package me.kuwg.re.frontend;

import me.kuwg.re.ast.AST;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.token.Tokenizer;
import me.kuwg.re.type.TypeRef;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public final class Frontend {
    private final File input;
    public Map<String, TypeRef> typeMap;

    public Frontend(File input) {
        this.input = input;
    }

    public AST parse() throws Exception {
        String source = Files.readString(input.toPath());
        var tokens = Tokenizer.tokenize(source);

        ASTParser parser = new ASTParser(input.getPath(), tokens);

        typeMap = parser.typeMap;

        return parser.parse();
    }
}
