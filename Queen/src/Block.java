public class Block {
    public String type = "", label = "", prevLabel = "";
    public int id;

    public Block(String type)
    {
        this(type, "");
    }

    public Block(String type, String label)
    {
        this(type, label, "");
    }

    public Block(String type, String label, String prevLabel)
    {
        this.type = type;
        this.label = label;
        this.prevLabel = prevLabel;
    }
}
