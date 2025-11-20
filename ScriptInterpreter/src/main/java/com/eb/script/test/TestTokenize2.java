package com.eb.script.test;
import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import java.util.List;

public class TestTokenize2 {
    public static void main(String[] args) {
        String code = "var x: array[5];";
        EbsLexer lexer = new EbsLexer();
        List<EbsToken> tokens = lexer.tokenize(code);
        
        for (int i = 0; i < tokens.size(); i++) {
            EbsToken token = tokens.get(i);
            System.out.println(i + ": Type: '" + token.type + "', Literal: '" + token.literal + "'");
        }
    }
}
