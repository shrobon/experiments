package org.example;

public class Re2cWordCounter {
    public static int countWords(String input) {
        int count = 0;
        int yycursor = 0;
        int yylimit = input.length();
        int yymarker = 0;

        loop: while (yycursor < yylimit) {
            /*!re2c
                re2c:define:YYCTYPE = "char";
                // FIX: Check bounds before calling charAt!
                re2c:define:YYPEEK = "yycursor < yylimit ? input.charAt(yycursor) : 0";
                re2c:define:YYSKIP = "yycursor++";
                re2c:define:YYBACKUP = "yymarker = yycursor";
                re2c:define:YYRESTORE = "yycursor = yymarker";
                re2c:yyfill:enable = 0;

                [a-zA-Z0-9]+ { count++; continue loop; }
                [ \t\r\n]+   { continue loop; }
                *            { continue loop; }
            */
        }
        return count;
    }
}