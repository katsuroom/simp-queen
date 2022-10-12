import java.util.regex.Pattern;

public class Inst {
    public String key;
    public Pattern pattern;

    public Inst(String key, Pattern pattern)
    {
        this.key = key;
        this.pattern = pattern;
    }
}
