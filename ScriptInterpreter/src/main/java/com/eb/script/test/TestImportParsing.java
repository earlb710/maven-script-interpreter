package com.eb.script.test;

import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import java.util.List;

public class TestImportParsing {
    public static void main(String[] args) {
        EbsLexer lexer = new EbsLexer();
        
        // Test 1: Import with subdirectory
        String test1 = "import \"util/stringUtil.ebs\";";
        System.out.println("Test 1: " + test1);
        List<EbsToken> tokens1 = lexer.tokenize(test1);
        for (EbsToken token : tokens1) {
            System.out.println("  " + token.type + ": " + token.literal);
        }
        System.out.println();
        
        // Test 2: Import with spaces in name (double quotes)
        String test2 = "import \"my utils/string util.ebs\";";
        System.out.println("Test 2: " + test2);
        List<EbsToken> tokens2 = lexer.tokenize(test2);
        for (EbsToken token : tokens2) {
            System.out.println("  " + token.type + ": " + token.literal);
        }
        System.out.println();
        
        // Test 3: Import with single quotes
        String test3 = "import 'util/stringUtil.ebs';";
        System.out.println("Test 3: " + test3);
        List<EbsToken> tokens3 = lexer.tokenize(test3);
        for (EbsToken token : tokens3) {
            System.out.println("  " + token.type + ": " + token.literal);
        }
    }
}
