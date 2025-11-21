package com.eb.script.test;

import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import java.util.List;

public class TestTokenizer {
    public static void main(String[] args) {
        String code = "x++;";
        System.out.println("Input: '" + code + "'");
        System.out.println("Length: " + code.length());
        EbsLexer lexer = new EbsLexer();
        List<EbsToken> tokens = lexer.tokenize(code);
        
        System.out.println("Total tokens: " + tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            EbsToken token = tokens.get(i);
            System.out.println(i + ": Type: '" + token.type + "', Literal: '" + token.literal + "'");
        }
    }
}
