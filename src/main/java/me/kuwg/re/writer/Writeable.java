package me.kuwg.re.writer;

public interface Writeable {
    String TAB = "  ";
    String NEWLINE = "\n";

    void write(StringBuilder sb, String indent);
}
