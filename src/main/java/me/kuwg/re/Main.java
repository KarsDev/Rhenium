package me.kuwg.re;

import me.kuwg.re.token.Token;
import me.kuwg.re.token.Tokenizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(final String[] args) throws IOException {
        final File file = new File("test.re"); // TODO: process input arguments

        final InputStream stream = Files.newInputStream(file.toPath());

        final List<Token> tokens = Tokenizer.tokenize(stream);


    }
}