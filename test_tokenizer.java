import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import java.util.List;

public class test_tokenizer {
    public static void main(String[] args) {
        EbsLexer lexer = new EbsLexer();
        String code = "call #debug.assert(scopeVisibleTest.VisibleSet.testVar1 == \"visible1\", \"TEST 1\");";
        System.out.println("Code: " + code);
        System.out.println("\nTokens:");
        List<EbsToken> tokens = lexer.tokenize(code);
        for (int i = 0; i < tokens.size() && i < 15; i++) {
            EbsToken token = tokens.get(i);
            System.out.println(i + ": " + token.type + " (" + token.literal + ")");
        }
    }
}
