import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Queen {

    Inst[] patternMath, patterns;

    Pattern dataRegex = Pattern.compile("(.+?)([+-])(.+)");
    Pattern boolRegex = Pattern.compile("(.+?)(==|!=|>=|>|<=|<)(.+)");
    Pattern commentRegex = Pattern.compile("(.*?)/+(.*)");
    Pattern hexRegex = Pattern.compile("0x[0-9a-f]+");
    Pattern intRegex = Pattern.compile("-?[0-9]+");
    Pattern charRegex = Pattern.compile("'[a-zA-Z]'|\"[a-zA-Z]\"");
    //Pattern stringRegex = Pattern.compile("'(.+?)'|\"(.+?)\"");

    ArrayList<String> registers = new ArrayList<>();
    String tempRegister = "$v1";

    String labelPrefix = "simp_label_";
    int labelCounter = 0;
    final int indentSize = 4;

    Stack<Block> blocks = new Stack<>();

    String comment = "";

    public Queen()
    {
        // Init Registers
        registers.add("$zero");
        registers.add("$v0");
        registers.add("$v1");
        for(int i = 0; i < 4; i++) registers.add("$a" + i);     // a0 - a3
        for(int i = 0; i < 8; i++) registers.add("$t" + i);     // t0 - t7
        for(int i = 0; i < 9; i++) registers.add("$s" + i);     // s0 - s8
        for(int i = 8; i < 10; i++) registers.add("$t" + i);    // t8 - t9
        registers.add("$gp");
        registers.add("$sp");
        registers.add("$fp");

        // MATH
        patternMath = new Inst[] {
                new Inst("add", Pattern.compile("(.+?)=(.+?)(\\s+add\\s+|\\+)(.+)")),
                new Inst("sub", Pattern.compile("(.+?)=(.+?)(\\s+sub\\s+|-)(.+)")),
                new Inst("mul", Pattern.compile("(.+?)=(.+?)(\\s+mul\\s+|\\*)(.+)")),
                new Inst("div", Pattern.compile("(.+?)=(.+?)(\\s+div\\s+|/)(.+)")),
                new Inst("addeq", Pattern.compile("(.+?)\\+=(.+)")),
                new Inst("subeq", Pattern.compile("(.+?)-=(.+)")),
                new Inst("muleq", Pattern.compile("(.+?)\\*=(.+)")),
                new Inst("diveq", Pattern.compile("(.+?)/=(.+)")),
                new Inst("mod", Pattern.compile("(.+?)=(.+?)(\\s+mod\\s+|%)(.+)")),
                new Inst("sll", Pattern.compile("(.+?)=(.+?)(\\s+sll\\s+|<<)(.+)")),
                new Inst("srl", Pattern.compile("(.+?)=(.+?)(\\s+srl\\s+|>>)(.+)")),
                new Inst("sra", Pattern.compile("(.+?)=(.+?)(\\s+sra\\s+|>>!)(.+)")),
                new Inst("and", Pattern.compile("(.+?)=(.+?)(\\s+and\\s+|&)(.+)")),
                new Inst("or", Pattern.compile("(.+?)=(.+?)(\\s+or\\s+|\\|)(.+)")),
                new Inst("xor", Pattern.compile("(.+?)=(.+?)(\\s+xor\\s+|\\^)(.+)")),
                new Inst("eq", Pattern.compile("(.+?)=(.+)")),
                new Inst("not", Pattern.compile("(.+?)=\\s*(not\\s+|!)\\s*(.+)"))
        };

        patterns = new Inst[] {
                // DATA
                new Inst("la", Pattern.compile("(.+?).la\\((.+?)\\)")),
                new Inst("lw", Pattern.compile("(.+?).lw\\((.+?)\\)")),
                new Inst("sw", Pattern.compile("(.+?).sw\\((.+?)\\)")),
                new Inst("lb", Pattern.compile("(.+?).lb\\((.+?)\\)")),
                new Inst("sb", Pattern.compile("(.+?).sb\\((.+?)\\)")),
                new Inst("push", Pattern.compile("stack\\.push\\((.+?)\\)")),
                new Inst("pop", Pattern.compile("stack\\.pop\\((.+?)\\)")),

                // BLOCKS
                new Inst("if", Pattern.compile("if\\s+(.+?)\\s*:")),
                new Inst("while", Pattern.compile("while\\s+(.*?)\\s*:")),
                new Inst("func", Pattern.compile("func\\s+(.+?)\\(\\)\\s*:")),
                new Inst("end", Pattern.compile("end")),

                // JUMP
                new Inst("j", Pattern.compile("j\\s+(.+?)\\(\\)")),
                new Inst("jal", Pattern.compile("jal\\s+(.+?)\\(\\)")),
                new Inst("return", Pattern.compile("return(\\s+.+|\\s*)")),

                // MACRO
                new Inst("exit", Pattern.compile("exit")),
                new Inst("asm", Pattern.compile("asm:\\s+(.+)"))
        };
        
    }

    public String eval(String input)
    {
        // Preserve comments
        comment = "";
        if(input.contains("//"))
        {
            Matcher m = commentRegex.matcher(input);
            if(m.find())
            {
                input = m.group(1).trim();
                comment = m.group(2).trim();
            }
            if(input.equals("")) return format("");
        }

        // Enter text section


        // MATH
        if(input.contains("="))
        {
            for(Inst inst : patternMath)
            {
                if(!inst.pattern.matcher(input).matches())
                    continue;

                Matcher m = inst.pattern.matcher(input);
                if (!m.find()) continue;

                String dest = m.group(1).trim();
                if(!isRegister(dest)) continue;

                String src1 = m.group(2).trim();
                String src2 = m.groupCount() >= 4 ? m.group(4).trim() : "";

                switch (inst.key) {
                    case "add":
                    case "and":
                    case "or":
                    case "xor":
                    {
                        if(isRegister(src1))
                        {
                            if(isRegister(src2)) return format(inst.key + " " + dest + ", " + src1 + ", " + src2);
                            if(isImmediate(src2)) return format(inst.key + "i " + dest + ", " + src1 + ", " + src2);
                        }
                        break;
                    }
                    case "sub":
                    {
                        if(src1.equals("")) continue; // a = -b

                        if(isRegister(src1))
                        {
                            if(isRegister(src2)) return format("sub " + dest + ", " + src1 + ", " + src2);
                            if(isImmediate(src2))
                                return format("li " + tempRegister + ", " + src2) + "\n" +
                                        format("sub " + dest + ", " + src1 + ", " + tempRegister);
                        }
                        break;
                    }
                    case "mul":
                    {
                        if(isRegister(src1))
                        {
                            if(isRegister(src2)) return format("mul " + dest + ", " + src1 + ", " + src2);
                            if(isImmediate(src2))
                                return format("li " + tempRegister + ", " + src2) + "\n" +
                                        ("mul " + dest + ", " + src1 + ", " + tempRegister);
                        }
                        break;
                    }
                    case "div":
                    {
                        if(isRegister(src1))
                        {
                            if(isRegister(src2))
                                return format("div " + src1 + ", " + src2) + "\n" +
                                        format("mflo " + dest);
                            if(isImmediate(src2))
                                return format("li " + tempRegister + ", " + src2) + "\n" +
                                        format("div " + src1 + ", " + tempRegister) + "\n" +
                                        format("mflo " + dest);
                        }
                        break;
                    }
                    case "mod":
                    {
                        if(isRegister(src1))
                        {
                            if(isRegister(src2))
                                return format("div " + src1 + ", " + src2) + "\n" +
                                        format("mfhi " + dest);
                            if(isImmediate(src2))
                                return format("li " + tempRegister + ", " + src2) + "\n" +
                                        format("div " + src1 + ", " + tempRegister) + "\n" +
                                        format("mfhi " + dest);
                        }
                        break;
                    }
                    case "sll":
                    case "srl":
                    case "sra":
                    {
                        if(isRegister(src1))
                        {
                            if(isRegister(src2)) return format(inst.key + "v " + dest + ", " + src1 + ", " + src2);
                            if(isImmediate(src2)) return format(inst.key + " " + dest + ", " + src1 + ", " + src2);
                        }
                        break;
                    }
                    case "not":
                    {
                        src1 = m.group(3).trim();
                        if(isRegister(src1)) return format("not " + dest + ", " + src1);
                    }
                    case "eq":
                    {
                        if(isRegister(src1)) return format("move " + dest + ", " + src1);
                        if(isImmediate(src1)) return format("li " + dest + ", " + src1);

                        // set
                        Matcher boolMatcher = boolRegex.matcher(src1);
                        if(!boolMatcher.find()) continue;

                        src1 = boolMatcher.group(1).trim();
                        if(!isRegister(src1)) continue;
                        src2 = boolMatcher.group(3).trim();

                        String operator = boolMatcher.group(2);

                        switch(operator)
                        {
                            case "==":
                            {
                                if(isRegister(src2)) return format("seq " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2))
                                    return format("li " + tempRegister + ", " + src2) + "\n" +
                                            format("seq " + dest + ", " + src1 + ", " + tempRegister);
                                break;
                            }
                            case "!=":
                            {
                                if(isRegister(src2)) return format("sne " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2))
                                    return format("li " + tempRegister + ", " + src2) + "\n" +
                                            format("sne " + dest + ", " + src1 + ", " + tempRegister);
                                break;
                            }
                            case ">=":
                            {
                                if(isRegister(src2)) return format("sge " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2))
                                    return format("li " + tempRegister + ", " + src2) + "\n" +
                                            format("sge " + dest + ", " + src1 + ", " + tempRegister);
                                break;
                            }
                            case ">":
                            {
                                if(isRegister(src2)) return format("sgt " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2))
                                    return format("li " + tempRegister + ", " + src2 + "\n") +
                                            format("sgt " + dest + ", " + src1 + ", " + tempRegister);
                                break;
                            }
                            case "<=":
                            {
                                if(isRegister(src2)) return format("sle " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2))
                                    return format("li " + tempRegister + ", " + src2 + "\n") +
                                            format("sle " + dest + ", " + src1 + ", " + tempRegister);
                                break;
                            }
                            case "<":
                            {
                                if(isRegister(src2)) return format("slt " + dest + ", " + src1 + ", " + src2);
                                if(isImmediate(src2)) return format("slti " + dest + ", " + src1 + ", " + src2);
                                break;
                            }
                        }
                        break;
                    }
                    case "addeq":
                    {
                        return eval(dest + "=" + dest + "+" + src1);
                    }
                    case "subeq":
                    {
                        return eval(dest + "=" + dest + "-" + src1);
                    }
                    case "muleq":
                    {
                        return eval(dest + "=" + dest + "*" + src1);
                    }
                    case "diveq":
                    {
                        return eval(dest + "=" + dest + "/" + src1);
                    }
                }
            }
        }

        // DATA, BLOCKS, JUMP
        for(Inst inst : patterns)
        {
            if(!inst.pattern.matcher(input).matches())
                continue;

            Matcher m = inst.pattern.matcher(input);
            if (!m.find()) return "error";

            switch (inst.key)
            {
                case "la":
                {
                    String dest = m.group(1).trim();
                    if(!isRegister(dest)) return "error";
                    return format("la " + dest + ", " + m.group(2).trim());
                }
                case "lw":
                case "sw":
                case "lb":
                case "sb":
                {
                    String dest = m.group(1).trim();
                    if(!isRegister(dest)) return "error";

                    String arg = m.group(2).trim();
                    if(isRegister(arg)) return inst.key + " " + dest + ", " + "0(" + arg + ")";

                    Matcher dataMatcher = dataRegex.matcher(arg);
                    if (!dataMatcher.find()) return "error";

                    String operator = dataMatcher.group(2).equals("+") ? "" : "-";
                    if (!isRegister(dataMatcher.group(1).trim())) return "error";
                    if (!isImmediate(dataMatcher.group(3).trim())) return "error";

                    return inst.key + " " + dest + ", " + operator + dataMatcher.group(3).trim() + "(" + dataMatcher.group(1).trim() + ")";
                }
                case "push":
                {
                    String reg = m.group(1).trim();
                    if(!isRegister(reg)) break;
                    return format("addi $sp, $sp, -4") + "\n" +
                            format("sw " + reg + ", 0($sp)");
                }
                case "pop":
                {
                    String reg = m.group(1).trim();
                    if(!isRegister(reg)) break;
                    return format("lw " + reg + ", 0($sp)") + "\n" +
                            format("addi $sp, $sp, 4");
                }
                case "if":
                case "while":
                {
                    String exp = m.group(1);
                    Matcher boolMatcher = boolRegex.matcher(exp);
                    if(!boolMatcher.find()) continue;

                    String src1 = boolMatcher.group(1).trim();
                    if(!isRegister(src1)) continue;
                    String src2 = boolMatcher.group(3).trim();

                    String operator = boolMatcher.group(2);

                    String neg = "";

                    switch(operator)
                    {
                        case "==": neg = "bne "; break;
                        case "!=": neg = "beq "; break;
                        case ">=": neg = "blt "; break;
                        case ">": neg = "ble "; break;
                        case "<=": neg = "bgt "; break;
                        case "<": neg = "bge "; break;
                    }

                    if(isRegister(src2))
                    {
                        String label = getLabel();
                        String out = "";
                        if(inst.key.equals("while"))
                        {
                            out += format(label + ":") + "\n";
                            String newLabel = getLabel();
                            out += format(neg + src1 + ", " + src2 + ", " + newLabel);
                            blocks.push(new Block(inst.key, newLabel, label));
                            return out;
                        }
                        out += format(neg + src1 + ", " + src2 + ", " + label);
                        blocks.push(new Block(inst.key, label));
                        return out;

                    }
                    else if(isImmediate(src2))
                    {
                        String label = getLabel();
                        String out = format("li " + tempRegister + ", " + src2) + "\n";
                        if(inst.key.equals("while"))
                        {
                            out += format(label + ":") + "\n";
                            String newLabel = getLabel();
                            out += format(neg + src1 + ", " + tempRegister + ", " + newLabel);
                            blocks.push(new Block(inst.key, newLabel, label));
                            return out;
                        }

                        out += format(neg + src1 + ", " + tempRegister + ", " + label);
                        blocks.push(new Block(inst.key, label));
                        return out;
                    }

                    break;
                }
                case "func":
                {
                    if(!blocks.empty()) break;

                    String function = m.group(1).trim();
                    String out = format(".globl " + function) + "\n" +
                            format(function + ":");
                    blocks.push(new Block("func"));
                    return out;
                }
                case "end":
                {
                    if(blocks.empty()) break;
                    String type = blocks.peek().type;
                    String out = "";

                    if(type.equals("func"))
                    {
                        blocks.pop();
                        return format("");
                    }
                    if(type.equals("while"))
                    {
                        out += format("j " + blocks.peek().prevLabel) + "\n";
                    }

                    String label = blocks.pop().label;
                    out += format(label + ":");
                    return out;
                }
                case "j":
                {
                    String function = m.group(1).trim();
                    return format("j " + function);
                }
                case "jal":
                {
                    String function = m.group(1).trim();
                    return format("addi $sp, $sp, -4") + "\n"+
                            format("sw $ra, 0($sp)") + "\n"+
                            format("jal " + function) + "\n" +
                            format("lw $ra, 0($sp)") + "\n" +
                            format("addi $sp, $sp, 4");
                }
                case "return":
                {
                    String value = m.group(1).trim();
                    if(value.equals("")) return format("jr $ra");
                    if(isRegister(value))
                        return format("move $v0, " + value) + "\n" +
                                format("jr $ra");
                    if(isImmediate(value))
                        return format("li $v0, " + value) + "\n" +
                            format("jr $ra");
                    break;
                }
                case "exit":
                {
                    return format("li $v0, 10") + "\n" +
                            format("syscall");
                }
                case "asm":
                {
                    return format(m.group(1));
                }
            }
        }

        return "error";
    }

    public String format(String str)
    {
        StringBuilder out = new StringBuilder();

        // indentation
        for(int i = 0; i < blocks.size(); i++)
            for(int j = 0; j < indentSize; j++)
                out.append(" ");

        out.append(str);

        if(!comment.equals(""))
        {
            if(out.toString().trim().length() > 0) out.append(" ");
            out.append("# ").append(comment);
        }

        return out.toString();
    }

    public String getLabel()
    {
        String str = labelPrefix + labelCounter;
        labelCounter++;
        return str;
    }

    public boolean isRegister(String str)
    {
        if(str.charAt(0) != '$') return false;

        // check $0 - $31
        try
        {
            int id = Integer.parseInt(str.substring(1));
            return id >= 0 && id < 32;
        }
        catch(Exception ignored) {}

        for(String s : registers)
            if(str.equals(s)) return true;

        return false;
    }

    public boolean isImmediate(String str)
    {
        return isInteger(str) || isChar(str);
    }

    public boolean isInteger(String str)
    {
        str = str.toLowerCase();

        // Check hex number
        if(str.indexOf("0x") == 0)
            return hexRegex.matcher(str).matches();

        // Check decimal number
        return intRegex.matcher(str).matches();
    }

    public boolean isChar(String str)
    {
        return charRegex.matcher(str).matches();
    }

    public void print(String str)
    {
        System.out.println(str);
    }
}