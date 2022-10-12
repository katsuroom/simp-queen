import java.util.Scanner;

public class Main {

    public static void main(String[] args)
    {
        Scanner scan = new Scanner(System.in);

        boolean testing = false;

        if(!testing)
            new GUI();
        else
        {
            Queen queen = new Queen();
            while(true)
            {
                System.out.print("Input: > ");
                String input = scan.nextLine().trim();
                System.out.println("\n" + queen.eval(input) + "\n");
            }
        }
    }
}