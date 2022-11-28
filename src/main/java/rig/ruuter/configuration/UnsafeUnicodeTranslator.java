package rig.ruuter.configuration;

import org.apache.commons.text.translate.CodePointTranslator;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class UnsafeUnicodeTranslator extends CodePointTranslator {
    private final Set<Integer> whitelist;

    public UnsafeUnicodeTranslator(String allowedCharacters) {
        whitelist = allowedCharacters.codePoints().boxed().collect(toSet());
    }

    @Override
    public boolean translate(final int codepoint, final Writer out) throws IOException {
        if (!whitelist.contains(codepoint)) {
            out.write("0x" + hex(codepoint));
            return true;
        }
        return false;
    }
}
