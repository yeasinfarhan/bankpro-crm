package com.bankpro.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.StringWriter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "=1+1|'=1+1",
            "+SUM(A1)|'+SUM(A1)",
            "-2+3|'-2+3",
            "@cmd|'@cmd",
            "Normal name|Normal name",
            "+8801712345678|+8801712345678",
            "-50.25|-50.25"
    })
    void neutralisesFormulasButKeepsPlainNumbers(String input, String expected) {
        assertThat(CsvWriter.neutraliseFormula(input)).isEqualTo(expected);
    }

    @Test
    void quotesCommasQuotesAndNewlines() throws Exception {
        StringWriter out = new StringWriter();
        new CsvWriter(out).row(Arrays.asList("a,b", "say \"hi\"", "line1\nline2", null, 42));
        assertThat(out.toString()).isEqualTo("\"a,b\",\"say \"\"hi\"\"\",\"line1\nline2\",,42\r\n");
    }
}
