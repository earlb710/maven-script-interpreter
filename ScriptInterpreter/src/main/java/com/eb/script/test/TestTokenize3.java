package com.eb.script.test;
import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import java.util.List;

public class TestTokenize3 {
    public static void main(String[] args) {
        String code = "var x: int[5]; var y: string[3]; var z: array[2];";
        EbsLexer lexer = new EbsLexer();
        List<EbsToken> tokens = lexer.tokenize(code);
        
        for (int i = 0; i < tokens.size(); i++) {
            EbsToken token = tokens.get(i);
            if (token.literal.equals("int") || token.literal.equals("string") || token.literal.equals("array")) {
                System.out.println(i + ": Type: '" + token.type + "', Literal: '" + token.literal + "', DataType: " + token.type.getDataType());
            }
        }
    }
}
