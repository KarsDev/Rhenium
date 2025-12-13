package me.kuwg.re.frontend;

import me.kuwg.re.ast.AST;
import me.kuwg.re.parser.ASTParser;
import me.kuwg.re.token.Tokenizer;

import java.io.File;
import java.nio.file.Files;

public final class Frontend {

    private final File input;

    public Frontend(File input) {
        this.input = input;
    }

    public AST parse() throws Exception {
        String source = Files.readString(input.toPath());
        var tokens = Tokenizer.tokenize(source);

        ASTParser parser = new ASTParser(input.getPath(), tokens, true);
        return parser.parse();
    }
}
