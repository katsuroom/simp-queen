import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.Objects;
import java.util.Scanner;

public class GUI implements ActionListener {

    private final int screenWidth = 480;
    private final int screenHeight = 280;

    JButton inputButton, outputButton, buildButton;
    JTextField inputText, outputText;
    JTextArea consoleBox;
    Queen queen;

    File fileIn;
    String outdir;
    File fileOut;

    public GUI()
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e)
        {
            consoleBox.setText(e.getMessage());
        }

        queen = new Queen();

        JFrame frame = new JFrame();
        frame.setTitle("Queen");
        frame.setSize(screenWidth, screenHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        Image icon = new ImageIcon(Objects.requireNonNull(GUI.class.getClassLoader().getResource("queen.png"))).getImage();
        frame.setIconImage(icon);

        Font font = new Font("Segoe UI", Font.PLAIN, 12);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        JLabel inputLabel = new JLabel("Input file:");
        inputLabel.setFont(font);
        inputLabel.setBounds(10, 10, 200, 20);
        panel.add(inputLabel);

        inputText = new JTextField();
        inputText.setBounds(10, 35, 370, 20);
        inputText.setEditable(false);
        panel.add(inputText);

        inputButton = new JButton("Browse");
        inputButton.setFont(font);
        inputButton.setBounds(385, 35, 70, 20);
        inputButton.addActionListener(this);
        panel.add(inputButton);

        JLabel outputLabel = new JLabel("Output directory:");
        outputLabel.setFont(font);
        outputLabel.setBounds(10, 60, 200, 20);
        panel.add(outputLabel);

        outputText = new JTextField();
        outputText.setBounds(10, 85, 370, 20);
        outputText.setEditable(false);
        panel.add(outputText);

        outputButton = new JButton("Browse");
        outputButton.setFont(font);
        outputButton.setBounds(385, 85, 70, 20);
        outputButton.addActionListener(this);
        panel.add(outputButton);

        consoleBox = new JTextArea("Queen for SIMP 1.0");
        consoleBox.setFont(font);
        consoleBox.setBounds(10, 120, 370, 100);
        consoleBox.setEditable(false);
        consoleBox.setBackground(Color.BLACK);
        consoleBox.setForeground(Color.LIGHT_GRAY);
        panel.add(consoleBox);

        buildButton = new JButton("<html>I simp<br>for the<br>Queen.</html>");
        buildButton.setFont(font);
        buildButton.setBounds(385, 120, 70, 100);
        buildButton.addActionListener(this);
        panel.add(buildButton);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if(event.getSource() == inputButton)
        {
            JFileChooser browser = new JFileChooser();
            browser.setCurrentDirectory(new File("."));
            browser.setFileFilter(new FileNameExtensionFilter("SIMP file (*.simp)", "simp"));

            if(browser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                fileIn = new File(browser.getSelectedFile().getAbsolutePath());
                inputText.setText(fileIn.toString());
            }
        }
        else if(event.getSource() == outputButton)
        {
            JFileChooser browser = new JFileChooser();
            browser.setCurrentDirectory(new File("."));
            browser.setFileFilter(new FolderFilter());
            browser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            browser.setAcceptAllFileFilterUsed(false);

            if(browser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
            {
                outdir = browser.getSelectedFile().getAbsolutePath() + "\\";
                outputText.setText(outdir);
            }
        }
        else if(event.getSource() == buildButton)
        {
            if(fileIn == null)
            {
                consoleBox.setText("No file selected.");
                return;
            }
            if(outdir == null)
            {
                consoleBox.setText("No output directory selected.");
                return;
            }

            try {
                Scanner scan = new Scanner(fileIn);
                fileOut = new File(outdir + fileIn.getName().substring(0, fileIn.getName().lastIndexOf('.')) + ".asm");
                fileOut.createNewFile();

                FileWriter writer = new FileWriter(fileOut);
                int lineNum = 1;

                while(scan.hasNextLine())
                {
                    String line = scan.nextLine().trim();
                    if(line.equals(""))
                        writer.write("\n");
                    else
                    {
                        consoleBox.setText("Processing... line " + lineNum);
                        String eval = queen.eval(line);
                        if(eval.equals("error"))
                        {
                            consoleBox.setText("error on line: " + lineNum);
                            writer.write("--error--");
                            writer.close();
                            return;
                        }

                        if(!eval.equals(""))
                            writer.write(eval + "\n");
                    }

                    lineNum++;
                }

                consoleBox.setText("");
                consoleBox.append("Translation complete!\n");
                consoleBox.append("Your code has been written to: '" + fileOut.getName() + "'");
                writer.close();

            }
            catch(Exception e)
            {
                consoleBox.setText(e.getMessage());
            }
        }
    }
}

class FolderFilter extends FileFilter {
    @Override
    public boolean accept(File file) {
        return file.isDirectory();
    }

    @Override
    public String getDescription() {
        return "Directory";
    }
}